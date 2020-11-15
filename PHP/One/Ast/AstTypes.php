<?php

namespace One\Ast\AstTypes;

use One\Ast\Types\Enum;
use One\Ast\Types\Interface_;
use One\Ast\Types\Class_;
use One\Ast\Types\MethodParameter;
use One\Ast\Types\IInterface;
use One\Ast\Interfaces\IType;

interface IPrimitiveType extends IType {
    
}

interface IHasTypeArguments {
    
}

interface IInterfaceType extends IType {
    function getDecl();
}

class TypeHelper {
    static function argsRepr($args) {
        return count($args) === 0 ? "" : "<" . implode(", ", array_map(function ($x) { return $x->repr(); }, $args)) . ">";
    }
    
    static function isGeneric($type) {
        if ($type instanceof GenericsType)
            return true;
        else if ($type instanceof ClassType)
            return \OneLang\ArrayHelper::some($type->typeArguments, function ($x) { return TypeHelper::isGeneric($x); });
        else if ($type instanceof InterfaceType)
            return \OneLang\ArrayHelper::some($type->typeArguments, function ($x) { return TypeHelper::isGeneric($x); });
        else if ($type instanceof LambdaType)
            return \OneLang\ArrayHelper::some($type->parameters, function ($x) { return TypeHelper::isGeneric($x->type); }) || TypeHelper::isGeneric($type->returnType);
        else
            return false;
    }
    
    static function equals($type1, $type2) {
        if ($type1 === null || $type2 === null)
            throw new \OneLang\Error("Type is missing!");
        if ($type1 instanceof VoidType && $type2 instanceof VoidType)
            return true;
        if ($type1 instanceof AnyType && $type2 instanceof AnyType)
            return true;
        if ($type1 instanceof GenericsType && $type2 instanceof GenericsType)
            return $type1->typeVarName === $type2->typeVarName;
        if ($type1 instanceof EnumType && $type2 instanceof EnumType)
            return $type1->decl === $type2->decl;
        if ($type1 instanceof LambdaType && $type2 instanceof LambdaType)
            return TypeHelper::equals($type1->returnType, $type2->returnType) && count($type1->parameters) === count($type2->parameters) && \OneLang\ArrayHelper::every($type1->parameters, function ($t, $i) use ($type2) { return TypeHelper::equals($t->type, $type2->parameters[$i]->type); });
        if ($type1 instanceof ClassType && $type2 instanceof ClassType)
            return $type1->decl === $type2->decl && count($type1->typeArguments) === count($type2->typeArguments) && \OneLang\ArrayHelper::every($type1->typeArguments, function ($t, $i) use ($type2) { return TypeHelper::equals($t, $type2->typeArguments[$i]); });
        if ($type1 instanceof InterfaceType && $type2 instanceof InterfaceType)
            return $type1->decl === $type2->decl && count($type1->typeArguments) === count($type2->typeArguments) && \OneLang\ArrayHelper::every($type1->typeArguments, function ($t, $i) use ($type2) { return TypeHelper::equals($t, $type2->typeArguments[$i]); });
        return false;
    }
    
