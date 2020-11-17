<?php

namespace OneLang\One\Transforms\ConvertToMethodCall;

use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\Ast\Expressions\ElementAccessExpression;
use OneLang\One\Ast\Expressions\UnresolvedCallExpression;
use OneLang\One\Ast\Expressions\PropertyAccessExpression;
use OneLang\One\Ast\Expressions\BinaryExpression;
use OneLang\One\Ast\Expressions\StringLiteral;
use OneLang\One\AstTransformer\AstTransformer;

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
