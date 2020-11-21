<?php

namespace OneLang\One\Transforms\InferTypesPlugins\ResolveElementAccess;

use OneLang\One\Transforms\InferTypesPlugins\Helpers\InferTypesPlugin\InferTypesPlugin;
use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\Ast\Expressions\ElementAccessExpression;
use OneLang\One\Ast\Expressions\UnresolvedMethodCallExpression;
use OneLang\One\Ast\Expressions\InstanceMethodCallExpression;
use OneLang\One\Ast\Expressions\StringLiteral;
use OneLang\One\Ast\Expressions\PropertyAccessExpression;
use OneLang\One\Ast\Expressions\BinaryExpression;
use OneLang\One\Transforms\InferTypesPlugins\ResolveMethodCalls\ResolveMethodCalls;
use OneLang\One\Ast\Interfaces\IType;
use OneLang\One\Ast\AstTypes\TypeHelper;

class ResolveElementAccess extends InferTypesPlugin {
    function __construct() {
        parent::__construct("ResolveElementAccess");
        
    }
    
    function canTransform($expr) {
        $isSet = $expr instanceof BinaryExpression && $expr->left instanceof ElementAccessExpression && in_array($expr->operator, array("="));
        return $expr instanceof ElementAccessExpression || $isSet;
    }
    
    function isMapOrArrayType($type) {
        return TypeHelper::isAssignableTo($type, $this->main->currentFile->literalTypes->map) || \OneLang\Core\ArrayHelper::some($this->main->currentFile->arrayTypes, function ($x) use ($type) { return TypeHelper::isAssignableTo($type, $x); });
    }
    
    function transform($expr) {
        // TODO: convert ElementAccess to ElementGet and ElementSet expressions
        if ($expr instanceof BinaryExpression && $expr->left instanceof ElementAccessExpression) {
            $expr->left->object = $this->main->runPluginsOn($expr->left->object);
            if ($this->isMapOrArrayType($expr->left->object->getType()))
                //const right = expr.operator === "=" ? expr.right : new BinaryExpression(<Expression>expr.left.clone(), expr.operator === "+=" ? "+" : "-", expr.right);
                return new UnresolvedMethodCallExpression($expr->left->object, "set", array(), array($expr->left->elementExpr, $expr->right));
        }
        else if ($expr instanceof ElementAccessExpression) {
            $expr->object = $this->main->runPluginsOn($expr->object);
            if ($this->isMapOrArrayType($expr->object->getType()))
                return new UnresolvedMethodCallExpression($expr->object, "get", array(), array($expr->elementExpr));
            else if ($expr->elementExpr instanceof StringLiteral)
                return new PropertyAccessExpression($expr->object, $expr->elementExpr->stringValue);
        }
        return $expr;
    }
}
