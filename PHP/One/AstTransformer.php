<?php

namespace One\AstTransformer;

use One\Ast\AstTypes\IHasTypeArguments;
use One\Ast\AstTypes\ClassType;
use One\Ast\AstTypes\InterfaceType;
use One\Ast\AstTypes\UnresolvedType;
use One\Ast\AstTypes\LambdaType;
use One\Ast\Expressions\Identifier;
use One\Ast\Expressions\BinaryExpression;
use One\Ast\Expressions\ConditionalExpression;
use One\Ast\Expressions\NewExpression;
use One\Ast\Expressions\TemplateString;
use One\Ast\Expressions\ParenthesizedExpression;
use One\Ast\Expressions\UnaryExpression;
use One\Ast\Expressions\PropertyAccessExpression;
use One\Ast\Expressions\ElementAccessExpression;
use One\Ast\Expressions\ArrayLiteral;
use One\Ast\Expressions\MapLiteral;
use One\Ast\Expressions\Expression;
use One\Ast\Expressions\CastExpression;
use One\Ast\Expressions\UnresolvedCallExpression;
use One\Ast\Expressions\InstanceOfExpression;
use One\Ast\Expressions\AwaitExpression;
use One\Ast\Expressions\StringLiteral;
use One\Ast\Expressions\NumericLiteral;
use One\Ast\Expressions\NullLiteral;
use One\Ast\Expressions\RegexLiteral;
use One\Ast\Expressions\BooleanLiteral;
use One\Ast\Expressions\StaticMethodCallExpression;
use One\Ast\Expressions\InstanceMethodCallExpression;
use One\Ast\Expressions\UnresolvedNewExpression;
use One\Ast\Expressions\NullCoalesceExpression;
use One\Ast\Expressions\UnresolvedMethodCallExpression;
use One\Ast\Expressions\GlobalFunctionCallExpression;
use One\Ast\Expressions\LambdaCallExpression;
use One\Ast\Statements\ReturnStatement;
use One\Ast\Statements\ExpressionStatement;
use One\Ast\Statements\IfStatement;
use One\Ast\Statements\ThrowStatement;
use One\Ast\Statements\VariableDeclaration;
use One\Ast\Statements\WhileStatement;
use One\Ast\Statements\ForStatement;
use One\Ast\Statements\ForeachStatement;
use One\Ast\Statements\Statement;
use One\Ast\Statements\UnsetStatement;
use One\Ast\Statements\BreakStatement;
use One\Ast\Statements\ContinueStatement;
use One\Ast\Statements\DoStatement;
use One\Ast\Statements\TryStatement;
use One\Ast\Statements\Block;
use One\Ast\Types\Method;
use One\Ast\Types\Constructor;
use One\Ast\Types\Field;
use One\Ast\Types\Property;
use One\Ast\Types\Interface_;
use One\Ast\Types\Class_;
use One\Ast\Types\Enum;
use One\Ast\Types\EnumMember;
use One\Ast\Types\SourceFile;
use One\Ast\Types\IVariable;
use One\Ast\Types\IVariableWithInitializer;
use One\Ast\Types\MethodParameter;
use One\Ast\Types\Lambda;
use One\Ast\Types\IMethodBase;
use One\Ast\Types\Package;
use One\Ast\Types\GlobalFunction;
use One\Ast\Types\IInterface;
use One\Ast\Types\IAstNode;
use One\Ast\Types\IHasAttributesAndTrivia;
use One\Ast\Types\Import;
use One\Ast\References\ClassReference;
use One\Ast\References\EnumReference;
use One\Ast\References\ThisReference;
use One\Ast\References\MethodParameterReference;
use One\Ast\References\VariableDeclarationReference;
use One\Ast\References\ForVariableReference;
use One\Ast\References\ForeachVariableReference;
use One\Ast\References\SuperReference;
use One\Ast\References\InstanceFieldReference;
use One\Ast\References\InstancePropertyReference;
use One\Ast\References\StaticPropertyReference;
use One\Ast\References\StaticFieldReference;
use One\Ast\References\CatchVariableReference;
use One\Ast\References\GlobalFunctionReference;
use One\Ast\References\EnumMemberReference;
use One\Ast\References\StaticThisReference;
use One\Ast\References\VariableReference;
use One\ErrorManager\ErrorManager;
use One\ITransformer\ITransformer;
use One\Ast\Interfaces\IType;

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
