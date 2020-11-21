<?php

namespace OneLang\One\Transforms\ResolveIdentifiers;

use OneLang\One\AstTransformer\AstTransformer;
use OneLang\One\Ast\Types\SourceFile;
use OneLang\One\Ast\Types\Class_;
use OneLang\One\Ast\Types\Enum;
use OneLang\One\Ast\Types\Method;
use OneLang\One\Ast\Types\Lambda;
use OneLang\One\Ast\Types\GlobalFunction;
use OneLang\One\Ast\Types\IMethodBase;
use OneLang\One\Ast\Types\Constructor;
use OneLang\One\Ast\Types\Interface_;
use OneLang\One\ErrorManager\ErrorManager;
use OneLang\One\Ast\Expressions\Identifier;
use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\Ast\References\IReferencable;
use OneLang\One\Ast\References\Reference;
use OneLang\One\Ast\References\StaticThisReference;
use OneLang\One\Ast\References\ThisReference;
use OneLang\One\Ast\References\SuperReference;
use OneLang\One\Ast\Statements\VariableDeclaration;
use OneLang\One\Ast\Statements\ForStatement;
use OneLang\One\Ast\Statements\ForeachStatement;
use OneLang\One\Ast\Statements\Statement;
use OneLang\One\Ast\Statements\IfStatement;
use OneLang\One\Ast\Statements\TryStatement;
use OneLang\One\Ast\Statements\Block;
use OneLang\One\Ast\AstTypes\ClassType;

class SymbolLookup {
    public $errorMan;
    public $levelStack;
    public $levelNames;
    public $currLevel;
    public $symbols;
    
    function __construct()
    {
        $this->errorMan = new ErrorManager();
        $this->levelStack = array();
        $this->levelNames = array();
        $this->symbols = new \OneLang\Core\Map();
    }
    
    function throw($msg) {
        $this->errorMan->throw($msg . " (context: " . implode(" > ", $this->levelNames) . ")");
    }
    
    function pushContext($name) {
        $this->levelStack[] = $this->currLevel;
        $this->levelNames[] = $name;
        $this->currLevel = array();
    }
    
    function addSymbol($name, $ref) {
        if ($this->symbols->has($name))
            $this->throw("Symbol shadowing: " . $name);
        $this->symbols->set($name, $ref);
        $this->currLevel[] = $name;
    }
    
    function popContext() {
        foreach ($this->currLevel as $name)
            $this->symbols->delete($name);
        array_pop($this->levelNames);
        $this->currLevel = count($this->levelStack) > 0 ? array_pop($this->levelStack) : null;
    }
    
    function getSymbol($name) {
        return $this->symbols->get($name);
    }
}

class ResolveIdentifiers extends AstTransformer {
    public $symbolLookup;
    
    function __construct() {
        parent::__construct("ResolveIdentifiers");
        $this->symbolLookup = new SymbolLookup();
    }
    
    protected function visitIdentifier($id) {
        parent::visitIdentifier($id);
        $symbol = $this->symbolLookup->getSymbol($id->text);
        if ($symbol === null) {
            $this->errorMan->throw("Identifier '" . $id->text . "' was not found in available symbols");
            return $id;
        }
        
        $ref = null;
        if ($symbol instanceof Class_ && $id->text === "this") {
            $withinStaticMethod = $this->currentMethod instanceof Method && $this->currentMethod->isStatic;
            $ref = $withinStaticMethod ? new StaticThisReference($symbol) : new ThisReference($symbol);
        }
        else if ($symbol instanceof Class_ && $id->text === "super")
            $ref = new SuperReference($symbol);
        else {
            $ref = $symbol->createReference();
            if ($ref === null)
                $this->errorMan->throw("createReference() should not return null!");
        }
        $ref->parentNode = $id->parentNode;
        return $ref;
    }
    
    protected function visitStatement($stmt) {
        if ($stmt instanceof ForStatement) {
            $this->symbolLookup->pushContext("For");
            if ($stmt->itemVar !== null)
                $this->symbolLookup->addSymbol($stmt->itemVar->name, $stmt->itemVar);
            parent::visitStatement($stmt);
            $this->symbolLookup->popContext();
        }
        else if ($stmt instanceof ForeachStatement) {
            $this->symbolLookup->pushContext("Foreach");
            $this->symbolLookup->addSymbol($stmt->itemVar->name, $stmt->itemVar);
            parent::visitStatement($stmt);
            $this->symbolLookup->popContext();
        }
        else if ($stmt instanceof TryStatement) {
            $this->symbolLookup->pushContext("Try");
            $this->visitBlock($stmt->tryBody);
            if ($stmt->catchBody !== null) {
                $this->symbolLookup->addSymbol($stmt->catchVar->name, $stmt->catchVar);
                $this->visitBlock($stmt->catchBody);
                $this->symbolLookup->popContext();
            }
            if ($stmt->finallyBody !== null)
                $this->visitBlock($stmt->finallyBody);
        }
        else
            return parent::visitStatement($stmt);
        return $stmt;
    }
    
    protected function visitLambda($lambda) {
        $this->symbolLookup->pushContext("Lambda");
        foreach ($lambda->parameters as $param)
            $this->symbolLookup->addSymbol($param->name, $param);
        parent::visitBlock($lambda->body);
        // directly process method's body without opening a new scope again
        $this->symbolLookup->popContext();
        return $lambda;
    }
    
    protected function visitBlock($block) {
        $this->symbolLookup->pushContext("block");
        parent::visitBlock($block);
        $this->symbolLookup->popContext();
        return $block;
    }
    
    protected function visitVariableDeclaration($stmt) {
        $this->symbolLookup->addSymbol($stmt->name, $stmt);
        return parent::visitVariableDeclaration($stmt);
    }
    
    protected function visitMethodBase($method) {
        $this->symbolLookup->pushContext($method instanceof Method ? "Method: " . $method->name : ($method instanceof Constructor ? "constructor" : "???"));
        
        foreach ($method->parameters as $param) {
            $this->symbolLookup->addSymbol($param->name, $param);
            if ($param->initializer !== null)
                $this->visitExpression($param->initializer);
        }
        
        if ($method->body !== null)
            parent::visitBlock($method->body);
        // directly process method's body without opening a new scope again
        
        $this->symbolLookup->popContext();
    }
    
    protected function visitClass($cls) {
        $this->symbolLookup->pushContext("Class: " . $cls->name);
        $this->symbolLookup->addSymbol("this", $cls);
        if ($cls->baseClass instanceof ClassType)
            $this->symbolLookup->addSymbol("super", $cls->baseClass->decl);
        parent::visitClass($cls);
        $this->symbolLookup->popContext();
    }
    
    function visitFile($sourceFile) {
        $this->errorMan->resetContext($this);
        $this->symbolLookup->pushContext("File: " . $sourceFile->sourcePath->toString());
        
        foreach ($sourceFile->availableSymbols->values() as $symbol) {
            if ($symbol instanceof Class_)
                $this->symbolLookup->addSymbol($symbol->name, $symbol);
            else if ($symbol instanceof Interface_) { }
            else if ($symbol instanceof Enum)
                $this->symbolLookup->addSymbol($symbol->name, $symbol);
            else if ($symbol instanceof GlobalFunction)
                $this->symbolLookup->addSymbol($symbol->name, $symbol);
            else { }
        }
        
        parent::visitFile($sourceFile);
        
        $this->symbolLookup->popContext();
        $this->errorMan->resetContext();
    }
}
