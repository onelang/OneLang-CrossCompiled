<?php

namespace OneLang\One\AstTransformer;

use OneLang\One\Ast\AstTypes\IHasTypeArguments;
use OneLang\One\Ast\AstTypes\ClassType;
use OneLang\One\Ast\AstTypes\InterfaceType;
use OneLang\One\Ast\AstTypes\UnresolvedType;
use OneLang\One\Ast\AstTypes\LambdaType;
use OneLang\One\Ast\Expressions\Identifier;
use OneLang\One\Ast\Expressions\BinaryExpression;
use OneLang\One\Ast\Expressions\ConditionalExpression;
use OneLang\One\Ast\Expressions\NewExpression;
use OneLang\One\Ast\Expressions\TemplateString;
use OneLang\One\Ast\Expressions\ParenthesizedExpression;
use OneLang\One\Ast\Expressions\UnaryExpression;
use OneLang\One\Ast\Expressions\PropertyAccessExpression;
use OneLang\One\Ast\Expressions\ElementAccessExpression;
use OneLang\One\Ast\Expressions\ArrayLiteral;
use OneLang\One\Ast\Expressions\MapLiteral;
use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\Ast\Expressions\CastExpression;
use OneLang\One\Ast\Expressions\UnresolvedCallExpression;
use OneLang\One\Ast\Expressions\InstanceOfExpression;
use OneLang\One\Ast\Expressions\AwaitExpression;
use OneLang\One\Ast\Expressions\StringLiteral;
use OneLang\One\Ast\Expressions\NumericLiteral;
use OneLang\One\Ast\Expressions\NullLiteral;
use OneLang\One\Ast\Expressions\RegexLiteral;
use OneLang\One\Ast\Expressions\BooleanLiteral;
use OneLang\One\Ast\Expressions\StaticMethodCallExpression;
use OneLang\One\Ast\Expressions\InstanceMethodCallExpression;
use OneLang\One\Ast\Expressions\UnresolvedNewExpression;
use OneLang\One\Ast\Expressions\NullCoalesceExpression;
use OneLang\One\Ast\Expressions\UnresolvedMethodCallExpression;
use OneLang\One\Ast\Expressions\GlobalFunctionCallExpression;
use OneLang\One\Ast\Expressions\LambdaCallExpression;
use OneLang\One\Ast\Statements\ReturnStatement;
use OneLang\One\Ast\Statements\ExpressionStatement;
use OneLang\One\Ast\Statements\IfStatement;
use OneLang\One\Ast\Statements\ThrowStatement;
use OneLang\One\Ast\Statements\VariableDeclaration;
use OneLang\One\Ast\Statements\WhileStatement;
use OneLang\One\Ast\Statements\ForStatement;
use OneLang\One\Ast\Statements\ForeachStatement;
use OneLang\One\Ast\Statements\Statement;
use OneLang\One\Ast\Statements\UnsetStatement;
use OneLang\One\Ast\Statements\BreakStatement;
use OneLang\One\Ast\Statements\ContinueStatement;
use OneLang\One\Ast\Statements\DoStatement;
use OneLang\One\Ast\Statements\TryStatement;
use OneLang\One\Ast\Statements\Block;
use OneLang\One\Ast\Types\Method;
use OneLang\One\Ast\Types\Constructor;
use OneLang\One\Ast\Types\Field;
use OneLang\One\Ast\Types\Property;
use OneLang\One\Ast\Types\Interface_;
use OneLang\One\Ast\Types\Class_;
use OneLang\One\Ast\Types\Enum;
use OneLang\One\Ast\Types\EnumMember;
use OneLang\One\Ast\Types\SourceFile;
use OneLang\One\Ast\Types\IVariable;
use OneLang\One\Ast\Types\IVariableWithInitializer;
use OneLang\One\Ast\Types\MethodParameter;
use OneLang\One\Ast\Types\Lambda;
use OneLang\One\Ast\Types\IMethodBase;
use OneLang\One\Ast\Types\Package;
use OneLang\One\Ast\Types\GlobalFunction;
use OneLang\One\Ast\Types\IInterface;
use OneLang\One\Ast\Types\IAstNode;
use OneLang\One\Ast\Types\IHasAttributesAndTrivia;
use OneLang\One\Ast\Types\Import;
use OneLang\One\Ast\References\ClassReference;
use OneLang\One\Ast\References\EnumReference;
use OneLang\One\Ast\References\ThisReference;
use OneLang\One\Ast\References\MethodParameterReference;
use OneLang\One\Ast\References\VariableDeclarationReference;
use OneLang\One\Ast\References\ForVariableReference;
use OneLang\One\Ast\References\ForeachVariableReference;
use OneLang\One\Ast\References\SuperReference;
use OneLang\One\Ast\References\InstanceFieldReference;
use OneLang\One\Ast\References\InstancePropertyReference;
use OneLang\One\Ast\References\StaticPropertyReference;
use OneLang\One\Ast\References\StaticFieldReference;
use OneLang\One\Ast\References\CatchVariableReference;
use OneLang\One\Ast\References\GlobalFunctionReference;
use OneLang\One\Ast\References\EnumMemberReference;
use OneLang\One\Ast\References\StaticThisReference;
use OneLang\One\Ast\References\VariableReference;
use OneLang\One\ErrorManager\ErrorManager;
use OneLang\One\ITransformer\ITransformer;
use OneLang\One\Ast\Interfaces\IType;

