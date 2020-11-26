<?php

namespace OneLang\Generator\TemplateFileGeneratorPlugin;

use OneLang\Parsers\Common\ExpressionParser\ExpressionParser;
use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\Ast\Expressions\GlobalFunctionCallExpression;
use OneLang\One\Ast\Expressions\ICallExpression;
use OneLang\One\Ast\Expressions\Identifier;
use OneLang\One\Ast\Expressions\IMethodCallExpression;
use OneLang\One\Ast\Expressions\InstanceMethodCallExpression;
use OneLang\One\Ast\Expressions\PropertyAccessExpression;
use OneLang\One\Ast\Expressions\StaticMethodCallExpression;
use OneLang\One\Ast\Expressions\UnresolvedCallExpression;
use OneLang\One\Ast\Interfaces\IExpression;
use OneLang\One\Ast\Interfaces\IType;
use OneLang\One\Ast\Statements\Statement;
use OneLang\Generator\IGeneratorPlugin\IGeneratorPlugin;
use OneLang\Parsers\Common\Reader\Reader;
use OneLang\VM\Values\BooleanValue;
use OneLang\VM\Values\ICallableValue;
use OneLang\VM\Values\IVMValue;
use OneLang\VM\Values\ObjectValue;
use OneLang\VM\Values\StringValue;
use OneLang\One\Ast\References\IInstanceMemberReference;
use OneLang\One\Ast\References\InstanceFieldReference;
use OneLang\One\Ast\References\InstancePropertyReference;
use OneLang\One\Ast\References\StaticFieldReference;
use OneLang\One\Ast\References\StaticPropertyReference;
use OneLang\One\Ast\References\VariableReference;
use OneLang\One\Ast\Types\IClassMember;
use OneLang\Template\TemplateParser\TemplateParser;
use OneLang\Generator\IGenerator\IGenerator;
use OneLang\VM\ExprVM\ExprVM;
use OneLang\VM\ExprVM\IVMHooks;
use OneLang\VM\ExprVM\VMContext;
use OneLang\Parsers\TypeScriptParser\TypeScriptParser2;
use OneLang\One\Ast\AstTypes\ClassType;
use OneLang\One\Ast\AstTypes\TypeHelper;

class CodeTemplate {
    public $template;
    public $includes;
    public $ifExpr;
    
    function __construct($template, $includes, $ifExpr) {
        $this->template = $template;
        $this->includes = $includes;
        $this->ifExpr = $ifExpr;
    }
}

class CallTemplate {
    public $className;
    public $methodName;
    public $args;
    public $template;
    
    function __construct($className, $methodName, $args, $template) {
        $this->className = $className;
        $this->methodName = $methodName;
        $this->args = $args;
        $this->template = $template;
    }
}

class FieldAccessTemplate {
    public $className;
    public $fieldName;
    public $template;
    
    function __construct($className, $fieldName, $template) {
        $this->className = $className;
        $this->fieldName = $fieldName;
        $this->template = $template;
    }
}

class ExpressionValue implements IVMValue {
    public $value;
    
    function __construct($value) {
        $this->value = $value;
    }
    
    function equals($other) {
        return $other instanceof ExpressionValue && $other->value === $this->value;
    }
}

class TypeValue implements IVMValue {
    public $type;
    
    function __construct($type) {
        $this->type = $type;
    }
    
    function equals($other) {
        return $other instanceof TypeValue && TypeHelper::equals($other->type, $this->type);
    }
}

class LambdaValue implements ICallableValue {
    public $callback;
    
    function __construct($callback) {
        $this->callback = $callback;
    }
    
    function equals($other) {
        return false;
    }
    
    function call($args) {
        return $this->callback($args);
    }
}

class TemplateFileGeneratorPlugin implements IGeneratorPlugin, IVMHooks {
    public $methods;
    public $fields;
    public $modelGlobals;
    public $generator;
    
    function __construct($generator, $templateYaml) {
        $this->generator = $generator;
        $this->methods = Array();
        $this->fields = Array();
        $this->modelGlobals = Array();
        $root = OneYaml::load($templateYaml);
        $exprDict = $root->dict("expressions");
        
        foreach (array_keys($exprDict) as $exprStr) {
            $val = @$exprDict[$exprStr] ?? null;
            $ifStr = $val->str("if");
            $ifExpr = $ifStr === null ? null : (new TypeScriptParser2($ifStr, null))->parseExpression();
            $tmpl = $val->type() === ValueType::STRING ? new CodeTemplate($val->asStr(), array(), null) : new CodeTemplate($val->str("template"), $val->strArr("includes"), $ifExpr);
            
            $this->addExprTemplate($exprStr, $tmpl);
        }
    }
    
