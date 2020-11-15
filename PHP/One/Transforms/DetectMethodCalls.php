<?php

namespace One\Transforms\DetectMethodCalls;

use One\AstTransformer\AstTransformer;
use One\Ast\Expressions\Expression;
use One\Ast\Expressions\UnresolvedCallExpression;
use One\Ast\Expressions\PropertyAccessExpression;
use One\Ast\Expressions\UnresolvedMethodCallExpression;

class DetectMethodCalls extends AstTransformer {
    function __construct() {
        parent::__construct("DetectMethodCalls");
        
    }
    
    protected function visitExpression($expr) {
        $expr = parent::visitExpression($expr);
        if ($expr instanceof UnresolvedCallExpression && $expr->func instanceof PropertyAccessExpression)
            return new UnresolvedMethodCallExpression($expr->func->object, $expr->func->propertyName, $expr->typeArgs, $expr->args);
        return $expr;
    }
}
