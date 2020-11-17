<?php

namespace OneLang\One\Ast\References;

use OneLang\One\Ast\Types\Class_;
use OneLang\One\Ast\Types\Enum;
use OneLang\One\Ast\Types\MethodParameter;
use OneLang\One\Ast\Types\GlobalFunction;
use OneLang\One\Ast\Types\Field;
use OneLang\One\Ast\Types\Property;
use OneLang\One\Ast\Types\Method;
use OneLang\One\Ast\Types\EnumMember;
use OneLang\One\Ast\Types\IMethodBase;
use OneLang\One\Ast\Types\Lambda;
use OneLang\One\Ast\Types\Constructor;
use OneLang\One\Ast\Types\IVariable;
use OneLang\One\Ast\Statements\VariableDeclaration;
use OneLang\One\Ast\Statements\ForVariable;
use OneLang\One\Ast\Statements\ForeachVariable;
use OneLang\One\Ast\Statements\CatchVariable;
use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\Ast\Expressions\TypeRestriction;
use OneLang\One\Ast\AstTypes\EnumType;
use OneLang\One\Ast\AstTypes\ClassType;
use OneLang\One\Ast\AstTypes\TypeHelper;
use OneLang\One\Ast\Interfaces\IExpression;
use OneLang\One\Ast\Interfaces\IType;

interface IReferencable {
    function createReference();
}

interface IGetMethodBase {
    function getMethodBase();
}

class Reference extends Expression {
    
}

class VariableReference extends Reference {
    function getVariable() {
        throw new \OneCore\Error("Abstract method");
    }
}

class ClassReference extends Reference {
    public $decl;
    
    function __construct($decl) {
        parent::__construct();
        $this->decl = $decl;
        $decl->classReferences[] = $this;
    }
    
    function setActualType($type, $allowVoid = false, $allowGeneric = false) {
        throw new \OneCore\Error("ClassReference cannot have a type!");
    }
}

class GlobalFunctionReference extends Reference implements IGetMethodBase {
    public $decl;
    
    function __construct($decl) {
        parent::__construct();
        $this->decl = $decl;
        $decl->references[] = $this;
    }
    
    function setActualType($type, $allowVoid = false, $allowGeneric = false) {
        throw new \OneCore\Error("GlobalFunctionReference cannot have a type!");
    }
    
    function getMethodBase() {
        return $this->decl;
    }
}

class MethodParameterReference extends VariableReference {
    public $decl;
    
    function __construct($decl) {
        parent::__construct();
        $this->decl = $decl;
        $decl->references[] = $this;
    }
    
    function setActualType($type, $allowVoid = false, $allowGeneric = false) {
        parent::setActualType($type, false, $this->decl->parentMethod instanceof Lambda ? \OneCore\ArrayHelper::some($this->decl->parentMethod->parameters, function ($x) { return TypeHelper::isGeneric($x->type); }) : ($this->decl->parentMethod instanceof Constructor ? count($this->decl->parentMethod->parentClass->typeArguments) > 0 : ($this->decl->parentMethod instanceof Method ? count($this->decl->parentMethod->typeArguments) > 0 || count($this->decl->parentMethod->parentInterface->typeArguments) > 0 : false)));
    }
    
    function getVariable() {
        return $this->decl;
    }
}

class EnumReference extends Reference {
    public $decl;
    
    function __construct($decl) {
        parent::__construct();
        $this->decl = $decl;
        $decl->references[] = $this;
    }
    
    function setActualType($type, $allowVoid = false, $allowGeneric = false) {
        throw new \OneCore\Error("EnumReference cannot have a type!");
    }
}

class EnumMemberReference extends Reference {
    public $decl;
    
    function __construct($decl) {
        parent::__construct();
        $this->decl = $decl;
        $decl->references[] = $this;
    }
    
    function setActualType($type, $allowVoid = false, $allowGeneric = false) {
        if (!($type instanceof EnumType))
            throw new \OneCore\Error("Expected EnumType!");
        parent::setActualType($type);
    }
}

