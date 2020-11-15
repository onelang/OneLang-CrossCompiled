<?php

namespace One\Transforms\InferTypesPlugins\TypeScriptNullCoalesce;

use One\Transforms\InferTypesPlugins\Helpers\InferTypesPlugin\InferTypesPlugin;
use One\Ast\Expressions\Expression;
use One\Ast\Expressions\BinaryExpression;
use One\Ast\Expressions\NullLiteral;
use One\Ast\Expressions\NullCoalesceExpression;
use One\Ast\Expressions\ArrayLiteral;
use One\Ast\Expressions\MapLiteral;
use One\Ast\AstTypes\ClassType;
use One\Ast\AstTypes\TypeHelper;

class TypeScriptNullCoalesce extends InferTypesPlugin {
    function __construct() {
        parent::__construct("TypeScriptNullCoalesce");
        
    }
    
    function canTransform($expr) {
        return $expr instanceof BinaryExpression && $expr->operator === "||";
    }
    
    function transform($expr) {
        if ($expr instanceof BinaryExpression && $expr->operator === "||") {
            $litTypes = $this->main->currentFile->literalTypes;
            
            $expr->left = $this->main->runPluginsOn($expr->left);
            $leftType = $expr->left->getType();
            
            if ($expr->right instanceof ArrayLiteral && count($expr->right->items) === 0) {
                if ($leftType instanceof ClassType && $leftType->decl === $litTypes->array->decl) {
                    $expr->right->setActualType($leftType);
                    return new NullCoalesceExpression($expr->left, $expr->right);
                }
            }
            
            if ($expr->right instanceof MapLiteral && count($expr->right->items) === 0) {
                if ($leftType instanceof ClassType && $leftType->decl === $litTypes->map->decl) {
                    $expr->right->setActualType($leftType);
                    return new NullCoalesceExpression($expr->left, $expr->right);
                }
            }
            
            $expr->right = $this->main->runPluginsOn($expr->right);
            $rightType = $expr->right->getType();
            
            if ($expr->right instanceof NullLiteral)
                // something-which-can-be-undefined || null
                return $expr->left;
            else if (TypeHelper::isAssignableTo($rightType, $leftType) && !TypeHelper::equals($rightType, $this->main->currentFile->literalTypes->boolean))
                return new NullCoalesceExpression($expr->left, $expr->right);
        }
        return $expr;
    }
}
