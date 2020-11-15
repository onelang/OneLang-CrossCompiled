<?php

namespace One\Transforms\InferTypesPlugins\InferReturnType;

use One\Transforms\InferTypesPlugins\Helpers\InferTypesPlugin\InferTypesPlugin;
use One\Ast\Types\Property;
use One\Ast\Types\Lambda;
use One\Ast\Types\Method;
use One\Ast\Types\IMethodBase;
use One\Ast\AstTypes\VoidType;
use One\Ast\AstTypes\AnyType;
use One\Ast\AstTypes\LambdaType;
use One\Ast\AstTypes\ClassType;
use One\Ast\AstTypes\TypeHelper;
use One\Ast\Statements\Statement;
use One\Ast\Statements\ReturnStatement;
use One\Ast\Statements\ThrowStatement;
use One\ErrorManager\ErrorManager;
use One\Ast\Expressions\NullLiteral;
use One\Ast\Expressions\Expression;
use One\Ast\Interfaces\IType;

class ReturnTypeInferer {
    public $returnsNull = false;
    public $throws = false;
    public $returnTypes;
    public $errorMan;
    
    function __construct($errorMan) {
        $this->errorMan = $errorMan;
        $this->returnTypes = array();
    }
    
    function addReturn($returnValue) {
        if ($returnValue instanceof NullLiteral) {
            $this->returnsNull = true;
            return;
        }
        
        $returnType = $returnValue->actualType;
        if ($returnType === null)
            throw new \OneLang\Error("Return type cannot be null");
        
        if (!\OneLang\ArrayHelper::some($this->returnTypes, function ($x) use ($returnType) { return TypeHelper::equals($x, $returnType); }))
            $this->returnTypes[] = $returnType;
    }
    
    function finish($declaredType, $errorContext, $asyncType) {
        $inferredType = null;
        
        if (count($this->returnTypes) === 0) {
            if ($this->throws)
                $inferredType = $declaredType ?? VoidType::$instance;
            else if ($this->returnsNull) {
                if ($declaredType !== null)
                    $inferredType = $declaredType;
                else
                    $this->errorMan->throw($errorContext . " returns only null and it has no declared return type!");
            }
            else
                $inferredType = VoidType::$instance;
        }
        else if (count($this->returnTypes) === 1)
            $inferredType = $this->returnTypes[0];
        else if ($declaredType !== null && \OneLang\ArrayHelper::every($this->returnTypes, function ($x, $i) use ($declaredType) { return TypeHelper::isAssignableTo($x, $declaredType); }))
            $inferredType = $declaredType;
        else {
            $this->errorMan->throw($errorContext . " returns different types: " . implode(", ", array_map(function ($x) { return $x->repr(); }, $this->returnTypes)));
            $inferredType = AnyType::$instance;
        }
        
        $checkType = $declaredType;
        if ($checkType !== null && $asyncType !== null && $checkType instanceof ClassType && $checkType->decl === $asyncType->decl)
            $checkType = $checkType->typeArguments[0];
        
        if ($checkType !== null && !TypeHelper::isAssignableTo($inferredType, $checkType))
            $this->errorMan->throw($errorContext . " returns different type (" . $inferredType->repr() . ") than expected " . $checkType->repr());
        
        $this->returnTypes = null;
        return $declaredType !== null ? $declaredType : $inferredType;
    }
}

class InferReturnType extends InferTypesPlugin {
    public $returnTypeInfer;
    
    function get_current() {
        return $this->returnTypeInfer[count($this->returnTypeInfer) - 1];
    }
    
    function __construct() {
        parent::__construct("InferReturnType");
        $this->returnTypeInfer = array();
    }
    
    function start() {
        $this->returnTypeInfer[] = new ReturnTypeInferer($this->errorMan);
    }
    
    function finish($declaredType, $errorContext, $asyncType) {
        return array_pop($this->returnTypeInfer)->finish($declaredType, $errorContext, $asyncType);
    }
    
    function handleStatement($stmt) {
        if (count($this->returnTypeInfer) === 0)
            return false;
        if ($stmt instanceof ReturnStatement && $stmt->expression !== null) {
            $this->main->processStatement($stmt);
            $this->get_current()->addReturn($stmt->expression);
            return true;
        }
        else if ($stmt instanceof ThrowStatement) {
            $this->get_current()->throws = true;
            return false;
        }
        else
            return false;
    }
    
    function handleLambda($lambda) {
        $this->start();
        $this->main->processLambda($lambda);
        $lambda->returns = $this->finish($lambda->returns, "Lambda", null);
        $lambda->setActualType(new LambdaType($lambda->parameters, $lambda->returns), false, true);
        return true;
    }
    
    function handleMethod($method) {
        if ($method instanceof Method && $method->body !== null) {
            $this->start();
            $this->main->processMethodBase($method);
            $method->returns = $this->finish($method->returns, "Method \"" . $method->name . "\"", $method->async ? $this->main->currentFile->literalTypes->promise : null);
            return true;
        }
        else
            return false;
    }
    
    function handleProperty($prop) {
        $this->main->processVariable($prop);
        
        if ($prop->getter !== null) {
            $this->start();
            $this->main->processBlock($prop->getter);
            $prop->type = $this->finish($prop->type, "Property \"" . $prop->name . "\" getter", null);
        }
        
        if ($prop->setter !== null) {
            $this->start();
            $this->main->processBlock($prop->setter);
            $this->finish(VoidType::$instance, "Property \"" . $prop->name . "\" setter", null);
        }
        
        return true;
    }
}
