<?php

namespace OneLang\VM\ExprVM;

use OneLang\One\Ast\Expressions\ConditionalExpression;
use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\Ast\Expressions\Identifier;
use OneLang\One\Ast\Expressions\NumericLiteral;
use OneLang\One\Ast\Expressions\PropertyAccessExpression;
use OneLang\One\Ast\Expressions\StringLiteral;
use OneLang\One\Ast\Expressions\TemplateString;
use OneLang\One\Ast\Expressions\UnresolvedCallExpression;
use OneLang\VM\Values\BooleanValue;
use OneLang\VM\Values\ICallableValue;
use OneLang\VM\Values\IVMValue;
use OneLang\VM\Values\NumericValue;
use OneLang\VM\Values\ObjectValue;
use OneLang\VM\Values\StringValue;

interface IVMHooks {
    function stringifyValue($value);
}

class VMContext {
    public $model;
    public $hooks;
    
    function __construct($model, $hooks = null) {
        $this->model = $model;
        $this->hooks = $hooks;
    }
}

class ExprVM {
    public $context;
    
    function __construct($context) {
        $this->context = $context;
    }
    
    static function propAccess($obj, $propName) {
        if (!($obj instanceof ObjectValue))
            throw new \OneLang\Core\Error("You can only access a property of an object!");
        if (!(array_key_exists($propName, ($obj)->props)))
            throw new \OneLang\Core\Error("Property '" . $propName . "' does not exists on this object!");
        return @($obj)->props[$propName] ?? null;
    }
    
    function evaluate($expr) {
        if ($expr instanceof Identifier)
            return ExprVM::propAccess($this->context->model, $expr->text);
        else if ($expr instanceof PropertyAccessExpression) {
            $objValue = $this->evaluate($expr->object);
            return ExprVM::propAccess($objValue, $expr->propertyName);
        }
        else if ($expr instanceof UnresolvedCallExpression) {
            $func = $this->evaluate($expr->func);
            $args = array_map(function ($x) { return $this->evaluate($x); }, $expr->args);
            $result = $func->call($args);
            return $result;
        }
        else if ($expr instanceof StringLiteral)
            return new StringValue($expr->stringValue);
        else if ($expr instanceof NumericLiteral)
            return new NumericValue(Global.parseInt($expr->valueAsText));
        else if ($expr instanceof ConditionalExpression) {
            $condResult = $this->evaluate($expr->condition);
            $result = $this->evaluate(($condResult)->value ? $expr->whenTrue : $expr->whenFalse);
            return $result;
        }
        else if ($expr instanceof TemplateString) {
            $result = "";
            foreach ($expr->parts as $part) {
                if ($part->isLiteral)
                    $result .= $part->literalText;
                else {
                    $value = $this->evaluate($part->expression);
                    $result .= $value instanceof StringValue ? $value->value : $this->context->hooks->stringifyValue($value);
                }
            }
            return new StringValue($result);
        }
        else
            throw new \OneLang\Core\Error("Unsupported expression!");
    }
}
