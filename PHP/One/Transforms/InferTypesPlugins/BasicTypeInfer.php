<?php

namespace One\Transforms\InferTypesPlugins\BasicTypeInfer;

use One\Transforms\InferTypesPlugins\Helpers\InferTypesPlugin\InferTypesPlugin;
use One\Ast\Expressions\Expression;
use One\Ast\Expressions\CastExpression;
use One\Ast\Expressions\ParenthesizedExpression;
use One\Ast\Expressions\BooleanLiteral;
use One\Ast\Expressions\NumericLiteral;
use One\Ast\Expressions\StringLiteral;
use One\Ast\Expressions\TemplateString;
use One\Ast\Expressions\RegexLiteral;
use One\Ast\Expressions\InstanceOfExpression;
use One\Ast\Expressions\NullLiteral;
use One\Ast\Expressions\UnaryExpression;
use One\Ast\Expressions\BinaryExpression;
use One\Ast\Expressions\ConditionalExpression;
use One\Ast\Expressions\NewExpression;
use One\Ast\Expressions\NullCoalesceExpression;
use One\Ast\Expressions\LambdaCallExpression;
use One\Ast\Expressions\AwaitExpression;
use One\Ast\References\ThisReference;
use One\Ast\References\MethodParameterReference;
use One\Ast\References\VariableDeclarationReference;
use One\Ast\References\ForeachVariableReference;
use One\Ast\References\ForVariableReference;
use One\Ast\References\SuperReference;
use One\Ast\References\CatchVariableReference;
use One\Ast\AstTypes\ClassType;
use One\Ast\AstTypes\AnyType;
use One\Ast\AstTypes\EnumType;
use One\Ast\AstTypes\NullType;
use One\Ast\AstTypes\TypeHelper;

class BasicTypeInfer extends InferTypesPlugin {
    function __construct() {
        parent::__construct("BasicTypeInfer");
        
    }
    
    function canDetectType($expr) {
        return true;
    }
    