    function propAccess($obj, $propName) {
        if ($obj instanceof ExpressionValue && $propName === "type")
            return new TypeValue($obj->value->getType());
        if ($obj instanceof TypeValue && $propName === "name" && $obj->type instanceof ClassType)
            return new StringValue($obj->type->decl->name);
        return null;
    }
    
    function stringifyValue($value) {
        if ($value instanceof ExpressionValue) {
            $result = $this->generator->expr($value->value);
            return $result;
        }
        return null;
    }
    
    function addMethod($name, $callTmpl) {
        if (!(array_key_exists($name, $this->methods)))
            $this->methods[$name] = array();
        @$this->methods[$name] ?? null[] = $callTmpl;
    }
    
    function addExprTemplate($exprStr, $tmpl) {
        $expr = (new ExpressionParser(new Reader($exprStr)))->parse();
        if ($expr instanceof UnresolvedCallExpression && $expr->func instanceof PropertyAccessExpression && $expr->func->object instanceof Identifier) {
            $callTmpl = new CallTemplate($expr->func->object->text, $expr->func->propertyName, array_map(function ($x) { return ($x)->text; }, $expr->args), $tmpl);
            $this->addMethod($callTmpl->className . "." . $callTmpl->methodName . "@" . count($callTmpl->args), $callTmpl);
        }
        else if ($expr instanceof UnresolvedCallExpression && $expr->func instanceof Identifier) {
            $callTmpl = new CallTemplate(null, $expr->func->text, array_map(function ($x) { return ($x)->text; }, $expr->args), $tmpl);
            $this->addMethod($callTmpl->methodName . "@" . count($callTmpl->args), $callTmpl);
        }
        else if ($expr instanceof PropertyAccessExpression && $expr->object instanceof Identifier) {
            $fieldTmpl = new FieldAccessTemplate($expr->object->text, $expr->propertyName, $tmpl);
            $this->fields[$fieldTmpl->className . "." . $fieldTmpl->fieldName] = $fieldTmpl;
        }
        else
            throw new \OneLang\Core\Error("This expression template format is not supported: '" . $exprStr . "'");
    }
    
    function expr($expr) {
        $isCallExpr = $expr instanceof StaticMethodCallExpression || $expr instanceof InstanceMethodCallExpression || $expr instanceof GlobalFunctionCallExpression;
        $isFieldRef = $expr instanceof StaticFieldReference || $expr instanceof StaticPropertyReference || $expr instanceof InstanceFieldReference || $expr instanceof InstancePropertyReference;
        
        if (!$isCallExpr && !$isFieldRef)
            return null;
        // quick return
        
        $codeTmpl = null;
        $model = Array();
        $context = new VMContext(new ObjectValue($model), $this);
        
        $model["type"] = new TypeValue($expr->getType());
        foreach (array_keys($this->modelGlobals) as $name)
            $model[$name] = @$this->modelGlobals[$name] ?? null;
        
        if ($isCallExpr) {
            $call = $expr;
            $parentIntf = $call->getParentInterface();
            $methodName = ($parentIntf === null ? "" : $parentIntf->name . ".") . $call->getName() . "@" . count($call->args);
            $callTmpls = @$this->methods[$methodName] ?? null;
            if ($callTmpls === null)
                return null;
            
            foreach ($callTmpls as $callTmpl) {
                if ($expr instanceof InstanceMethodCallExpression)
                    $model["this"] = new ExpressionValue($expr->object);
                for ($i = 0; $i < count($callTmpl->args); $i++)
                    $model[$callTmpl->args[$i]] = new ExpressionValue($call->args[$i]);
                
                if ($callTmpl->template->ifExpr === null || ((new ExprVM($context))->evaluate($callTmpl->template->ifExpr))->value) {
                    $codeTmpl = $callTmpl->template;
                    break;
                }
            }
        }
        else if ($isFieldRef) {
            $cm = ($expr)->getVariable();
            $field = @$this->fields[$cm->getParentInterface()->name . "." . $cm->name] ?? null;
            if ($field === null)
                return null;
            
            if ($expr instanceof InstanceFieldReference || $expr instanceof InstancePropertyReference)
                $model["this"] = new ExpressionValue(($expr)->object);
            $codeTmpl = $field->template;
        }
        else
            return null;
        
        if ($codeTmpl === null)
            return null;
        
        foreach ($codeTmpl->includes ?? array() as $inc)
            $this->generator->addInclude($inc);
        
        $tmpl = (new TemplateParser($codeTmpl->template))->parse();
        $result = $tmpl->format($context);
        return $result;
    }
    
    function stmt($stmt) {
        return null;
    }
}
