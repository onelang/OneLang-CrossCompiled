<?php

namespace OneLang\One\Transforms\ResolveUnresolvedTypes;

use OneLang\One\AstTransformer\AstTransformer;
use OneLang\One\Ast\AstTypes\UnresolvedType;
use OneLang\One\Ast\AstTypes\ClassType;
use OneLang\One\Ast\AstTypes\InterfaceType;
use OneLang\One\Ast\AstTypes\EnumType;
use OneLang\One\Ast\AstTypes\GenericsType;
use OneLang\One\Ast\Types\Class_;
use OneLang\One\Ast\Types\Interface_;
use OneLang\One\Ast\Types\Enum;
use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\Ast\Expressions\UnresolvedNewExpression;
use OneLang\One\Ast\Expressions\NewExpression;
use OneLang\One\Ast\Interfaces\IType;

class ResolveUnresolvedTypes extends AstTransformer {
    function __construct() {
        parent::__construct("ResolveUnresolvedTypes");
        
    }
    
    protected function visitType($type) {
        parent::visitType($type);
        if ($type instanceof UnresolvedType) {
            if ($this->currentInterface !== null && in_array($type->typeName, $this->currentInterface->typeArguments))
                return new GenericsType($type->typeName);
            
            $symbol = $this->currentFile->availableSymbols->get($type->typeName);
            if ($symbol === null) {
                $this->errorMan->throw("Unresolved type '" . $type->typeName . "' was not found in available symbols");
                return $type;
            }
            
            if ($symbol instanceof Class_)
                return new ClassType($symbol, $type->typeArguments);
            else if ($symbol instanceof Interface_)
                return new InterfaceType($symbol, $type->typeArguments);
            else if ($symbol instanceof Enum)
                return new EnumType($symbol);
            else {
                $this->errorMan->throw("Unknown symbol type: " . $symbol);
                return $type;
            }
        }
        else
            return $type;
    }
    
    protected function visitExpression($expr) {
        if ($expr instanceof UnresolvedNewExpression) {
            $clsType = $this->visitType($expr->cls);
            if ($clsType instanceof ClassType) {
                $newExpr = new NewExpression($clsType, $expr->args);
                $newExpr->parentNode = $expr->parentNode;
                parent::visitExpression($newExpr);
                return $newExpr;
            }
            else {
                $this->errorMan->throw("Excepted ClassType, but got " . $clsType);
                return $expr;
            }
        }
        else
            return parent::visitExpression($expr);
    }
}
