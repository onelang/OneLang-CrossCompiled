<?php

namespace OneLang\One\Transforms\LambdaCaptureCollector;

use OneLang\One\AstTransformer\AstTransformer;
use OneLang\One\Ast\AstTypes\UnresolvedType;
use OneLang\One\Ast\AstTypes\GenericsType;
use OneLang\One\Ast\Types\Class_;
use OneLang\One\Ast\Types\IVariable;
use OneLang\One\Ast\Types\Lambda;
use OneLang\One\Ast\Types\Method;
use OneLang\One\Ast\Interfaces\IType;
use OneLang\One\Ast\References\InstanceFieldReference;
use OneLang\One\Ast\References\InstancePropertyReference;
use OneLang\One\Ast\References\StaticFieldReference;
use OneLang\One\Ast\References\StaticPropertyReference;
use OneLang\One\Ast\References\VariableReference;

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
        
        $this->scopeVars = new \OneLang\Core\Set();
        $this->capturedVars = new \OneLang\Core\Set();
        
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
