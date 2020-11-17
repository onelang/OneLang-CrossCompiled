<?php

namespace OneLang\One\Transforms\FillParent;

use OneLang\One\Ast\Types\SourceFile;
use OneLang\One\Ast\Types\Method;
use OneLang\One\Ast\Types\IInterface;
use OneLang\One\Ast\Types\Enum;
use OneLang\One\Ast\Types\Interface_;
use OneLang\One\Ast\Types\Class_;
use OneLang\One\Ast\Types\Field;
use OneLang\One\Ast\Types\Property;
use OneLang\One\Ast\Types\IAstNode;
use OneLang\One\Ast\Types\IMethodBase;
use OneLang\One\Ast\Types\Constructor;
use OneLang\One\Ast\Types\GlobalFunction;
use OneLang\One\Ast\Types\Lambda;
use OneLang\One\Ast\Statements\Statement;
use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\AstTransformer\AstTransformer;

class FillParent extends AstTransformer {
    public $parentNodeStack;
    
    function __construct() {
        parent::__construct("FillParent");
        $this->parentNodeStack = array();
    }
    
    protected function visitExpression($expr) {
        if (count($this->parentNodeStack) === 0) { }
        $expr->parentNode = $this->parentNodeStack[count($this->parentNodeStack) - 1];
        $this->parentNodeStack[] = $expr;
        parent::visitExpression($expr);
        array_pop($this->parentNodeStack);
        return $expr;
    }
    
    protected function visitStatement($stmt) {
        $this->parentNodeStack[] = $stmt;
        parent::visitStatement($stmt);
        array_pop($this->parentNodeStack);
        return $stmt;
    }
    
    protected function visitEnum($enum_) {
        $enum_->parentFile = $this->currentFile;
        parent::visitEnum($enum_);
        foreach ($enum_->values as $value)
            $value->parentEnum = $enum_;
    }
    
    protected function visitInterface($intf) {
        $intf->parentFile = $this->currentFile;
        parent::visitInterface($intf);
    }
    
    protected function visitClass($cls) {
        $cls->parentFile = $this->currentFile;
        parent::visitClass($cls);
    }
    
    protected function visitGlobalFunction($func) {
        $func->parentFile = $this->currentFile;
        parent::visitGlobalFunction($func);
    }
    
    protected function visitField($field) {
        $field->parentInterface = $this->currentInterface;
        
        $this->parentNodeStack[] = $field;
        parent::visitField($field);
        array_pop($this->parentNodeStack);
    }
    
    protected function visitProperty($prop) {
        $prop->parentClass = $this->currentInterface;
        
        $this->parentNodeStack[] = $prop;
        parent::visitProperty($prop);
        array_pop($this->parentNodeStack);
    }
    
    protected function visitMethodBase($method) {
        if ($method instanceof Constructor)
            $method->parentClass = $this->currentInterface;
        else if ($method instanceof Method)
            $method->parentInterface = $this->currentInterface;
        else if ($method instanceof GlobalFunction) { }
        else if ($method instanceof Lambda) { }
        else { }
        
        foreach ($method->parameters as $param)
            $param->parentMethod = $method;
        
        $this->parentNodeStack[] = $method;
        parent::visitMethodBase($method);
        array_pop($this->parentNodeStack);
    }
    
    function visitFile($file) {
        foreach ($file->imports as $imp)
            $imp->parentFile = $file;
        
        parent::visitFile($file);
    }
}
