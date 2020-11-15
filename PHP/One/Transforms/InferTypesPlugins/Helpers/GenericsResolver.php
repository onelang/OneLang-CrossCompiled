<?php

namespace One\Transforms\InferTypesPlugins\Helpers\GenericsResolver;

use One\Ast\Expressions\Expression;
use One\Ast\Expressions\IMethodCallExpression;
use One\Ast\AstTypes\ClassType;
use One\Ast\AstTypes\GenericsType;
use One\Ast\AstTypes\InterfaceType;
use One\Ast\AstTypes\LambdaType;
use One\Ast\AstTypes\EnumType;
use One\Ast\AstTypes\AnyType;
use One\Ast\AstTypes\TypeHelper;
use One\Ast\Types\MethodParameter;
use One\Ast\Interfaces\IType;

class GenericsResolver {
    public $resolutionMap;
    
    function __construct()
    {
        $this->resolutionMap = new \OneLang\Map();
    }
    
    static function fromObject($object) {
        $resolver = new GenericsResolver();
        $resolver->collectClassGenericsFromObject($object);
        return $resolver;
    }
    
    function addResolution($typeVarName, $actualType) {
        $prevRes = $this->resolutionMap->get($typeVarName);
        if ($prevRes !== null && !TypeHelper::equals($prevRes, $actualType))
            throw new \OneLang\Error("Resolving '" . $typeVarName . "' is ambiguous, " . $prevRes->repr() . " <> " . $actualType->repr());
        $this->resolutionMap->set($typeVarName, $actualType);
    }
    
    function collectFromMethodCall($methodCall) {
        if (count($methodCall->typeArgs) === 0)
            return;
        if (count($methodCall->typeArgs) !== count($methodCall->method->typeArguments))
            throw new \OneLang\Error("Expected " . count($methodCall->method->typeArguments) . " type argument(s) for method call, but got " . count($methodCall->typeArgs));
        for ($i = 0; $i < count($methodCall->typeArgs); $i++)
            $this->addResolution($methodCall->method->typeArguments[$i], $methodCall->typeArgs[$i]);
    }
    
    function collectClassGenericsFromObject($actualObject) {
        $actualType = $actualObject->getType();
        if ($actualType instanceof ClassType) {
            if (!$this->collectResolutionsFromActualType($actualType->decl->type, $actualType)) { }
        }
        else if ($actualType instanceof InterfaceType) {
            if (!$this->collectResolutionsFromActualType($actualType->decl->type, $actualType)) { }
        }
        else
            throw new \OneLang\Error("Expected ClassType or InterfaceType, got " . ($actualType !== null ? $actualType->repr() : "<null>"));
    }
    
    function collectResolutionsFromActualType($genericType, $actualType) {
        if (!TypeHelper::isGeneric($genericType))
            return true;
        if ($genericType instanceof GenericsType) {
            $this->addResolution($genericType->typeVarName, $actualType);
            return true;
        }
        else if ($genericType instanceof ClassType && $actualType instanceof ClassType && $genericType->decl === $actualType->decl) {
            if (count($genericType->typeArguments) !== count($actualType->typeArguments))
                throw new \OneLang\Error("Same class (" . $genericType->repr() . ") used with different number of type arguments (" . count($genericType->typeArguments) . " <> " . count($actualType->typeArguments) . ")");
            return \OneLang\ArrayHelper::every($genericType->typeArguments, function ($x, $i) use ($actualType) { return $this->collectResolutionsFromActualType($x, $actualType->typeArguments[$i]); });
        }
        else if ($genericType instanceof InterfaceType && $actualType instanceof InterfaceType && $genericType->decl === $actualType->decl) {
            if (count($genericType->typeArguments) !== count($actualType->typeArguments))
                throw new \OneLang\Error("Same class (" . $genericType->repr() . ") used with different number of type arguments (" . count($genericType->typeArguments) . " <> " . count($actualType->typeArguments) . ")");
            return \OneLang\ArrayHelper::every($genericType->typeArguments, function ($x, $i) use ($actualType) { return $this->collectResolutionsFromActualType($x, $actualType->typeArguments[$i]); });
        }
        else if ($genericType instanceof LambdaType && $actualType instanceof LambdaType) {
            if (count($genericType->parameters) !== count($actualType->parameters))
                throw new \OneLang\Error("Generic lambda type has " . count($genericType->parameters) . " parameters while the actual type has " . count($actualType->parameters));
            $paramsOk = \OneLang\ArrayHelper::every($genericType->parameters, function ($x, $i) use ($actualType) { return $this->collectResolutionsFromActualType($x->type, $actualType->parameters[$i]->type); });
            $resultOk = $this->collectResolutionsFromActualType($genericType->returnType, $actualType->returnType);
            return $paramsOk && $resultOk;
        }
        else if ($genericType instanceof EnumType && $actualType instanceof EnumType && $genericType->decl === $actualType->decl) { }
        else if ($genericType instanceof AnyType || $actualType instanceof AnyType) { }
        else
            throw new \OneLang\Error("Generic type " . $genericType->repr() . " is not compatible with actual type " . $actualType->repr());
        return false;
    }
    
    function resolveType($type, $mustResolveAllGenerics) {
        if ($type instanceof GenericsType) {
            $resolvedType = $this->resolutionMap->get($type->typeVarName);
            if ($resolvedType === null && $mustResolveAllGenerics)
                throw new \OneLang\Error("Could not resolve generics type: " . $type->repr());
            return $resolvedType !== null ? $resolvedType : $type;
        }
        else if ($type instanceof ClassType)
            return new ClassType($type->decl, array_map(function ($x) use ($mustResolveAllGenerics) { return $this->resolveType($x, $mustResolveAllGenerics); }, $type->typeArguments));
        else if ($type instanceof InterfaceType)
            return new InterfaceType($type->decl, array_map(function ($x) use ($mustResolveAllGenerics) { return $this->resolveType($x, $mustResolveAllGenerics); }, $type->typeArguments));
        else if ($type instanceof LambdaType)
            return new LambdaType(array_map(function ($x) use ($mustResolveAllGenerics) { return new MethodParameter($x->name, $this->resolveType($x->type, $mustResolveAllGenerics), $x->initializer, null); }, $type->parameters), $this->resolveType($type->returnType, $mustResolveAllGenerics));
        else
            return $type;
    }
}