class AstTransformer implements ITransformer {
    public $errorMan;
    public $currentFile;
    public $currentInterface;
    public $currentMethod;
    public $currentClosure;
    public $currentStatement;
    public $name;
    
    function __construct($name) {
        $this->name = $name;
        $this->errorMan = new ErrorManager();
        $this->currentFile = null;
        $this->currentInterface = null;
        $this->currentMethod = null;
        $this->currentClosure = null;
        $this->currentStatement = null;
    }
    
    protected function visitAttributesAndTrivia($node) {
        
    }
    
    protected function visitType($type) {
        if ($type instanceof ClassType || $type instanceof InterfaceType || $type instanceof UnresolvedType) {
            $type2 = $type;
            $type2->typeArguments = array_map(function ($x) { return $this->visitType($x); }, $type2->typeArguments);
        }
        else if ($type instanceof LambdaType) {
            foreach ($type->parameters as $mp)
                $this->visitMethodParameter($mp);
            $type->returnType = $this->visitType($type->returnType);
        }
        return $type;
    }
    
    protected function visitIdentifier($id) {
        return $id;
    }
    
    protected function visitVariable($variable) {
        if ($variable->type !== null)
            $variable->type = $this->visitType($variable->type);
        return $variable;
    }
    
    protected function visitVariableWithInitializer($variable) {
        $this->visitVariable($variable);
        if ($variable->initializer !== null)
            $variable->initializer = $this->visitExpression($variable->initializer);
        return $variable;
    }
    
    protected function visitVariableDeclaration($stmt) {
        $this->visitVariableWithInitializer($stmt);
        return $stmt;
    }
    
    protected function visitUnknownStatement($stmt) {
        $this->errorMan->throw("Unknown statement type");
        return $stmt;
    }
    
