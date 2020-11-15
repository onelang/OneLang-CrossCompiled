<?php

namespace One\Ast\Expressions;

use One\Ast\AstTypes\VoidType;
use One\Ast\AstTypes\UnresolvedType;
use One\Ast\AstTypes\ClassType;
use One\Ast\AstTypes\TypeHelper;
use One\Ast\Types\Method;
use One\Ast\Types\GlobalFunction;
use One\Ast\Types\IAstNode;
use One\Ast\Interfaces\IExpression;
use One\Ast\Interfaces\IType;

class TypeRestriction {
    const NORESTRICTION = 1;
    const SHOULDNOTHAVETYPE = 2;
    const MUSTBEGENERIC = 3;
    const SHOULDNOTBEGENERIC = 4;
}
class UnaryType {
    const POSTFIX = 1;
    const PREFIX = 2;
}

interface IMethodCallExpression extends IExpression {
    
}

class Expression implements IAstNode, IExpression {
    public $parentNode;
    public $expectedType;
    public $actualType;
    
    function __construct()
    {
        $this->parentNode = null;
        $this->expectedType = null;
        $this->actualType = null;
    }
    
    protected function typeCheck($type, $allowVoid) {
        if ($type === null)
            throw new \OneLang\Error("New type cannot be null!");
        
        if ($type instanceof VoidType && !$allowVoid)
            throw new \OneLang\Error("Expression's type cannot be VoidType!");
        
        if ($type instanceof UnresolvedType)
            throw new \OneLang\Error("Expression's type cannot be UnresolvedType!");
    }
    
    function setActualType($actualType, $allowVoid = false, $allowGeneric = false) {
        if ($this->actualType !== null)
            throw new \OneLang\Error("Expression already has actual type (current type = " . $this->actualType->repr() . ", new type = " . $actualType->repr() . ")");
        
        $this->typeCheck($actualType, $allowVoid);
        
        if ($this->expectedType !== null && !TypeHelper::isAssignableTo($actualType, $this->expectedType))
            throw new \OneLang\Error("Actual type (" . $actualType->repr() . ") is not assignable to the declared type (" . $this->expectedType->repr() . ")!");
        
        // TODO: decide if this check needed or not
        //if (!allowGeneric && TypeHelper.isGeneric(actualType))
        //    throw new Error(`Actual type cannot be generic (${actualType.repr()})!`);
        
        $this->actualType = $actualType;
    }
    
    function setExpectedType($type, $allowVoid = false) {
        if ($this->actualType !== null)
            throw new \OneLang\Error("Cannot set expected type after actual type was already set!");
        
        if ($this->expectedType !== null)
            throw new \OneLang\Error("Expression already has a expected type!");
        
        $this->typeCheck($type, $allowVoid);
        
        $this->expectedType = $type;
    }
    
    function getType() {
        return $this->actualType ?? $this->expectedType;
    }
}

class Identifier extends Expression {
    public $text;
    
    function __construct($text) {
        parent::__construct();
        $this->text = $text;
    }
}

class NumericLiteral extends Expression {
    public $valueAsText;
    
    function __construct($valueAsText) {
        parent::__construct();
        $this->valueAsText = $valueAsText;
    }
}

class BooleanLiteral extends Expression {
    public $boolValue;
    
    function __construct($boolValue) {
        parent::__construct();
        $this->boolValue = $boolValue;
    }
}

class CharacterLiteral extends Expression {
    public $charValue;
    
    function __construct($charValue) {
        parent::__construct();
        $this->charValue = $charValue;
    }
}

class StringLiteral extends Expression {
    public $stringValue;
    
    function __construct($stringValue) {
        parent::__construct();
        $this->stringValue = $stringValue;
    }
}

class NullLiteral extends Expression {
    
}

class RegexLiteral extends Expression {
    public $pattern;
    public $caseInsensitive;
    public $global;
    
    function __construct($pattern, $caseInsensitive, $global) {
        parent::__construct();
        $this->pattern = $pattern;
        $this->caseInsensitive = $caseInsensitive;
        $this->global = $global;
    }
}

class TemplateStringPart implements IAstNode {
    public $isLiteral;
    public $literalText;
    public $expression;
    
    function __construct($isLiteral, $literalText, $expression) {
        $this->isLiteral = $isLiteral;
        $this->literalText = $literalText;
        $this->expression = $expression;
    }
    
    static function Literal($literalText) {
        return new TemplateStringPart(true, $literalText, null);
    }
    
    static function Expression($expr) {
        return new TemplateStringPart(false, null, $expr);
    }
}

class TemplateString extends Expression {
    public $parts;
    
    function __construct($parts) {
        parent::__construct();
        $this->parts = $parts;
    }
}

class ArrayLiteral extends Expression {
    public $items;
    
    function __construct($items) {
        parent::__construct();
        $this->items = $items;
    }
}

class MapLiteralItem implements IAstNode {
    public $key;
    public $value;
    
    function __construct($key, $value) {
        $this->key = $key;
        $this->value = $value;
    }
}

