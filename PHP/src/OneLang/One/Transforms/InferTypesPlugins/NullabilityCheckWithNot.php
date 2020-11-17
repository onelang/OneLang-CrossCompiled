<?php

namespace OneLang\One\Transforms\InferTypesPlugins\NullabilityCheckWithNot;

use OneLang\One\Transforms\InferTypesPlugins\Helpers\InferTypesPlugin\InferTypesPlugin;
use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\Ast\Expressions\UnaryExpression;
use OneLang\One\Ast\Expressions\BinaryExpression;
use OneLang\One\Ast\Expressions\NullLiteral;
use OneLang\One\Ast\AstTypes\ClassType;

class NullabilityCheckWithNot extends InferTypesPlugin {
    function __construct() {
        parent::__construct("NullabilityCheckWithNot");
        
    }
    
    function canTransform($expr) {
        return $expr instanceof UnaryExpression ? $expr->operator === "!" : false;
    }
    
    function transform($expr) {
        $unaryExpr = $expr;
        if ($unaryExpr->operator === "!") {
            $this->main->processExpression($expr);
            $type = $unaryExpr->operand->actualType;
            $litTypes = $this->main->currentFile->literalTypes;
            if ($type instanceof ClassType && $type->decl !== $litTypes->boolean->decl && $type->decl !== $litTypes->numeric->decl)
                return new BinaryExpression($unaryExpr->operand, "==", new NullLiteral());
        }
        
        return $expr;
    }
}
