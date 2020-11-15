package OneLang.One.Transforms.InferTypesPlugins.BasicTypeInfer;

import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.CastExpression;
import OneLang.One.Ast.Expressions.ParenthesizedExpression;
import OneLang.One.Ast.Expressions.BooleanLiteral;
import OneLang.One.Ast.Expressions.NumericLiteral;
import OneLang.One.Ast.Expressions.StringLiteral;
import OneLang.One.Ast.Expressions.TemplateString;
import OneLang.One.Ast.Expressions.RegexLiteral;
import OneLang.One.Ast.Expressions.InstanceOfExpression;
import OneLang.One.Ast.Expressions.NullLiteral;
import OneLang.One.Ast.Expressions.UnaryExpression;
import OneLang.One.Ast.Expressions.BinaryExpression;
import OneLang.One.Ast.Expressions.ConditionalExpression;
import OneLang.One.Ast.Expressions.NewExpression;
import OneLang.One.Ast.Expressions.NullCoalesceExpression;
import OneLang.One.Ast.Expressions.LambdaCallExpression;
import OneLang.One.Ast.Expressions.AwaitExpression;
import OneLang.One.Ast.References.ThisReference;
import OneLang.One.Ast.References.MethodParameterReference;
import OneLang.One.Ast.References.VariableDeclarationReference;
import OneLang.One.Ast.References.ForeachVariableReference;
import OneLang.One.Ast.References.ForVariableReference;
import OneLang.One.Ast.References.SuperReference;
import OneLang.One.Ast.References.CatchVariableReference;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.AnyType;
import OneLang.One.Ast.AstTypes.EnumType;
import OneLang.One.Ast.AstTypes.NullType;
import OneLang.One.Ast.AstTypes.TypeHelper;

import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.CastExpression;
import OneLang.One.Ast.Expressions.ParenthesizedExpression;
import OneLang.One.Ast.References.ThisReference;
import OneLang.One.Ast.References.SuperReference;
import OneLang.One.Ast.References.MethodParameterReference;
import OneLang.One.Ast.Expressions.BooleanLiteral;
import OneLang.One.Ast.Expressions.NumericLiteral;
import OneLang.One.Ast.Expressions.StringLiteral;
import OneLang.One.Ast.Expressions.TemplateString;
import OneLang.One.Ast.Expressions.RegexLiteral;
import OneLang.One.Ast.Expressions.InstanceOfExpression;
import OneLang.One.Ast.Expressions.NullLiteral;
import OneLang.One.Ast.References.VariableDeclarationReference;
import OneLang.One.Ast.References.ForeachVariableReference;
import OneLang.One.Ast.References.ForVariableReference;
import OneLang.One.Ast.References.CatchVariableReference;
import OneLang.One.Ast.Expressions.UnaryExpression;
import OneLang.One.Ast.AstTypes.ClassType;
import OneStd.Objects;
import OneLang.One.Ast.AstTypes.AnyType;
import OneLang.One.Ast.Expressions.BinaryExpression;
import java.util.List;
import java.util.ArrayList;
import OneLang.One.Ast.AstTypes.EnumType;
import OneLang.One.Ast.Expressions.ConditionalExpression;
import OneLang.One.Ast.Expressions.NullCoalesceExpression;
import OneLang.One.Ast.Expressions.AwaitExpression;

public class BasicTypeInfer extends InferTypesPlugin {
    public BasicTypeInfer()
    {
        super("BasicTypeInfer");
        
    }
    
    public Boolean canDetectType(Expression expr) {
        return true;
    }
    
