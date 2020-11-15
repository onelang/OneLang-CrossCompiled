<?php

namespace One\Transforms\InferTypesPlugins\ResolveElementAccess;

use One\Transforms\InferTypesPlugins\Helpers\InferTypesPlugin\InferTypesPlugin;
use One\Ast\Expressions\Expression;
use One\Ast\Expressions\ElementAccessExpression;
use One\Ast\Expressions\UnresolvedMethodCallExpression;
use One\Ast\Expressions\InstanceMethodCallExpression;
use One\Ast\Expressions\StringLiteral;
use One\Ast\Expressions\PropertyAccessExpression;
use One\Ast\Expressions\BinaryExpression;
use One\Transforms\InferTypesPlugins\ResolveMethodCalls\ResolveMethodCalls;
use One\Ast\Interfaces\IType;
use One\Ast\AstTypes\TypeHelper;

class ResolveElementAccess extends InferTypesPlugin {
    function __construct() {
        parent::__construct("ResolveElementAccess");
        
    }
    
    function canTransform($expr) {
        $isSet = $expr instanceof BinaryExpression && $expr->left instanceof ElementAccessExpression && in_array($expr->operator, array("="));
        return $expr instanceof ElementAccessExpression || $isSet;
    }
    
    function isMapOrArrayType($type) {
        return TypeHelper::isAssignableTo($type, $this->main->currentFile->literalTypes->map) || \OneLang\ArrayHelper::some($this->main->currentFile->arrayTypes, function ($x) use ($type) { return TypeHelper::isAssignableTo($type, $x); });
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
