<?php

namespace One\Transforms\ConvertNullCoalesce;

use Utils\TSOverviewGenerator\TSOverviewGenerator;
use One\Ast\Expressions\Expression;
use One\Ast\Expressions\IMethodCallExpression;
use One\Ast\Expressions\InstanceMethodCallExpression;
use One\Ast\Expressions\NullCoalesceExpression;
use One\Ast\Expressions\StaticMethodCallExpression;
use One\Ast\References\InstanceFieldReference;
use One\Ast\References\StaticFieldReference;
use One\Ast\References\VariableDeclarationReference;
use One\Ast\Statements\Block;
use One\Ast\Statements\Statement;
use One\Ast\Statements\VariableDeclaration;
use One\Ast\Types\IMethodBase;
use One\Ast\Types\IVariable;
use One\Ast\Types\Lambda;
use One\Ast\Types\MutabilityInfo;
use One\AstTransformer\AstTransformer;

interface IExpressionNamingStrategy {
    function getNameFor($expr);
}

class DefaultExpressionNamingStrategy implements IExpressionNamingStrategy {
    function getNameFor($expr) {
        if ($expr instanceof InstanceMethodCallExpression || $expr instanceof StaticMethodCallExpression)
            return ($expr)->method->name . "Result";
        return "result";
    }
}

class VariableNameHandler {
    public $usageCount;
    
    function __construct()
    {
        $this->usageCount = new \OneLang\Map();
    }
    
    function useName($name) {
        if ($this->usageCount->has($name)) {
            $newIdx = $this->usageCount->get($name) + 1;
            $this->usageCount->set($name, $newIdx);
            return $name . $newIdx;
        }
        else {
            $this->usageCount->set($name, 1);
            return $name;
        }
    }
    
    function resetScope() {
        $this->usageCount = new \OneLang\Map();
    }
}

class ConvertNullCoalesce extends AstTransformer {
    public $exprNaming;
    public $varNames;
    public $statements;
    
    function __construct() {
        parent::__construct("RemoveNullCoalesce");
        $this->exprNaming = new DefaultExpressionNamingStrategy();
        $this->varNames = new VariableNameHandler();
        $this->statements = array();
    }
    
    protected function visitVariable($variable) {
        $this->varNames->useName($variable->name);
        return parent::visitVariable($variable);
    }
    
    protected function visitMethodBase($methodBase) {
        if (!($methodBase instanceof Lambda))
            $this->varNames->resetScope();
        parent::visitMethodBase($methodBase);
    }
    
    protected function visitBlock($block) {
        $prevStatements = $this->statements;
        $this->statements = array();
        foreach ($block->statements as $stmt)
            $this->statements[] = $this->visitStatement($stmt);
        $block->statements = $this->statements;
        $this->statements = $prevStatements;
        return $block;
    }
    
    protected function visitExpression($expr) {
        $expr = parent::visitExpression($expr);
        if ($expr instanceof NullCoalesceExpression) {
            if ($expr->defaultExpr instanceof InstanceFieldReference || $expr->defaultExpr instanceof StaticFieldReference)
                return $expr;
            
            $varName = $this->varNames->useName($this->exprNaming->getNameFor($expr->defaultExpr));
            
            $varDecl = new VariableDeclaration($varName, $expr->defaultExpr->getType(), $expr->defaultExpr);
            $varDecl->mutability = new MutabilityInfo(false, false, false);
            $this->statements[] = $varDecl;
            
            $expr->defaultExpr = new VariableDeclarationReference($varDecl);
        }
        return $expr;
    }
}
