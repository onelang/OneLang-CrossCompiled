<?php

namespace One\Transforms\ResolveGenericTypeIdentifiers;

use One\AstTransformer\AstTransformer;
use One\Ast\AstTypes\UnresolvedType;
use One\Ast\AstTypes\GenericsType;
use One\Ast\Types\Class_;
use One\Ast\Types\Method;
use One\Ast\Interfaces\IType;

class ResolveGenericTypeIdentifiers extends AstTransformer {
    function __construct() {
        parent::__construct("ResolveGenericTypeIdentifiers");
        
    }
    
    protected function visitType($type) {
        parent::visitType($type);
        
        //console.log(type && type.constructor.name, JSON.stringify(type));
        if ($type instanceof UnresolvedType && (($this->currentInterface instanceof Class_ && in_array($type->typeName, $this->currentInterface->typeArguments)) || ($this->currentMethod instanceof Method && in_array($type->typeName, $this->currentMethod->typeArguments))))
            return new GenericsType($type->typeName);
        
        return $type;
    }
}