    static function isAssignableTo($toBeAssigned, $whereTo) {
        // AnyType can assigned to any type except to void
        if ($toBeAssigned instanceof AnyType && !($whereTo instanceof VoidType))
            return true;
        // any type can assigned to AnyType except void
        if ($whereTo instanceof AnyType && !($toBeAssigned instanceof VoidType))
            return true;
        // any type can assigned to GenericsType except void
        if ($whereTo instanceof GenericsType && !($toBeAssigned instanceof VoidType))
            return true;
        // null can be assigned anywhere
        // TODO: filter out number and boolean types...
        if ($toBeAssigned instanceof NullType && !($whereTo instanceof VoidType))
            return true;
        
        if (TypeHelper::equals($toBeAssigned, $whereTo))
            return true;
        
        if ($toBeAssigned instanceof ClassType && $whereTo instanceof ClassType)
            return ($toBeAssigned->decl->baseClass !== null && TypeHelper::isAssignableTo($toBeAssigned->decl->baseClass, $whereTo)) || $toBeAssigned->decl === $whereTo->decl && \OneLang\ArrayHelper::every($toBeAssigned->typeArguments, function ($x, $i) use ($whereTo) { return TypeHelper::isAssignableTo($x, $whereTo->typeArguments[$i]); });
        if ($toBeAssigned instanceof ClassType && $whereTo instanceof InterfaceType)
            return ($toBeAssigned->decl->baseClass !== null && TypeHelper::isAssignableTo($toBeAssigned->decl->baseClass, $whereTo)) || \OneLang\ArrayHelper::some($toBeAssigned->decl->baseInterfaces, function ($x) use ($whereTo) { return TypeHelper::isAssignableTo($x, $whereTo); });
        if ($toBeAssigned instanceof InterfaceType && $whereTo instanceof InterfaceType)
            return \OneLang\ArrayHelper::some($toBeAssigned->decl->baseInterfaces, function ($x) use ($whereTo) { return TypeHelper::isAssignableTo($x, $whereTo); }) || $toBeAssigned->decl === $whereTo->decl && \OneLang\ArrayHelper::every($toBeAssigned->typeArguments, function ($x, $i) use ($whereTo) { return TypeHelper::isAssignableTo($x, $whereTo->typeArguments[$i]); });
        if ($toBeAssigned instanceof LambdaType && $whereTo instanceof LambdaType)
            return count($toBeAssigned->parameters) === count($whereTo->parameters) && \OneLang\ArrayHelper::every($toBeAssigned->parameters, function ($p, $i) use ($whereTo) { return TypeHelper::isAssignableTo($p->type, $whereTo->parameters[$i]->type); }) && (TypeHelper::isAssignableTo($toBeAssigned->returnType, $whereTo->returnType) || $whereTo->returnType instanceof GenericsType);
        
        return false;
    }
}

class VoidType implements IPrimitiveType {
    public static $instance;
    
    static function StaticInit()
    {
        VoidType::$instance = new VoidType();
    }
    
    function repr() {
        return "Void";
    }
}
VoidType::StaticInit();

class AnyType implements IPrimitiveType {
    public static $instance;
    
    static function StaticInit()
    {
        AnyType::$instance = new AnyType();
    }
    
    function repr() {
        return "Any";
    }
}
AnyType::StaticInit();

class NullType implements IPrimitiveType {
    public static $instance;
    
    static function StaticInit()
    {
        NullType::$instance = new NullType();
    }
    
    function repr() {
        return "Null";
    }
}
NullType::StaticInit();

class GenericsType implements IType {
    public $typeVarName;
    
    function __construct($typeVarName) {
        $this->typeVarName = $typeVarName;
    }
    
    function repr() {
        return "G:" . $this->typeVarName;
    }
}

class EnumType implements IType {
    public $decl;
    
    function __construct($decl) {
        $this->decl = $decl;
    }
    
    function repr() {
        return "E:" . $this->decl->name;
    }
}

class InterfaceType implements IType, IHasTypeArguments, IInterfaceType {
    public $decl;
    public $typeArguments;
    
    function __construct($decl, $typeArguments) {
        $this->decl = $decl;
        $this->typeArguments = $typeArguments;
    }
    
    function getDecl() {
        return $this->decl;
    }
    
    function repr() {
        return "I:" . $this->decl->name . TypeHelper::argsRepr($this->typeArguments);
    }
}

class ClassType implements IType, IHasTypeArguments, IInterfaceType {
    public $decl;
    public $typeArguments;
    
    function __construct($decl, $typeArguments) {
        $this->decl = $decl;
        $this->typeArguments = $typeArguments;
    }
    
    function getDecl() {
        return $this->decl;
    }
    
    function repr() {
        return "C:" . $this->decl->name . TypeHelper::argsRepr($this->typeArguments);
    }
}

class UnresolvedType implements IType, IHasTypeArguments {
    public $typeName;
    public $typeArguments;
    
    function __construct($typeName, $typeArguments) {
        $this->typeName = $typeName;
        $this->typeArguments = $typeArguments;
    }
    
    function repr() {
        return "X:" . $this->typeName . TypeHelper::argsRepr($this->typeArguments);
    }
}

class LambdaType implements IType {
    public $parameters;
    public $returnType;
    
    function __construct($parameters, $returnType) {
        $this->parameters = $parameters;
        $this->returnType = $returnType;
    }
    
    function repr() {
        return "L:(" . implode(", ", array_map(function ($x) { return $x->type->repr(); }, $this->parameters)) . ")=>" . $this->returnType->repr();
    }
}
