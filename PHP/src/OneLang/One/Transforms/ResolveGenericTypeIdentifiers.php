<?php

namespace OneLang\One\Transforms\ResolveGenericTypeIdentifiers;

use OneLang\One\AstTransformer\AstTransformer;
use OneLang\One\Ast\AstTypes\UnresolvedType;
use OneLang\One\Ast\AstTypes\GenericsType;
use OneLang\One\Ast\Types\Class_;
use OneLang\One\Ast\Types\Method;
use OneLang\One\Ast\Interfaces\IType;

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