class StaticThisReference extends Reference {
    public $cls;
    
    function __construct($cls) {
        parent::__construct();
        $this->cls = $cls;
        $cls->staticThisReferences[] = $this;
    }
    
    function setActualType($type, $allowVoid = false, $allowGeneric = false) {
        throw new \OneCore\Error("StaticThisReference cannot have a type!");
    }
}

class ThisReference extends Reference {
    public $cls;
    
    function __construct($cls) {
        parent::__construct();
        $this->cls = $cls;
        $cls->thisReferences[] = $this;
    }
    
    function setActualType($type, $allowVoid = false, $allowGeneric = false) {
        if (!($type instanceof ClassType))
            throw new \OneCore\Error("Expected ClassType!");
        parent::setActualType($type, false, count($this->cls->typeArguments) > 0);
    }
}

class SuperReference extends Reference {
    public $cls;
    
    function __construct($cls) {
        parent::__construct();
        $this->cls = $cls;
        $cls->superReferences[] = $this;
    }
    
    function setActualType($type, $allowVoid = false, $allowGeneric = false) {
        if (!($type instanceof ClassType))
            throw new \OneCore\Error("Expected ClassType!");
        parent::setActualType($type, false, count($this->cls->typeArguments) > 0);
    }
}

class VariableDeclarationReference extends VariableReference {
    public $decl;
    
    function __construct($decl) {
        parent::__construct();
        $this->decl = $decl;
        $decl->references[] = $this;
    }
    
    function getVariable() {
        return $this->decl;
    }
}

class ForVariableReference extends VariableReference {
    public $decl;
    
    function __construct($decl) {
        parent::__construct();
        $this->decl = $decl;
        $decl->references[] = $this;
    }
    
    function getVariable() {
        return $this->decl;
    }
}

class CatchVariableReference extends VariableReference {
    public $decl;
    
    function __construct($decl) {
        parent::__construct();
        $this->decl = $decl;
        $decl->references[] = $this;
    }
    
    function getVariable() {
        return $this->decl;
    }
}

class ForeachVariableReference extends VariableReference {
    public $decl;
    
    function __construct($decl) {
        parent::__construct();
        $this->decl = $decl;
        $decl->references[] = $this;
    }
    
    function getVariable() {
        return $this->decl;
    }
}

class StaticFieldReference extends VariableReference {
    public $decl;
    
    function __construct($decl) {
        parent::__construct();
        $this->decl = $decl;
        $decl->staticReferences[] = $this;
    }
    
    function setActualType($type, $allowVoid = false, $allowGeneric = false) {
        if (TypeHelper::isGeneric($type))
            throw new \OneCore\Error("StaticField's type cannot be Generic");
        parent::setActualType($type);
    }
    
    function getVariable() {
        return $this->decl;
    }
}

class StaticPropertyReference extends VariableReference {
    public $decl;
    
    function __construct($decl) {
        parent::__construct();
        $this->decl = $decl;
        $decl->staticReferences[] = $this;
    }
    
    function setActualType($type, $allowVoid = false, $allowGeneric = false) {
        if (TypeHelper::isGeneric($type))
            throw new \OneCore\Error("StaticProperty's type cannot be Generic");
        parent::setActualType($type);
    }
    
    function getVariable() {
        return $this->decl;
    }
}

class InstanceFieldReference extends VariableReference {
    public $object;
    public $field;
    
    function __construct($object, $field) {
        parent::__construct();
        $this->object = $object;
        $this->field = $field;
        $field->instanceReferences[] = $this;
    }
    
    function getVariable() {
        return $this->field;
    }
}

class InstancePropertyReference extends VariableReference {
    public $object;
    public $property;
    
    function __construct($object, $property) {
        parent::__construct();
        $this->object = $object;
        $this->property = $property;
        $property->instanceReferences[] = $this;
    }
    
    function getVariable() {
        return $this->property;
    }
}
