<?php

namespace One\Transforms\LambdaCaptureCollector;

use One\AstTransformer\AstTransformer;
use One\Ast\AstTypes\UnresolvedType;
use One\Ast\AstTypes\GenericsType;
use One\Ast\Types\Class_;
use One\Ast\Types\IVariable;
use One\Ast\Types\Lambda;
use One\Ast\Types\Method;
use One\Ast\Interfaces\IType;
use One\Ast\References\InstanceFieldReference;
use One\Ast\References\InstancePropertyReference;
use One\Ast\References\StaticFieldReference;
use One\Ast\References\StaticPropertyReference;
use One\Ast\References\VariableReference;

class LambdaCaptureCollector extends AstTransformer {
    public $scopeVarStack;
    public $scopeVars;
    public $capturedVars;
    
    function __construct() {
        parent::__construct("LambdaCaptureCollector");
        $this->scopeVarStack = array();
        $this->scopeVars = null;
        $this->capturedVars = null;
    }
    
    protected function visitLambda($lambda) {
        if ($this->scopeVars !== null)
            $this->scopeVarStack[] = $this->scopeVars;
        
        $this->scopeVars = new \OneLang\Set();
        $this->capturedVars = new \OneLang\Set();
        
        parent::visitLambda($lambda);
        $lambda->captures = array();
        foreach ($this->capturedVars->values() as $capture)
            $lambda->captures[] = $capture;
        
        $this->scopeVars = count($this->scopeVarStack) > 0 ? array_pop($this->scopeVarStack) : null;
        return $lambda;
    }
    
    protected function visitVariable($variable) {
        if ($this->scopeVars !== null)
            $this->scopeVars->add($variable);
        return $variable;
    }
    
    protected function visitVariableReference($varRef) {
        if ($varRef instanceof StaticFieldReference || $varRef instanceof InstanceFieldReference || $varRef instanceof StaticPropertyReference || $varRef instanceof InstancePropertyReference || $this->scopeVars === null)
            return $varRef;
        
        $vari = $varRef->getVariable();
        if (!$this->scopeVars->has($vari))
            $this->capturedVars->add($vari);
        
        return $varRef;
    }
}
