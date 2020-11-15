<?php

namespace One\Transforms\FillMutabilityInfo;

use One\AstTransformer\AstTransformer;
use One\Ast\Types\IVariable;
use One\Ast\Types\MutabilityInfo;
use One\Ast\Expressions\Expression;
use One\Ast\Expressions\BinaryExpression;
use One\Ast\Expressions\InstanceMethodCallExpression;
use One\Ast\References\VariableReference;
use One\Ast\Statements\VariableDeclaration;

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
