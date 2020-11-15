<?php

namespace One\Transforms\InferTypesPlugins\ResolveEnumMemberAccess;

use One\Transforms\InferTypesPlugins\Helpers\InferTypesPlugin\InferTypesPlugin;
use One\Ast\Expressions\Expression;
use One\Ast\Expressions\PropertyAccessExpression;
use One\Ast\References\EnumMemberReference;
use One\Ast\References\EnumReference;

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
        $member = \OneLang\ArrayHelper::find($enumMemberRef->decl->values, function ($x) use ($pa) { return $x->name === $pa->propertyName; });
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
