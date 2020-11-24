<?php

namespace OneLang\VM\ExprVM;

use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\Ast\Expressions\Identifier;
use OneLang\One\Ast\Expressions\PropertyAccessExpression;
use OneLang\One\Ast\Expressions\StringLiteral;
use OneLang\One\Ast\Expressions\UnresolvedCallExpression;
use OneLang\VM\Values\ICallableValue;
use OneLang\VM\Values\IVMValue;
use OneLang\VM\Values\ObjectValue;
use OneLang\VM\Values\StringValue;

class ExprVM {
    public $model;
    
    function __construct($model) {
        $this->model = $model;
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
            return ExprVM::propAccess($this->model, $expr->text);
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
        else
            throw new \OneLang\Core\Error("Unsupported expression!");
    }
}