    public Boolean detectType(Expression expr) {
        var litTypes = this.main.currentFile.literalTypes;
        
        if (expr instanceof CastExpression)
            ((CastExpression)expr).setActualType(((CastExpression)expr).newType, false, false);
        else if (expr instanceof ParenthesizedExpression)
            ((ParenthesizedExpression)expr).setActualType(((ParenthesizedExpression)expr).expression.getType(), false, false);
        else if (expr instanceof ThisReference)
            ((ThisReference)expr).setActualType(((ThisReference)expr).cls.type, false, false);
        else if (expr instanceof SuperReference)
            ((SuperReference)expr).setActualType(((SuperReference)expr).cls.type, false, false);
        else if (expr instanceof MethodParameterReference)
            ((MethodParameterReference)expr).setActualType(((MethodParameterReference)expr).decl.getType(), false, false);
        else if (expr instanceof BooleanLiteral)
            ((BooleanLiteral)expr).setActualType(litTypes.boolean_, false, false);
        else if (expr instanceof NumericLiteral)
            ((NumericLiteral)expr).setActualType(litTypes.numeric, false, false);
        else if (expr instanceof StringLiteral || expr instanceof TemplateString)
            expr.setActualType(litTypes.string, false, false);
        else if (expr instanceof RegexLiteral)
            ((RegexLiteral)expr).setActualType(litTypes.regex, false, false);
        else if (expr instanceof InstanceOfExpression)
            ((InstanceOfExpression)expr).setActualType(litTypes.boolean_, false, false);
        else if (expr instanceof NullLiteral)
            ((NullLiteral)expr).setActualType(((NullLiteral)expr).expectedType != null ? ((NullLiteral)expr).expectedType : NullType.instance, false, false);
        else if (expr instanceof VariableDeclarationReference)
            ((VariableDeclarationReference)expr).setActualType(((VariableDeclarationReference)expr).decl.getType(), false, false);
        else if (expr instanceof ForeachVariableReference)
            ((ForeachVariableReference)expr).setActualType(((ForeachVariableReference)expr).decl.getType(), false, false);
        else if (expr instanceof ForVariableReference)
            ((ForVariableReference)expr).setActualType(((ForVariableReference)expr).decl.getType(), false, false);
        else if (expr instanceof CatchVariableReference)
            ((CatchVariableReference)expr).setActualType(((CatchVariableReference)expr).decl.getType() != null ? ((CatchVariableReference)expr).decl.getType() : this.main.currentFile.literalTypes.error, false, false);
        else if (expr instanceof UnaryExpression) {
            var operandType = ((UnaryExpression)expr).operand.getType();
            if (operandType instanceof ClassType) {
                var opId = ((UnaryExpression)expr).operator + ((ClassType)operandType).decl.getName();
                
                if (Objects.equals(opId, "-TsNumber"))
                    ((UnaryExpression)expr).setActualType(litTypes.numeric, false, false);
                else if (Objects.equals(opId, "+TsNumber"))
                    ((UnaryExpression)expr).setActualType(litTypes.numeric, false, false);
                else if (Objects.equals(opId, "!TsBoolean"))
                    ((UnaryExpression)expr).setActualType(litTypes.boolean_, false, false);
                else if (Objects.equals(opId, "++TsNumber"))
                    ((UnaryExpression)expr).setActualType(litTypes.numeric, false, false);
                else if (Objects.equals(opId, "--TsNumber"))
                    ((UnaryExpression)expr).setActualType(litTypes.numeric, false, false);
                else { }
            }
            else if (operandType instanceof AnyType)
                ((UnaryExpression)expr).setActualType(AnyType.instance, false, false);
            else { }
        }
        else if (expr instanceof BinaryExpression) {
            var leftType = ((BinaryExpression)expr).left.getType();
            var rightType = ((BinaryExpression)expr).right.getType();
            var isEqOrNeq = Objects.equals(((BinaryExpression)expr).operator, "==") || Objects.equals(((BinaryExpression)expr).operator, "!=");
            if (Objects.equals(((BinaryExpression)expr).operator, "=")) {
                if (TypeHelper.isAssignableTo(rightType, leftType))
                    ((BinaryExpression)expr).setActualType(leftType, false, true);
                else
                    throw new Error("Right-side expression (" + rightType.repr() + ") is not assignable to left-side (" + leftType.repr() + ").");
            }
            else if (isEqOrNeq)
                ((BinaryExpression)expr).setActualType(litTypes.boolean_, false, false);
            else if (leftType instanceof ClassType && rightType instanceof ClassType) {
                if (((ClassType)leftType).decl == litTypes.numeric.decl && ((ClassType)rightType).decl == litTypes.numeric.decl && new ArrayList<>(List.of("-", "+", "-=", "+=", "%", "/")).stream().anyMatch(((BinaryExpression)expr).operator::equals))
                    ((BinaryExpression)expr).setActualType(litTypes.numeric, false, false);
                else if (((ClassType)leftType).decl == litTypes.numeric.decl && ((ClassType)rightType).decl == litTypes.numeric.decl && new ArrayList<>(List.of("<", "<=", ">", ">=")).stream().anyMatch(((BinaryExpression)expr).operator::equals))
                    ((BinaryExpression)expr).setActualType(litTypes.boolean_, false, false);
                else if (((ClassType)leftType).decl == litTypes.string.decl && ((ClassType)rightType).decl == litTypes.string.decl && new ArrayList<>(List.of("+", "+=")).stream().anyMatch(((BinaryExpression)expr).operator::equals))
                    ((BinaryExpression)expr).setActualType(litTypes.string, false, false);
                else if (((ClassType)leftType).decl == litTypes.string.decl && ((ClassType)rightType).decl == litTypes.string.decl && new ArrayList<>(List.of("<=")).stream().anyMatch(((BinaryExpression)expr).operator::equals))
                    ((BinaryExpression)expr).setActualType(litTypes.boolean_, false, false);
                else if (((ClassType)leftType).decl == litTypes.boolean_.decl && ((ClassType)rightType).decl == litTypes.boolean_.decl && new ArrayList<>(List.of("||", "&&")).stream().anyMatch(((BinaryExpression)expr).operator::equals))
                    ((BinaryExpression)expr).setActualType(litTypes.boolean_, false, false);
                else if (((ClassType)leftType).decl == litTypes.string.decl && ((ClassType)rightType).decl == litTypes.map.decl && Objects.equals(((BinaryExpression)expr).operator, "in"))
                    ((BinaryExpression)expr).setActualType(litTypes.boolean_, false, false);
                else { }
            }
            else if (leftType instanceof EnumType && rightType instanceof EnumType) {
                if (((EnumType)leftType).decl == ((EnumType)rightType).decl && isEqOrNeq)
                    ((BinaryExpression)expr).setActualType(litTypes.boolean_, false, false);
                else { }
            }
            else if (leftType instanceof AnyType && rightType instanceof AnyType)
                ((BinaryExpression)expr).setActualType(AnyType.instance, false, false);
            else { }
        }
        else if (expr instanceof ConditionalExpression) {
            var trueType = ((ConditionalExpression)expr).whenTrue.getType();
            var falseType = ((ConditionalExpression)expr).whenFalse.getType();
            if (((ConditionalExpression)expr).expectedType != null) {
                if (!TypeHelper.isAssignableTo(trueType, ((ConditionalExpression)expr).expectedType))
                    throw new Error("Conditional expression expects " + ((ConditionalExpression)expr).expectedType.repr() + " but got " + trueType.repr() + " as true branch");
                if (!TypeHelper.isAssignableTo(falseType, ((ConditionalExpression)expr).expectedType))
                    throw new Error("Conditional expression expects " + ((ConditionalExpression)expr).expectedType.repr() + " but got " + falseType.repr() + " as false branch");
                ((ConditionalExpression)expr).setActualType(((ConditionalExpression)expr).expectedType, false, false);
            }
            else if (TypeHelper.isAssignableTo(trueType, falseType))
                ((ConditionalExpression)expr).setActualType(falseType, false, false);
            else if (TypeHelper.isAssignableTo(falseType, trueType))
                ((ConditionalExpression)expr).setActualType(trueType, false, false);
            else
                throw new Error("Different types in the whenTrue (" + trueType.repr() + ") and whenFalse (" + falseType.repr() + ") expressions of a conditional expression");
        }
        else if (expr instanceof NullCoalesceExpression) {
            var defaultType = ((NullCoalesceExpression)expr).defaultExpr.getType();
            var ifNullType = ((NullCoalesceExpression)expr).exprIfNull.getType();
            if (!TypeHelper.isAssignableTo(ifNullType, defaultType))
                this.errorMan.throw_("Null-coalescing operator tried to assign incompatible type \"" + ifNullType.repr() + "\" to \"" + defaultType.repr() + "\"");
            else
                ((NullCoalesceExpression)expr).setActualType(defaultType, false, false);
        }
        else if (expr instanceof AwaitExpression) {
            var exprType = ((AwaitExpression)expr).expr.getType();
            if (exprType instanceof ClassType && ((ClassType)exprType).decl == litTypes.promise.decl)
                ((AwaitExpression)expr).setActualType((((ClassType)((ClassType)exprType))).getTypeArguments()[0], true, false);
            else
                this.errorMan.throw_("Expected promise type (" + litTypes.promise.repr() + ") for await expression, but got " + exprType.repr());
        }
        else
            return false;
        
        return true;
    }
}