    protected function visitStatement($stmt) {
        $this->currentStatement = $stmt;
        $this->visitAttributesAndTrivia($stmt);
        if ($stmt instanceof ReturnStatement) {
            if ($stmt->expression !== null)
                $stmt->expression = $this->visitExpression($stmt->expression);
        }
        else if ($stmt instanceof ExpressionStatement)
            $stmt->expression = $this->visitExpression($stmt->expression);
        else if ($stmt instanceof IfStatement) {
            $stmt->condition = $this->visitExpression($stmt->condition);
            $stmt->then = $this->visitBlock($stmt->then);
            if ($stmt->else_ !== null)
                $stmt->else_ = $this->visitBlock($stmt->else_);
        }
        else if ($stmt instanceof ThrowStatement)
            $stmt->expression = $this->visitExpression($stmt->expression);
        else if ($stmt instanceof VariableDeclaration)
            return $this->visitVariableDeclaration($stmt);
        else if ($stmt instanceof WhileStatement) {
            $stmt->condition = $this->visitExpression($stmt->condition);
            $stmt->body = $this->visitBlock($stmt->body);
        }
        else if ($stmt instanceof DoStatement) {
            $stmt->condition = $this->visitExpression($stmt->condition);
            $stmt->body = $this->visitBlock($stmt->body);
        }
        else if ($stmt instanceof ForStatement) {
            if ($stmt->itemVar !== null)
                $this->visitVariableWithInitializer($stmt->itemVar);
            $stmt->condition = $this->visitExpression($stmt->condition);
            $stmt->incrementor = $this->visitExpression($stmt->incrementor);
            $stmt->body = $this->visitBlock($stmt->body);
        }
        else if ($stmt instanceof ForeachStatement) {
            $this->visitVariable($stmt->itemVar);
            $stmt->items = $this->visitExpression($stmt->items);
            $stmt->body = $this->visitBlock($stmt->body);
        }
        else if ($stmt instanceof TryStatement) {
            $stmt->tryBody = $this->visitBlock($stmt->tryBody);
            if ($stmt->catchBody !== null) {
                $this->visitVariable($stmt->catchVar);
                $stmt->catchBody = $this->visitBlock($stmt->catchBody);
            }
            if ($stmt->finallyBody !== null)
                $stmt->finallyBody = $this->visitBlock($stmt->finallyBody);
        }
        else if ($stmt instanceof BreakStatement) { }
        else if ($stmt instanceof UnsetStatement)
            $stmt->expression = $this->visitExpression($stmt->expression);
        else if ($stmt instanceof ContinueStatement) { }
        else
            return $this->visitUnknownStatement($stmt);
        return $stmt;
    }
    
    protected function visitBlock($block) {
        $block->statements = array_map(function ($x) { return $this->visitStatement($x); }, $block->statements);
        return $block;
    }
    
    protected function visitTemplateString($expr) {
        for ($i = 0; $i < count($expr->parts); $i++) {
            $part = $expr->parts[$i];
            if (!$part->isLiteral)
                $part->expression = $this->visitExpression($part->expression);
        }
        return $expr;
    }
    
    protected function visitUnknownExpression($expr) {
        $this->errorMan->throw("Unknown expression type");
        return $expr;
    }
    
    protected function visitLambda($lambda) {
        $prevClosure = $this->currentClosure;
        $this->currentClosure = $lambda;
        $this->visitMethodBase($lambda);
        $this->currentClosure = $prevClosure;
        return $lambda;
    }
    
    protected function visitVariableReference($varRef) {
        return $varRef;
    }
    
