<?php

namespace OneLang\One\Transforms\InferTypesPlugins\ResolveFuncCalls;

use OneLang\One\Transforms\InferTypesPlugins\Helpers\InferTypesPlugin\InferTypesPlugin;
use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\Ast\Expressions\UnresolvedCallExpression;
use OneLang\One\Ast\Expressions\GlobalFunctionCallExpression;
use OneLang\One\Ast\Expressions\LambdaCallExpression;
use OneLang\One\Ast\References\GlobalFunctionReference;
use OneLang\One\Ast\AstTypes\LambdaType;

class ResolveFuncCalls extends InferTypesPlugin {
    function __construct() {
        parent::__construct("ResolveFuncCalls");
        
    }
    
    function canTransform($expr) {
        return $expr instanceof UnresolvedCallExpression;
    }
    
    function transform($expr) {
        $callExpr = $expr;
        if ($callExpr->func instanceof GlobalFunctionReference) {
            $newExpr = new GlobalFunctionCallExpression($callExpr->func->decl, $callExpr->args);
            $callExpr->args = array_map(function ($arg) { return $this->main->runPluginsOn($arg); }, $callExpr->args);
            $newExpr->setActualType($callExpr->func->decl->returns);
            return $newExpr;
        }
        else {
            $this->main->processExpression($expr);
            if ($callExpr->func->actualType instanceof LambdaType) {
                $newExpr = new LambdaCallExpression($callExpr->func, $callExpr->args);
                $newExpr->setActualType($callExpr->func->actualType->returnType);
                return $newExpr;
            }
            else
                return $expr;
        }
    }
}