class MapLiteral extends Expression {
    public $items;
    
    function __construct($items) {
        parent::__construct();
        $this->items = $items;
    }
}

class UnresolvedNewExpression extends Expression {
    public $cls;
    public $args;
    
    function __construct($cls, $args) {
        parent::__construct();
        $this->cls = $cls;
        $this->args = $args;
    }
}

class NewExpression extends Expression {
    public $cls;
    public $args;
    
    function __construct($cls, $args) {
        parent::__construct();
        $this->cls = $cls;
        $this->args = $args;
    }
}

class BinaryExpression extends Expression {
    public $left;
    public $operator;
    public $right;
    
    function __construct($left, $operator, $right) {
        parent::__construct();
        $this->left = $left;
        $this->operator = $operator;
        $this->right = $right;
    }
}

class NullCoalesceExpression extends Expression {
    public $defaultExpr;
    public $exprIfNull;
    
    function __construct($defaultExpr, $exprIfNull) {
        parent::__construct();
        $this->defaultExpr = $defaultExpr;
        $this->exprIfNull = $exprIfNull;
    }
}

class UnaryExpression extends Expression {
    public $unaryType;
    public $operator;
    public $operand;
    
    function __construct($unaryType, $operator, $operand) {
        parent::__construct();
        $this->unaryType = $unaryType;
        $this->operator = $operator;
        $this->operand = $operand;
    }
}

class CastExpression extends Expression {
    public $newType;
    public $expression;
    public $instanceOfCast;
    
    function __construct($newType, $expression) {
        parent::__construct();
        $this->newType = $newType;
        $this->expression = $expression;
        $this->instanceOfCast = null;
    }
}

class ParenthesizedExpression extends Expression {
    public $expression;
    
    function __construct($expression) {
        parent::__construct();
        $this->expression = $expression;
    }
}

class ConditionalExpression extends Expression {
    public $condition;
    public $whenTrue;
    public $whenFalse;
    
    function __construct($condition, $whenTrue, $whenFalse) {
        parent::__construct();
        $this->condition = $condition;
        $this->whenTrue = $whenTrue;
        $this->whenFalse = $whenFalse;
    }
}

class PropertyAccessExpression extends Expression {
    public $object;
    public $propertyName;
    
    function __construct($object, $propertyName) {
        parent::__construct();
        $this->object = $object;
        $this->propertyName = $propertyName;
    }
}

class ElementAccessExpression extends Expression {
    public $object;
    public $elementExpr;
    
    function __construct($object, $elementExpr) {
        parent::__construct();
        $this->object = $object;
        $this->elementExpr = $elementExpr;
    }
}

class UnresolvedCallExpression extends Expression {
    public $func;
    public $typeArgs;
    public $args;
    
    function __construct($func, $typeArgs, $args) {
        parent::__construct();
        $this->func = $func;
        $this->typeArgs = $typeArgs;
        $this->args = $args;
    }
}

class UnresolvedMethodCallExpression extends Expression {
    public $object;
    public $methodName;
    public $typeArgs;
    public $args;
    
    function __construct($object, $methodName, $typeArgs, $args) {
        parent::__construct();
        $this->object = $object;
        $this->methodName = $methodName;
        $this->typeArgs = $typeArgs;
        $this->args = $args;
    }
}

class StaticMethodCallExpression extends Expression implements IMethodCallExpression {
    public $method;
    public $typeArgs;
    public $args;
    public $isThisCall;
    
    function __construct($method, $typeArgs, $args, $isThisCall) {
        parent::__construct();
        $this->method = $method;
        $this->typeArgs = $typeArgs;
        $this->args = $args;
        $this->isThisCall = $isThisCall;
    }
}

class InstanceMethodCallExpression extends Expression implements IMethodCallExpression {
    public $object;
    public $method;
    public $typeArgs;
    public $args;
    
    function __construct($object, $method, $typeArgs, $args) {
        parent::__construct();
        $this->object = $object;
        $this->method = $method;
        $this->typeArgs = $typeArgs;
        $this->args = $args;
    }
}

class GlobalFunctionCallExpression extends Expression {
    public $func;
    public $args;
    
    function __construct($func, $args) {
        parent::__construct();
        $this->func = $func;
        $this->args = $args;
    }
}

class LambdaCallExpression extends Expression {
    public $method;
    public $args;
    
    function __construct($method, $args) {
        parent::__construct();
        $this->method = $method;
        $this->args = $args;
    }
}

class TodoExpression extends Expression {
    public $expr;
    
    function __construct($expr) {
        parent::__construct();
        $this->expr = $expr;
    }
}

class InstanceOfExpression extends Expression {
    public $expr;
    public $checkType;
    public $implicitCasts;
    public $alias;
    
    function __construct($expr, $checkType) {
        parent::__construct();
        $this->expr = $expr;
        $this->checkType = $checkType;
        $this->implicitCasts = null;
        $this->alias = null;
    }
}

class AwaitExpression extends Expression {
    public $expr;
    
    function __construct($expr) {
        parent::__construct();
        $this->expr = $expr;
    }
}
