<?php

namespace One\Transforms\ResolveUnresolvedTypes;

use One\AstTransformer\AstTransformer;
use One\Ast\AstTypes\UnresolvedType;
use One\Ast\AstTypes\ClassType;
use One\Ast\AstTypes\InterfaceType;
use One\Ast\AstTypes\EnumType;
use One\Ast\AstTypes\GenericsType;
use One\Ast\Types\Class_;
use One\Ast\Types\Interface_;
use One\Ast\Types\Enum;
use One\Ast\Expressions\Expression;
use One\Ast\Expressions\UnresolvedNewExpression;
use One\Ast\Expressions\NewExpression;
use One\Ast\Interfaces\IType;

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