    protected function visitExpression($expr) {
        if ($expr instanceof BinaryExpression) {
            $expr->left = $this->visitExpression($expr->left);
            $expr->right = $this->visitExpression($expr->right);
        }
        else if ($expr instanceof NullCoalesceExpression) {
            $expr->defaultExpr = $this->visitExpression($expr->defaultExpr);
            $expr->exprIfNull = $this->visitExpression($expr->exprIfNull);
        }
        else if ($expr instanceof UnresolvedCallExpression) {
            $expr->func = $this->visitExpression($expr->func);
            $expr->typeArgs = array_map(function ($x) { return $this->visitType($x); }, $expr->typeArgs);
            $expr->args = array_map(function ($x) { return $this->visitExpression($x); }, $expr->args);
        }
        else if ($expr instanceof UnresolvedMethodCallExpression) {
            $expr->object = $this->visitExpression($expr->object);
            $expr->typeArgs = array_map(function ($x) { return $this->visitType($x); }, $expr->typeArgs);
            $expr->args = array_map(function ($x) { return $this->visitExpression($x); }, $expr->args);
        }
        else if ($expr instanceof ConditionalExpression) {
            $expr->condition = $this->visitExpression($expr->condition);
            $expr->whenTrue = $this->visitExpression($expr->whenTrue);
            $expr->whenFalse = $this->visitExpression($expr->whenFalse);
        }
        else if ($expr instanceof Identifier)
            return $this->visitIdentifier($expr);
        else if ($expr instanceof UnresolvedNewExpression) {
            $this->visitType($expr->cls);
            $expr->args = array_map(function ($x) { return $this->visitExpression($x); }, $expr->args);
        }
        else if ($expr instanceof NewExpression) {
            $this->visitType($expr->cls);
            $expr->args = array_map(function ($x) { return $this->visitExpression($x); }, $expr->args);
        }
        else if ($expr instanceof TemplateString)
            return $this->visitTemplateString($expr);
        else if ($expr instanceof ParenthesizedExpression)
            $expr->expression = $this->visitExpression($expr->expression);
        else if ($expr instanceof UnaryExpression)
            $expr->operand = $this->visitExpression($expr->operand);
        else if ($expr instanceof PropertyAccessExpression)
            $expr->object = $this->visitExpression($expr->object);
        else if ($expr instanceof ElementAccessExpression) {
            $expr->object = $this->visitExpression($expr->object);
            $expr->elementExpr = $this->visitExpression($expr->elementExpr);
        }
        else if ($expr instanceof ArrayLiteral)
            $expr->items = array_map(function ($x) { return $this->visitExpression($x); }, $expr->items);
        else if ($expr instanceof MapLiteral)
            foreach ($expr->items as $item)
                $item->value = $this->visitExpression($item->value);
        else if ($expr instanceof StringLiteral) { }
        else if ($expr instanceof BooleanLiteral) { }
        else if ($expr instanceof NumericLiteral) { }
        else if ($expr instanceof NullLiteral) { }
        else if ($expr instanceof RegexLiteral) { }
        else if ($expr instanceof CastExpression) {
            $expr->newType = $this->visitType($expr->newType);
            $expr->expression = $this->visitExpression($expr->expression);
        }
        else if ($expr instanceof InstanceOfExpression) {
            $expr->expr = $this->visitExpression($expr->expr);
            $expr->checkType = $this->visitType($expr->checkType);
        }
        else if ($expr instanceof AwaitExpression)
            $expr->expr = $this->visitExpression($expr->expr);
        else if ($expr instanceof Lambda)
            return $this->visitLambda($expr);
        else if ($expr instanceof ClassReference) { }
        else if ($expr instanceof EnumReference) { }
        else if ($expr instanceof ThisReference) { }
        else if ($expr instanceof StaticThisReference) { }
        else if ($expr instanceof MethodParameterReference)
            return $this->visitVariableReference($expr);
        else if ($expr instanceof VariableDeclarationReference)
            return $this->visitVariableReference($expr);
        else if ($expr instanceof ForVariableReference)
            return $this->visitVariableReference($expr);
        else if ($expr instanceof ForeachVariableReference)
            return $this->visitVariableReference($expr);
        else if ($expr instanceof CatchVariableReference)
            return $this->visitVariableReference($expr);
        else if ($expr instanceof GlobalFunctionReference) { }
        else if ($expr instanceof SuperReference) { }
        else if ($expr instanceof InstanceFieldReference) {
            $expr->object = $this->visitExpression($expr->object);
            return $this->visitVariableReference($expr);
        }
        else if ($expr instanceof InstancePropertyReference) {
            $expr->object = $this->visitExpression($expr->object);
            return $this->visitVariableReference($expr);
        }
        else if ($expr instanceof StaticFieldReference)
            return $this->visitVariableReference($expr);
        else if ($expr instanceof StaticPropertyReference)
            return $this->visitVariableReference($expr);
        else if ($expr instanceof EnumMemberReference) { }
        else if ($expr instanceof StaticMethodCallExpression) {
            $expr->typeArgs = array_map(function ($x) { return $this->visitType($x); }, $expr->typeArgs);
            $expr->args = array_map(function ($x) { return $this->visitExpression($x); }, $expr->args);
        }
        else if ($expr instanceof GlobalFunctionCallExpression)
            $expr->args = array_map(function ($x) { return $this->visitExpression($x); }, $expr->args);
        else if ($expr instanceof InstanceMethodCallExpression) {
            $expr->object = $this->visitExpression($expr->object);
            $expr->typeArgs = array_map(function ($x) { return $this->visitType($x); }, $expr->typeArgs);
            $expr->args = array_map(function ($x) { return $this->visitExpression($x); }, $expr->args);
        }
        else if ($expr instanceof LambdaCallExpression)
            $expr->args = array_map(function ($x) { return $this->visitExpression($x); }, $expr->args);
        else
            return $this->visitUnknownExpression($expr);
        return $expr;
    }
    
