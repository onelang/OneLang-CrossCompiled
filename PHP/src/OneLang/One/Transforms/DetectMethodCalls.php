<?php

namespace OneLang\One\Transforms\DetectMethodCalls;

use OneLang\One\AstTransformer\AstTransformer;
use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\Ast\Expressions\UnresolvedCallExpression;
use OneLang\One\Ast\Expressions\PropertyAccessExpression;
use OneLang\One\Ast\Expressions\UnresolvedMethodCallExpression;

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
