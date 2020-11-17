<?php

namespace OneLang\One\Transforms\InferTypesPlugins\ResolveFieldAndPropertyAccess;

use OneLang\One\Transforms\InferTypesPlugins\Helpers\InferTypesPlugin\InferTypesPlugin;
use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\Ast\Expressions\PropertyAccessExpression;
use OneLang\One\Ast\Expressions\UnresolvedCallExpression;
use OneLang\One\Ast\References\ClassReference;
use OneLang\One\Ast\References\Reference;
use OneLang\One\Ast\References\StaticFieldReference;
use OneLang\One\Ast\References\StaticPropertyReference;
use OneLang\One\Ast\References\InstanceFieldReference;
use OneLang\One\Ast\References\InstancePropertyReference;
use OneLang\One\Ast\References\ThisReference;
use OneLang\One\Ast\References\SuperReference;
use OneLang\One\Ast\References\EnumReference;
use OneLang\One\Ast\References\StaticThisReference;
use OneLang\One\Ast\Types\Class_;
use OneLang\One\Ast\Types\Method;
use OneLang\One\Ast\Types\Interface_;
use OneLang\One\Ast\AstTypes\ClassType;
use OneLang\One\Ast\AstTypes\InterfaceType;
use OneLang\One\Ast\AstTypes\AnyType;
use OneLang\One\Ast\AstTypes\TypeHelper;
use OneLang\One\Transforms\InferTypesPlugins\Helpers\GenericsResolver\GenericsResolver;

class ResolveFieldAndPropertyAccess extends InferTypesPlugin {
    function __construct() {
        parent::__construct("ResolveFieldAndPropertyAccess");
        
    }
    
    protected function getStaticRef($cls, $memberName) {
        $field = \OneCore\ArrayHelper::find($cls->fields, function ($x) use ($memberName) { return $x->name === $memberName; });
        if ($field !== null && $field->isStatic)
            return new StaticFieldReference($field);
        
        $prop = \OneCore\ArrayHelper::find($cls->properties, function ($x) use ($memberName) { return $x->name === $memberName; });
        if ($prop !== null && $prop->isStatic)
            return new StaticPropertyReference($prop);
        
        $this->errorMan->throw("Could not resolve static member access of a class: " . $cls->name . "::" . $memberName);
        return null;
    }
    
    protected function getInstanceRef($cls, $memberName, $obj) {
        while (true) {
            $field = \OneCore\ArrayHelper::find($cls->fields, function ($x) use ($memberName) { return $x->name === $memberName; });
            if ($field !== null && !$field->isStatic)
                return new InstanceFieldReference($obj, $field);
            
            $prop = \OneCore\ArrayHelper::find($cls->properties, function ($x) use ($memberName) { return $x->name === $memberName; });
            if ($prop !== null && !$prop->isStatic)
                return new InstancePropertyReference($obj, $prop);
            
            if ($cls->baseClass === null)
                break;
            
            $cls = ($cls->baseClass)->decl;
        }
        
        $this->errorMan->throw("Could not resolve instance member access of a class: " . $cls->name . "::" . $memberName);
        return null;
    }
    
    protected function getInterfaceRef($intf, $memberName, $obj) {
        $field = \OneCore\ArrayHelper::find($intf->fields, function ($x) use ($memberName) { return $x->name === $memberName; });
        if ($field !== null && !$field->isStatic)
            return new InstanceFieldReference($obj, $field);
        
        foreach ($intf->baseInterfaces as $baseIntf) {
            $res = $this->getInterfaceRef(($baseIntf)->decl, $memberName, $obj);
            if ($res !== null)
                return $res;
        }
        return null;
    }
    
    protected function transformPA($expr) {
        if ($expr->object instanceof ClassReference)
            return $this->getStaticRef($expr->object->decl, $expr->propertyName);
        
        if ($expr->object instanceof StaticThisReference)
            return $this->getStaticRef($expr->object->cls, $expr->propertyName);
        
        $expr->object = $this->main->runPluginsOn($expr->object);
        
        if ($expr->object instanceof ThisReference)
            return $this->getInstanceRef($expr->object->cls, $expr->propertyName, $expr->object);
        
        $type = $expr->object->getType();
        if ($type instanceof ClassType)
            return $this->getInstanceRef($type->decl, $expr->propertyName, $expr->object);
        else if ($type instanceof InterfaceType) {
            $ref = $this->getInterfaceRef($type->decl, $expr->propertyName, $expr->object);
            if ($ref === null)
                $this->errorMan->throw("Could not resolve instance member access of a interface: " . $type->repr() . "::" . $expr->propertyName);
            return $ref;
        }
        else if ($type === null)
            $this->errorMan->throw("Type was not inferred yet (prop=\"" . $expr->propertyName . "\")");
        else if ($type instanceof AnyType)
            //this.errorMan.throw(`Object has any type (prop="${expr.propertyName}")`);
            $expr->setActualType(AnyType::$instance);
        else
            $this->errorMan->throw("Expected class as variable type, but got: " . $type->repr() . " (prop=\"" . $expr->propertyName . "\")");
        
        return $expr;
    }
    
    function canTransform($expr) {
        return $expr instanceof PropertyAccessExpression && !($expr->object instanceof EnumReference) && !($expr->parentNode instanceof UnresolvedCallExpression && $expr->parentNode->func === $expr) && !($expr->actualType instanceof AnyType);
    }
    
    function transform($expr) {
        return $this->transformPA($expr);
    }
    
    function canDetectType($expr) {
        return $expr instanceof InstanceFieldReference || $expr instanceof InstancePropertyReference || $expr instanceof StaticFieldReference || $expr instanceof StaticPropertyReference;
    }
    
    function detectType($expr) {
        if ($expr instanceof InstanceFieldReference) {
            $actualType = GenericsResolver::fromObject($expr->object)->resolveType($expr->field->type, true);
            $expr->setActualType($actualType, false, TypeHelper::isGeneric($expr->object->actualType));
            return true;
        }
        else if ($expr instanceof InstancePropertyReference) {
            $actualType = GenericsResolver::fromObject($expr->object)->resolveType($expr->property->type, true);
            $expr->setActualType($actualType);
            return true;
        }
        else if ($expr instanceof StaticPropertyReference) {
            $expr->setActualType($expr->decl->type, false, false);
            return true;
        }
        else if ($expr instanceof StaticFieldReference) {
            $expr->setActualType($expr->decl->type, false, false);
            return true;
        }
        
        return false;
    }
}
