<?php

namespace One\Transforms\ConvertToMethodCall;

use One\Ast\Expressions\Expression;
use One\Ast\Expressions\ElementAccessExpression;
use One\Ast\Expressions\UnresolvedCallExpression;
use One\Ast\Expressions\PropertyAccessExpression;
use One\Ast\Expressions\BinaryExpression;
use One\Ast\Expressions\StringLiteral;
use One\AstTransformer\AstTransformer;

class ConvertToMethodCall extends AstTransformer {
    function __construct() {
        parent::__construct("ConvertToMethodCall");
        
    }
    
    protected function visitExpression($expr) {
        $origExpr = $expr;
        
        $expr = parent::visitExpression($expr);
        
        if ($expr instanceof BinaryExpression && $expr->operator === "in")
            $expr = new UnresolvedCallExpression(new PropertyAccessExpression($expr->right, "hasKey"), array(), array($expr->left));
        
        $expr->parentNode = $origExpr->parentNode;
        return $expr;
    }
}
