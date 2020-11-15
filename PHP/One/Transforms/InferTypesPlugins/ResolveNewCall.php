<?php

namespace One\Transforms\InferTypesPlugins\ResolveNewCall;

use One\Transforms\InferTypesPlugins\Helpers\InferTypesPlugin\InferTypesPlugin;
use One\Ast\Expressions\Expression;
use One\Ast\Expressions\NewExpression;

class ResolveNewCalls extends InferTypesPlugin {
    function __construct() {
        parent::__construct("ResolveNewCalls");
        
    }
    
    function canTransform($expr) {
        return $expr instanceof NewExpression;
    }
    
    function transform($expr) {
        $newExpr = $expr;
        for ($i = 0; $i < count($newExpr->args); $i++) {
            $newExpr->args[$i]->setExpectedType($newExpr->cls->decl->constructor_->parameters[$i]->type);
            $newExpr->args[$i] = $this->main->runPluginsOn($newExpr->args[$i]);
        }
        $expr->setActualType($newExpr->cls);
        return $expr;
    }
}