    function detectType($expr) {
        $litTypes = $this->main->currentFile->literalTypes;
        
        if ($expr instanceof CastExpression)
            $expr->setActualType($expr->newType);
        else if ($expr instanceof ParenthesizedExpression)
            $expr->setActualType($expr->expression->getType());
        else if ($expr instanceof ThisReference)
            $expr->setActualType($expr->cls->type, false, false);
        else if ($expr instanceof SuperReference)
            $expr->setActualType($expr->cls->type, false, false);
        else if ($expr instanceof MethodParameterReference)
            $expr->setActualType($expr->decl->type, false, false);
        else if ($expr instanceof BooleanLiteral)
            $expr->setActualType($litTypes->boolean);
        else if ($expr instanceof NumericLiteral)
            $expr->setActualType($litTypes->numeric);
        else if ($expr instanceof StringLiteral || $expr instanceof TemplateString)
            $expr->setActualType($litTypes->string);
        else if ($expr instanceof RegexLiteral)
            $expr->setActualType($litTypes->regex);
        else if ($expr instanceof InstanceOfExpression)
            $expr->setActualType($litTypes->boolean);
        else if ($expr instanceof NullLiteral)
            $expr->setActualType($expr->expectedType !== null ? $expr->expectedType : NullType::$instance);
        else if ($expr instanceof VariableDeclarationReference)
            $expr->setActualType($expr->decl->type);
        else if ($expr instanceof ForeachVariableReference)
            $expr->setActualType($expr->decl->type);
        else if ($expr instanceof ForVariableReference)
            $expr->setActualType($expr->decl->type);
        else if ($expr instanceof CatchVariableReference)
            $expr->setActualType($expr->decl->type ?? $this->main->currentFile->literalTypes->error);
        else if ($expr instanceof UnaryExpression) {
            $operandType = $expr->operand->getType();
            if ($operandType instanceof ClassType) {
                $opId = $expr->operator . $operandType->decl->name;
                
                if ($opId === "-TsNumber")
                    $expr->setActualType($litTypes->numeric);
                else if ($opId === "+TsNumber")
                    $expr->setActualType($litTypes->numeric);
                else if ($opId === "!TsBoolean")
                    $expr->setActualType($litTypes->boolean);
                else if ($opId === "++TsNumber")
                    $expr->setActualType($litTypes->numeric);
                else if ($opId === "--TsNumber")
                    $expr->setActualType($litTypes->numeric);
                else { }
            }
            else if ($operandType instanceof AnyType)
                $expr->setActualType(AnyType::$instance);
            else { }
        }
        else if ($expr instanceof BinaryExpression) {
            $leftType = $expr->left->getType();
            $rightType = $expr->right->getType();
            $isEqOrNeq = $expr->operator === "==" || $expr->operator === "!=";
            if ($expr->operator === "=") {
                if (TypeHelper::isAssignableTo($rightType, $leftType))
                    $expr->setActualType($leftType, false, true);
                else
                    throw new \OneLang\Error("Right-side expression (" . $rightType->repr() . ") is not assignable to left-side (" . $leftType->repr() . ").");
            }
            else if ($isEqOrNeq)
                $expr->setActualType($litTypes->boolean);
            else if ($leftType instanceof ClassType && $rightType instanceof ClassType) {
                if ($leftType->decl === $litTypes->numeric->decl && $rightType->decl === $litTypes->numeric->decl && in_array($expr->operator, array("-", "+", "-=", "+=", "%", "/")))
                    $expr->setActualType($litTypes->numeric);
                else if ($leftType->decl === $litTypes->numeric->decl && $rightType->decl === $litTypes->numeric->decl && in_array($expr->operator, array("<", "<=", ">", ">=")))
                    $expr->setActualType($litTypes->boolean);
                else if ($leftType->decl === $litTypes->string->decl && $rightType->decl === $litTypes->string->decl && in_array($expr->operator, array("+", "+=")))
                    $expr->setActualType($litTypes->string);
                else if ($leftType->decl === $litTypes->string->decl && $rightType->decl === $litTypes->string->decl && in_array($expr->operator, array("<=")))
                    $expr->setActualType($litTypes->boolean);
                else if ($leftType->decl === $litTypes->boolean->decl && $rightType->decl === $litTypes->boolean->decl && in_array($expr->operator, array("||", "&&")))
                    $expr->setActualType($litTypes->boolean);
                else if ($leftType->decl === $litTypes->string->decl && $rightType->decl === $litTypes->map->decl && $expr->operator === "in")
                    $expr->setActualType($litTypes->boolean);
                else { }
            }
            else if ($leftType instanceof EnumType && $rightType instanceof EnumType) {
                if ($leftType->decl === $rightType->decl && $isEqOrNeq)
                    $expr->setActualType($litTypes->boolean);
                else { }
            }
            else if ($leftType instanceof AnyType && $rightType instanceof AnyType)
                $expr->setActualType(AnyType::$instance);
            else { }
        }
        else if ($expr instanceof ConditionalExpression) {
            $trueType = $expr->whenTrue->getType();
            $falseType = $expr->whenFalse->getType();
            if ($expr->expectedType !== null) {
                if (!TypeHelper::isAssignableTo($trueType, $expr->expectedType))
                    throw new \OneLang\Error("Conditional expression expects " . $expr->expectedType->repr() . " but got " . $trueType->repr() . " as true branch");
                if (!TypeHelper::isAssignableTo($falseType, $expr->expectedType))
                    throw new \OneLang\Error("Conditional expression expects " . $expr->expectedType->repr() . " but got " . $falseType->repr() . " as false branch");
                $expr->setActualType($expr->expectedType);
            }
            else if (TypeHelper::isAssignableTo($trueType, $falseType))
                $expr->setActualType($falseType);
            else if (TypeHelper::isAssignableTo($falseType, $trueType))
                $expr->setActualType($trueType);
            else
                throw new \OneLang\Error("Different types in the whenTrue (" . $trueType->repr() . ") and whenFalse (" . $falseType->repr() . ") expressions of a conditional expression");
        }
        else if ($expr instanceof NullCoalesceExpression) {
            $defaultType = $expr->defaultExpr->getType();
            $ifNullType = $expr->exprIfNull->getType();
            if (!TypeHelper::isAssignableTo($ifNullType, $defaultType))
                $this->errorMan->throw("Null-coalescing operator tried to assign incompatible type \"" . $ifNullType->repr() . "\" to \"" . $defaultType->repr() . "\"");
            else
                $expr->setActualType($defaultType);
        }
        else if ($expr instanceof AwaitExpression) {
            $exprType = $expr->expr->getType();
            if ($exprType instanceof ClassType && $exprType->decl === $litTypes->promise->decl)
                $expr->setActualType(($exprType)->typeArguments[0], true);
            else
                $this->errorMan->throw("Expected promise type (" . $litTypes->promise->repr() . ") for await expression, but got " . $exprType->repr());
        }
        else
            return false;
        
        return true;
    }
}
