<?php

namespace OneLang\One\Transforms\FillMutabilityInfo;

use OneLang\One\AstTransformer\AstTransformer;
use OneLang\One\Ast\Types\IVariable;
use OneLang\One\Ast\Types\MutabilityInfo;
use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\Ast\Expressions\BinaryExpression;
use OneLang\One\Ast\Expressions\InstanceMethodCallExpression;
use OneLang\One\Ast\References\VariableReference;
use OneLang\One\Ast\Statements\VariableDeclaration;

class FillMutabilityInfo extends AstTransformer {
    function __construct() {
        parent::__construct("FillMutabilityInfo");
        
    }
    
    protected function getVar($varRef) {
        $v = $varRef->getVariable();
        if ($v->mutability === null)
            $v->mutability = new MutabilityInfo(true, false, false);
        return $v;
    }
    
    protected function visitVariableReference($varRef) {
        $this->getVar($varRef)->mutability->unused = false;
        return $varRef;
    }
    
    protected function visitVariableDeclaration($stmt) {
        parent::visitVariableDeclaration($stmt);
        if ($stmt->attributes !== null && @$stmt->attributes["mutated"] ?? null === "true")
            $stmt->mutability->mutated = true;
        return $stmt;
    }
    
    protected function visitExpression($expr) {
        $expr = parent::visitExpression($expr);
        
        if ($expr instanceof BinaryExpression && $expr->left instanceof VariableReference && $expr->operator === "=")
            $this->getVar($expr->left)->mutability->reassigned = true;
        else if ($expr instanceof InstanceMethodCallExpression && $expr->object instanceof VariableReference && array_key_exists("mutates", $expr->method->attributes))
            $this->getVar($expr->object)->mutability->mutated = true;
        return $expr;
    }
    
    protected function visitVariable($variable) {
        if ($variable->mutability === null)
            $variable->mutability = new MutabilityInfo(true, false, false);
        return $variable;
    }
}
