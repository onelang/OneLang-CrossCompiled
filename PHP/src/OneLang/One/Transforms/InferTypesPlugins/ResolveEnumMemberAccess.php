<?php

namespace OneLang\One\Transforms\InferTypesPlugins\ResolveEnumMemberAccess;

use OneLang\One\Transforms\InferTypesPlugins\Helpers\InferTypesPlugin\InferTypesPlugin;
use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\Ast\Expressions\PropertyAccessExpression;
use OneLang\One\Ast\References\EnumMemberReference;
use OneLang\One\Ast\References\EnumReference;

class ResolveEnumMemberAccess extends InferTypesPlugin {
    function __construct() {
        parent::__construct("ResolveEnumMemberAccess");
        
    }
    
    function canTransform($expr) {
        return $expr instanceof PropertyAccessExpression && $expr->object instanceof EnumReference;
    }
    
    function transform($expr) {
        $pa = $expr;
        $enumMemberRef = $pa->object;
        $member = \OneCore\ArrayHelper::find($enumMemberRef->decl->values, function ($x) use ($pa) { return $x->name === $pa->propertyName; });
        if ($member === null) {
            $this->errorMan->throw("Enum member was not found: " . $enumMemberRef->decl->name . "::" . $pa->propertyName);
            return $expr;
        }
        return new EnumMemberReference($member);
    }
    
    function canDetectType($expr) {
        return $expr instanceof EnumMemberReference;
    }
    
    function detectType($expr) {
        $expr->setActualType(($expr)->decl->parentEnum->type);
        return true;
    }
}
