<?php

namespace OneLang\One\Ast\Statements;

use OneLang\One\Ast\Types\IVariableWithInitializer;
use OneLang\One\Ast\Types\IVariable;
use OneLang\One\Ast\Types\IHasAttributesAndTrivia;
use OneLang\One\Ast\Types\IAstNode;
use OneLang\One\Ast\Types\MutabilityInfo;
use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\Ast\References\ForVariableReference;
use OneLang\One\Ast\References\ForeachVariableReference;
use OneLang\One\Ast\References\VariableDeclarationReference;
use OneLang\One\Ast\References\IReferencable;
use OneLang\One\Ast\References\Reference;
use OneLang\One\Ast\References\CatchVariableReference;
use OneLang\One\Ast\Interfaces\IType;

class Statement implements IHasAttributesAndTrivia, IAstNode {
    public $leadingTrivia;
    public $attributes;
    
    function __construct() {
        $this->leadingTrivia = null;
        $this->attributes = null;
    }
}

class IfStatement extends Statement {
    public $condition;
    public $then;
    public $else_;
    
    function __construct($condition, $then, $else_) {
        parent::__construct();
        $this->condition = $condition;
        $this->then = $then;
        $this->else_ = $else_;
    }
}

class ReturnStatement extends Statement {
    public $expression;
    
    function __construct($expression) {
        parent::__construct();
        $this->expression = $expression;
    }
}

class ThrowStatement extends Statement {
    public $expression;
    
    function __construct($expression) {
        parent::__construct();
        $this->expression = $expression;
    }
}

class ExpressionStatement extends Statement {
    public $expression;
    
    function __construct($expression) {
        parent::__construct();
        $this->expression = $expression;
    }
}

class BreakStatement extends Statement {
    
}

class ContinueStatement extends Statement {
    
}

class UnsetStatement extends Statement {
    public $expression;
    
    function __construct($expression) {
        parent::__construct();
        $this->expression = $expression;
    }
}

class VariableDeclaration extends Statement implements IVariableWithInitializer, IReferencable {
    public $name;
    public $type;
    public $initializer;
    public $references;
    public $mutability;
    
    function __construct($name, $type, $initializer) {
        parent::__construct();
        $this->name = $name;
        $this->type = $type;
        $this->initializer = $initializer;
        $this->references = array();
        $this->mutability = null;
    }
    
    function createReference() {
        return new VariableDeclarationReference($this);
    }
}

class WhileStatement extends Statement {
    public $condition;
    public $body;
    
    function __construct($condition, $body) {
        parent::__construct();
        $this->condition = $condition;
        $this->body = $body;
    }
}

class DoStatement extends Statement {
    public $condition;
    public $body;
    
    function __construct($condition, $body) {
        parent::__construct();
        $this->condition = $condition;
        $this->body = $body;
    }
}

class ForeachVariable implements IVariable, IReferencable {
    public $name;
    public $type;
    public $references;
    public $mutability;
    
    function __construct($name) {
        $this->name = $name;
        $this->type = null;
        $this->references = array();
        $this->mutability = null;
    }
    
    function createReference() {
        return new ForeachVariableReference($this);
    }
}

class ForeachStatement extends Statement {
    public $itemVar;
    public $items;
    public $body;
    
    function __construct($itemVar, $items, $body) {
        parent::__construct();
        $this->itemVar = $itemVar;
        $this->items = $items;
        $this->body = $body;
    }
}

class ForVariable implements IVariableWithInitializer, IReferencable {
    public $name;
    public $type;
    public $initializer;
    public $references;
    public $mutability;
    
    function __construct($name, $type, $initializer) {
        $this->name = $name;
        $this->type = $type;
        $this->initializer = $initializer;
        $this->references = array();
        $this->mutability = null;
    }
    
    function createReference() {
        return new ForVariableReference($this);
    }
}

class ForStatement extends Statement {
    public $itemVar;
    public $condition;
    public $incrementor;
    public $body;
    
    function __construct($itemVar, $condition, $incrementor, $body) {
        parent::__construct();
        $this->itemVar = $itemVar;
        $this->condition = $condition;
        $this->incrementor = $incrementor;
        $this->body = $body;
    }
}

class CatchVariable implements IVariable, IReferencable {
    public $name;
    public $type;
    public $references;
    public $mutability;
    
    function __construct($name, $type) {
        $this->name = $name;
        $this->type = $type;
        $this->references = array();
        $this->mutability = null;
    }
    
    function createReference() {
        return new CatchVariableReference($this);
    }
}

class TryStatement extends Statement {
    public $tryBody;
    public $catchVar;
    public $catchBody;
    public $finallyBody;
    
    function __construct($tryBody, $catchVar, $catchBody, $finallyBody) {
        parent::__construct();
        $this->tryBody = $tryBody;
        $this->catchVar = $catchVar;
        $this->catchBody = $catchBody;
        $this->finallyBody = $finallyBody;
        if ($this->catchBody === null && $this->finallyBody === null)
            throw new \OneLang\Core\Error("try without catch and finally is not allowed");
    }
}

class Block {
    public $statements;
    
    function __construct($statements) {
        $this->statements = $statements;
    }
}
