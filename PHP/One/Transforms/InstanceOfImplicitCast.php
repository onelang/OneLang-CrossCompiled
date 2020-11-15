<?php

namespace One\Transforms\InstanceOfImplicitCast;

use One\AstTransformer\AstTransformer;
use One\Ast\Expressions\InstanceOfExpression;
use One\Ast\Expressions\BinaryExpression;
use One\Ast\Expressions\Expression;
use One\Ast\Expressions\CastExpression;
use One\Ast\Expressions\ConditionalExpression;
use One\Ast\Expressions\PropertyAccessExpression;
use One\Ast\Statements\Statement;
use One\Ast\Statements\IfStatement;
use One\Ast\Statements\WhileStatement;
use One\Ast\References\ForeachVariableReference;
use One\Ast\References\VariableDeclarationReference;
use One\Ast\References\MethodParameterReference;
use One\Ast\References\InstanceFieldReference;
use One\Ast\References\ThisReference;
use One\Ast\References\Reference;
use One\Ast\References\StaticThisReference;
use Utils\ArrayHelper\ArrayHelper;

class InstanceOfImplicitCast extends AstTransformer {
    public $casts;
    public $castCounts;
    
    function __construct() {
        parent::__construct("InstanceOfImplicitCast");
        $this->casts = array();
        $this->castCounts = array();
    }
    
    protected function addCast($cast) {
        if (count($this->castCounts) > 0) {
            $cast->implicitCasts = array();
            $this->casts[] = $cast;
            $last = count($this->castCounts) - 1;
            $this->castCounts[$last] = $this->castCounts[$last] + 1;
        }
    }
    
    protected function pushContext() {
        $this->castCounts[] = 0;
    }
    
    protected function popContext() {
        $castCount = array_pop($this->castCounts);
        if ($castCount !== 0)
            array_splice($this->casts, -$castCount);
    }
    
    protected function equals($expr1, $expr2) {
        // implicit casts don't matter when checking equality...
        while ($expr1 instanceof CastExpression && $expr1->instanceOfCast !== null)
            $expr1 = $expr1->expression;
        while ($expr2 instanceof CastExpression && $expr2->instanceOfCast !== null)
            $expr2 = $expr2->expression;
        
        // MetP, V, MethP.PA, V.PA, MethP/V [ {FEVR} ], FEVR
        if ($expr1 instanceof PropertyAccessExpression)
            return $expr2 instanceof PropertyAccessExpression && $expr1->propertyName === $expr2->propertyName && $this->equals($expr1->object, $expr2->object);
        else if ($expr1 instanceof VariableDeclarationReference)
            return $expr2 instanceof VariableDeclarationReference && $expr1->decl === $expr2->decl;
        else if ($expr1 instanceof MethodParameterReference)
            return $expr2 instanceof MethodParameterReference && $expr1->decl === $expr2->decl;
        else if ($expr1 instanceof ForeachVariableReference)
            return $expr2 instanceof ForeachVariableReference && $expr1->decl === $expr2->decl;
        else if ($expr1 instanceof InstanceFieldReference)
            return $expr2 instanceof InstanceFieldReference && $expr1->field === $expr2->field;
        else if ($expr1 instanceof ThisReference)
            return $expr2 instanceof ThisReference;
        else if ($expr1 instanceof StaticThisReference)
            return $expr2 instanceof StaticThisReference;
        return false;
    }
    
    protected function visitExpression($expr) {
        $result = $expr;
        if ($expr instanceof InstanceOfExpression) {
            $this->visitExpression($expr->expr);
            $this->addCast($expr);
        }
        else if ($expr instanceof BinaryExpression && $expr->operator === "&&") {
            $expr->left = $this->visitExpression($expr->left);
            $expr->right = $this->visitExpression($expr->right);
        }
        else if ($expr instanceof ConditionalExpression) {
            $this->pushContext();
            $expr->condition = $this->visitExpression($expr->condition);
            $expr->whenTrue = $this->visitExpression($expr->whenTrue);
            $this->popContext();
            
            $expr->whenFalse = $this->visitExpression($expr->whenFalse);
        }
        else if ($expr instanceof Reference && $expr->parentNode instanceof BinaryExpression && $expr->parentNode->operator === "=" && $expr->parentNode->left === $expr) { }
        else {
            $this->pushContext();
            $result = parent::visitExpression($expr);
            $this->popContext();
            // @java final var result2 = result;
            $result2 = $result;
            $match = \OneLang\ArrayHelper::find($this->casts, function ($cast) use ($result2) { return $this->equals($result2, $cast->expr); });
            if ($match !== null) {
                $castExpr = new CastExpression($match->checkType, $result);
                $castExpr->instanceOfCast = $match;
                $match->implicitCasts[] = $castExpr;
                $result = $castExpr;
            }
        }
        return $result;
    }
    
    protected function visitStatement($stmt) {
        $this->currentStatement = $stmt;
        
        if ($stmt instanceof IfStatement) {
            $this->pushContext();
            $stmt->condition = $this->visitExpression($stmt->condition);
            $this->visitBlock($stmt->then);
            $this->popContext();
            
            if ($stmt->else_ !== null)
                $this->visitBlock($stmt->else_);
        }
        else if ($stmt instanceof WhileStatement) {
            $this->pushContext();
            $stmt->condition = $this->visitExpression($stmt->condition);
            $this->visitBlock($stmt->body);
            $this->popContext();
        }
        else {
            $this->pushContext();
            parent::visitStatement($stmt);
            $this->popContext();
        }
        
        return $stmt;
    }
}