    protected function visitMethodParameter($methodParameter) {
        $this->visitAttributesAndTrivia($methodParameter);
        $this->visitVariableWithInitializer($methodParameter);
    }
    
    protected function visitMethodBase($method) {
        foreach ($method->parameters as $item)
            $this->visitMethodParameter($item);
        
        if ($method->body !== null)
            $method->body = $this->visitBlock($method->body);
    }
    
    protected function visitMethod($method) {
        $this->currentMethod = $method;
        $this->currentClosure = $method;
        $this->visitAttributesAndTrivia($method);
        $this->visitMethodBase($method);
        $method->returns = $this->visitType($method->returns);
        $this->currentClosure = null;
        $this->currentMethod = null;
    }
    
    protected function visitGlobalFunction($func) {
        $this->visitMethodBase($func);
        $func->returns = $this->visitType($func->returns);
    }
    
    protected function visitConstructor($constructor) {
        $this->currentMethod = $constructor;
        $this->currentClosure = $constructor;
        $this->visitAttributesAndTrivia($constructor);
        $this->visitMethodBase($constructor);
        $this->currentClosure = null;
        $this->currentMethod = null;
    }
    
    protected function visitField($field) {
        $this->visitAttributesAndTrivia($field);
        $this->visitVariableWithInitializer($field);
    }
    
    protected function visitProperty($prop) {
        $this->visitAttributesAndTrivia($prop);
        $this->visitVariable($prop);
        if ($prop->getter !== null)
            $prop->getter = $this->visitBlock($prop->getter);
        if ($prop->setter !== null)
            $prop->setter = $this->visitBlock($prop->setter);
    }
    
    protected function visitInterface($intf) {
        $this->currentInterface = $intf;
        $this->visitAttributesAndTrivia($intf);
        $intf->baseInterfaces = array_map(function ($x) { return $this->visitType($x); }, $intf->baseInterfaces);
        foreach ($intf->fields as $field)
            $this->visitField($field);
        foreach ($intf->methods as $method)
            $this->visitMethod($method);
        $this->currentInterface = null;
    }
    
    protected function visitClass($cls) {
        $this->currentInterface = $cls;
        $this->visitAttributesAndTrivia($cls);
        if ($cls->constructor_ !== null)
            $this->visitConstructor($cls->constructor_);
        
        $cls->baseClass = $this->visitType($cls->baseClass);
        $cls->baseInterfaces = array_map(function ($x) { return $this->visitType($x); }, $cls->baseInterfaces);
        foreach ($cls->fields as $field)
            $this->visitField($field);
        foreach ($cls->properties as $prop)
            $this->visitProperty($prop);
        foreach ($cls->methods as $method)
            $this->visitMethod($method);
        $this->currentInterface = null;
    }
    
    protected function visitEnum($enum_) {
        $this->visitAttributesAndTrivia($enum_);
        foreach ($enum_->values as $value)
            $this->visitEnumMember($value);
    }
    
    protected function visitEnumMember($enumMember) {
        
    }
    
    protected function visitImport($imp) {
        $this->visitAttributesAndTrivia($imp);
    }
    
    function visitFile($sourceFile) {
        $this->errorMan->resetContext($this);
        $this->currentFile = $sourceFile;
        foreach ($sourceFile->imports as $imp)
            $this->visitImport($imp);
        foreach ($sourceFile->enums as $enum_)
            $this->visitEnum($enum_);
        foreach ($sourceFile->interfaces as $intf)
            $this->visitInterface($intf);
        foreach ($sourceFile->classes as $cls)
            $this->visitClass($cls);
        foreach ($sourceFile->funcs as $func)
            $this->visitGlobalFunction($func);
        $sourceFile->mainBlock = $this->visitBlock($sourceFile->mainBlock);
        $this->currentFile = null;
    }
    
    function visitFiles($files) {
        foreach ($files as $file)
            $this->visitFile($file);
    }
    
    function visitPackage($pkg) {
        $this->visitFiles(array_values($pkg->files));
    }
}
