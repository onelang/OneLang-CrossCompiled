<?php

namespace OneLang\One\Transforms\ConvertNullCoalesce;

use OneLang\Utils\TSOverviewGenerator\TSOverviewGenerator;
use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\Ast\Expressions\IMethodCallExpression;
use OneLang\One\Ast\Expressions\InstanceMethodCallExpression;
use OneLang\One\Ast\Expressions\NullCoalesceExpression;
use OneLang\One\Ast\Expressions\StaticMethodCallExpression;
use OneLang\One\Ast\References\InstanceFieldReference;
use OneLang\One\Ast\References\StaticFieldReference;
use OneLang\One\Ast\References\VariableDeclarationReference;
use OneLang\One\Ast\Statements\Block;
use OneLang\One\Ast\Statements\Statement;
use OneLang\One\Ast\Statements\VariableDeclaration;
use OneLang\One\Ast\Types\IMethodBase;
use OneLang\One\Ast\Types\IVariable;
use OneLang\One\Ast\Types\Lambda;
use OneLang\One\Ast\Types\MutabilityInfo;
use OneLang\One\AstTransformer\AstTransformer;

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
        $this->usageCount = new \OneLang\Core\Map();
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
        $this->usageCount = new \OneLang\Core\Map();
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
        // @csharp var prevStatements = this.statements;
        // @java var prevStatements = this.statements;
        $prevStatements = $this->statements;
        $this->statements = array();
        foreach ($block->statements as $stmt)
            $this->statements[] = $this->visitStatement($stmt);
        $block->statements = $this->statements;
        // @csharp this.statements = prevStatements;
        // @java this.statements = prevStatements;
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
