<?php

namespace OneLang\Generator\TemplateFileGeneratorPlugin;

use OneLang\Parsers\Common\ExpressionParser\ExpressionParser;
use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\Ast\Expressions\Identifier;
use OneLang\One\Ast\Expressions\IMethodCallExpression;
use OneLang\One\Ast\Expressions\InstanceMethodCallExpression;
use OneLang\One\Ast\Expressions\PropertyAccessExpression;
use OneLang\One\Ast\Expressions\StaticMethodCallExpression;
use OneLang\One\Ast\Expressions\UnresolvedCallExpression;
use OneLang\One\Ast\Interfaces\IExpression;
use OneLang\One\Ast\Statements\Statement;
use OneLang\Generator\IGeneratorPlugin\IGeneratorPlugin;
use OneLang\Parsers\Common\Reader\Reader;
use OneLang\VM\Values\ICallableValue;
use OneLang\VM\Values\IVMValue;
use OneLang\VM\Values\ObjectValue;
use OneLang\One\Ast\References\IInstanceMemberReference;
use OneLang\One\Ast\References\InstanceFieldReference;
use OneLang\One\Ast\References\InstancePropertyReference;
use OneLang\One\Ast\References\StaticFieldReference;
use OneLang\One\Ast\References\StaticPropertyReference;
use OneLang\One\Ast\References\VariableReference;
use OneLang\One\Ast\Types\IClassMember;
use OneLang\Template\TemplateParser\TemplateParser;
use OneLang\Generator\IGenerator\IGenerator;
use OneLang\Template\Nodes\ITemplateFormatHooks;
use OneLang\Template\Nodes\TemplateContext;

class CodeTemplate {
    public $template;
    public $includes;
    
    function __construct($template, $includes) {
        $this->template = $template;
        $this->includes = $includes;
    }
}

class MethodCallTemplate {
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
}

class LambdaValue implements ICallableValue {
    public $callback;
    
    function __construct($callback) {
        $this->callback = $callback;
    }
    
    function call($args) {
        return $this->callback($args);
    }
}

class TemplateFileGeneratorPlugin implements IGeneratorPlugin, ITemplateFormatHooks {
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
            $tmpl = $val->type() === ValueType::STRING ? new CodeTemplate($val->asStr(), array()) : new CodeTemplate($val->str("template"), $val->strArr("includes"));
            
            $this->addExprTemplate($exprStr, $tmpl);
        }
    }
    
    function formatValue($value) {
        if ($value instanceof ExpressionValue) {
            $result = $this->generator->expr($value->value);
            return $result;
        }
        return null;
    }
    
    function addExprTemplate($exprStr, $tmpl) {
        $expr = (new ExpressionParser(new Reader($exprStr)))->parse();
        if ($expr instanceof UnresolvedCallExpression && $expr->func instanceof PropertyAccessExpression && $expr->func->object instanceof Identifier) {
            $callTmpl = new MethodCallTemplate($expr->func->object->text, $expr->func->propertyName, array_map(function ($x) { return ($x)->text; }, $expr->args), $tmpl);
            $this->methods[$callTmpl->className . "." . $callTmpl->methodName . "@" . count($callTmpl->args)] = $callTmpl;
        }
        else if ($expr instanceof PropertyAccessExpression && $expr->object instanceof Identifier) {
            $fieldTmpl = new FieldAccessTemplate($expr->object->text, $expr->propertyName, $tmpl);
            $this->fields[$fieldTmpl->className . "." . $fieldTmpl->fieldName] = $fieldTmpl;
        }
        else
            throw new \OneLang\Core\Error("This expression template format is not supported: '" . $exprStr . "'");
    }
    
    function expr($expr) {
        $codeTmpl = null;
        $model = Array();
        
        if ($expr instanceof StaticMethodCallExpression || $expr instanceof InstanceMethodCallExpression) {
            $call = $expr;
            $methodName = $call->method->parentInterface->name . "." . $call->method->name . "@" . count($call->args);
            $callTmpl = @$this->methods[$methodName] ?? null;
            if ($callTmpl === null)
                return null;
            
            if ($expr instanceof InstanceMethodCallExpression)
                $model["this"] = new ExpressionValue($expr->object);
            for ($i = 0; $i < count($callTmpl->args); $i++)
                $model[$callTmpl->args[$i]] = new ExpressionValue($call->args[$i]);
            $codeTmpl = $callTmpl->template;
        }
        else if ($expr instanceof StaticFieldReference || $expr instanceof StaticPropertyReference || $expr instanceof InstanceFieldReference || $expr instanceof InstancePropertyReference) {
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
        
        foreach (array_keys($this->modelGlobals) as $name)
            $model[$name] = @$this->modelGlobals[$name] ?? null;
        
        foreach ($codeTmpl->includes ?? array() as $inc)
            $this->generator->addInclude($inc);
        
        $tmpl = (new TemplateParser($codeTmpl->template))->parse();
        $result = $tmpl->format(new TemplateContext(new ObjectValue($model), $this));
        return $result;
    }
    
    function stmt($stmt) {
        return null;
    }
}
