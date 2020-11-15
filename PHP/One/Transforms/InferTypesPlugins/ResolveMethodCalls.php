<?php

namespace One\Transforms\InferTypesPlugins\ResolveMethodCalls;

use One\Transforms\InferTypesPlugins\Helpers\InferTypesPlugin\InferTypesPlugin;
use One\Ast\Expressions\Expression;
use One\Ast\Expressions\UnresolvedMethodCallExpression;
use One\Ast\Expressions\InstanceMethodCallExpression;
use One\Ast\Expressions\StaticMethodCallExpression;
use One\Ast\Expressions\IMethodCallExpression;
use One\Ast\AstTypes\ClassType;
use One\Ast\AstTypes\InterfaceType;
use One\Ast\AstTypes\AnyType;
use One\Ast\AstTypes\TypeHelper;
use One\Transforms\InferTypesPlugins\Helpers\GenericsResolver\GenericsResolver;
use One\Ast\References\ClassReference;
use One\Ast\References\StaticThisReference;
use One\Ast\Types\Class_;
use One\Ast\Types\IInterface;
use One\Ast\Types\Method;

class ResolveMethodCalls extends InferTypesPlugin {
    function __construct() {
        parent::__construct("ResolveMethodCalls");
        
    }
    
    protected function findMethod($cls, $methodName, $isStatic, $args) {
        $allBases = $cls instanceof Class_ ? array_values(array_filter($cls->getAllBaseInterfaces(), function ($x) { return $x instanceof Class_; })) : $cls->getAllBaseInterfaces();
        
        $methods = array();
        foreach ($allBases as $base)
            foreach ($base->methods as $m) {
                $minLen = count(array_values(array_filter($m->parameters, function ($p) { return $p->initializer === null; })));
                $maxLen = count($m->parameters);
                $match = $m->name === $methodName && $m->isStatic === $isStatic && $minLen <= count($args) && count($args) <= $maxLen;
                if ($match)
                    $methods[] = $m;
            }
        
        if (count($methods) === 0)
            throw new \OneLang\Error("Method '" . $methodName . "' was not found on type '" . $cls->name . "' with " . count($args) . " arguments");
        else if (count($methods) > 1) {
            // TODO: actually we should implement proper method shadowing here...
            $thisMethods = array_values(array_filter($methods, function ($x) use ($cls) { return $x->parentInterface === $cls; }));
            if (count($thisMethods) === 1)
                return $thisMethods[0];
            throw new \OneLang\Error("Multiple methods found with name '" . $methodName . "' and " . count($args) . " arguments on type '" . $cls->name . "'");
        }
        return $methods[0];
    }
    
    protected function resolveReturnType($expr, $genericsResolver) {
        $genericsResolver->collectFromMethodCall($expr);
        
        for ($i = 0; $i < count($expr->args); $i++) {
            // actually doesn't have to resolve, but must check if generic type confirm the previous argument with the same generic type
            $paramType = $genericsResolver->resolveType($expr->method->parameters[$i]->type, false);
            if ($paramType !== null)
                $expr->args[$i]->setExpectedType($paramType);
            $expr->args[$i] = $this->main->runPluginsOn($expr->args[$i]);
            $genericsResolver->collectResolutionsFromActualType($paramType, $expr->args[$i]->actualType);
        }
        
        if ($expr->method->returns === null) {
            $this->errorMan->throw("Method (" . $expr->method->parentInterface->name . "::" . $expr->method->name . ") return type was not specified or infered before the call.");
            return;
        }
        
        $expr->setActualType($genericsResolver->resolveType($expr->method->returns, true), true, $expr instanceof InstanceMethodCallExpression && TypeHelper::isGeneric($expr->object->getType()));
    }
    
    protected function transformMethodCall($expr) {
        if ($expr->object instanceof ClassReference || $expr->object instanceof StaticThisReference) {
            $cls = $expr->object instanceof ClassReference ? $expr->object->decl : ($expr->object instanceof StaticThisReference ? $expr->object->cls : null);
            $method = $this->findMethod($cls, $expr->methodName, true, $expr->args);
            $result = new StaticMethodCallExpression($method, $expr->typeArgs, $expr->args, $expr->object instanceof StaticThisReference);
            $this->resolveReturnType($result, new GenericsResolver());
            return $result;
        }
        else {
            $resolvedObject = $expr->object->actualType !== null ? $expr->object : $this->main->runPluginsOn($expr->object);
            $objectType = $resolvedObject->getType();
            $intfType = $objectType instanceof ClassType ? $objectType->decl : ($objectType instanceof InterfaceType ? $objectType->decl : null);
            
            if ($intfType !== null) {
                $method = $this->findMethod($intfType, $expr->methodName, false, $expr->args);
                $result = new InstanceMethodCallExpression($resolvedObject, $method, $expr->typeArgs, $expr->args);
                $this->resolveReturnType($result, GenericsResolver::fromObject($resolvedObject));
                return $result;
            }
            else if ($objectType instanceof AnyType) {
                $expr->setActualType(AnyType::$instance);
                return $expr;
            }
            else { }
            return $resolvedObject;
        }
    }
    
    function canTransform($expr) {
        return $expr instanceof UnresolvedMethodCallExpression && !($expr->actualType instanceof AnyType);
    }
    
    function transform($expr) {
        return $this->transformMethodCall($expr);
    }
}
