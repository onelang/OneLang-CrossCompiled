using One.Ast;
using One.Transforms.InferTypesPlugins.Helpers;
using System.Collections.Generic;

namespace One.Transforms.InferTypesPlugins
{
    public class BasicTypeInfer : InferTypesPlugin {
        public BasicTypeInfer(): base("BasicTypeInfer")
        {
            
        }
        
        public override bool canDetectType(Expression expr)
        {
            return true;
        }
        
        public override bool detectType(Expression expr)
        {
            var litTypes = this.main.currentFile.literalTypes;
            
            if (expr is CastExpression castExpr)
                castExpr.setActualType(castExpr.newType);
            else if (expr is ParenthesizedExpression parExpr)
                parExpr.setActualType(parExpr.expression.getType());
            else if (expr is ThisReference thisRef)
                thisRef.setActualType(thisRef.cls.type, false, false);
            else if (expr is SuperReference superRef)
                superRef.setActualType(superRef.cls.type, false, false);
            else if (expr is MethodParameterReference methParRef)
                methParRef.setActualType(methParRef.decl.type, false, false);
            else if (expr is BooleanLiteral boolLit)
                boolLit.setActualType(litTypes.boolean);
            else if (expr is NumericLiteral numLit)
                numLit.setActualType(litTypes.numeric);
            else if (expr is StringLiteral strLit || expr is TemplateString)
                expr.setActualType(litTypes.string_);
            else if (expr is RegexLiteral regexLit)
                regexLit.setActualType(litTypes.regex);
            else if (expr is InstanceOfExpression instOfExpr)
                instOfExpr.setActualType(litTypes.boolean);
            else if (expr is NullLiteral nullLit)
                nullLit.setActualType(nullLit.expectedType != null ? nullLit.expectedType : NullType.instance);
            else if (expr is VariableDeclarationReference varDeclRef)
                varDeclRef.setActualType(varDeclRef.decl.type);
            else if (expr is ForeachVariableReference forVarRef)
                forVarRef.setActualType(forVarRef.decl.type);
            else if (expr is ForVariableReference forVarRef2)
                forVarRef2.setActualType(forVarRef2.decl.type);
            else if (expr is CatchVariableReference catchVarRef)
                catchVarRef.setActualType(catchVarRef.decl.type ?? this.main.currentFile.literalTypes.error);
            else if (expr is UnaryExpression unaryExpr) {
                var operandType = unaryExpr.operand.getType();
                if (operandType is ClassType classType) {
                    var opId = $"{unaryExpr.operator_}{classType.decl.name}";
                    
                    if (opId == "-TsNumber")
                        unaryExpr.setActualType(litTypes.numeric);
                    else if (opId == "+TsNumber")
                        unaryExpr.setActualType(litTypes.numeric);
                    else if (opId == "!TsBoolean")
                        unaryExpr.setActualType(litTypes.boolean);
                    else if (opId == "++TsNumber")
                        unaryExpr.setActualType(litTypes.numeric);
                    else if (opId == "--TsNumber")
                        unaryExpr.setActualType(litTypes.numeric);
                    else { }
                }
                else if (operandType is AnyType)
                    unaryExpr.setActualType(AnyType.instance);
                else { }
            }
            else if (expr is BinaryExpression binExpr) {
                var leftType = binExpr.left.getType();
                var rightType = binExpr.right.getType();
                var isEqOrNeq = binExpr.operator_ == "==" || binExpr.operator_ == "!=";
                if (binExpr.operator_ == "=") {
                    if (TypeHelper.isAssignableTo(rightType, leftType))
                        binExpr.setActualType(leftType, false, true);
                    else
                        throw new Error($"Right-side expression ({rightType.repr()}) is not assignable to left-side ({leftType.repr()}).");
                }
                else if (isEqOrNeq)
                    binExpr.setActualType(litTypes.boolean);
                else if (leftType is ClassType classType2 && rightType is ClassType classType3) {
                    if (classType2.decl == litTypes.numeric.decl && classType3.decl == litTypes.numeric.decl && new List<string> { "-", "+", "-=", "+=", "%", "/" }.includes(binExpr.operator_))
                        binExpr.setActualType(litTypes.numeric);
                    else if (classType2.decl == litTypes.numeric.decl && classType3.decl == litTypes.numeric.decl && new List<string> { "<", "<=", ">", ">=" }.includes(binExpr.operator_))
                        binExpr.setActualType(litTypes.boolean);
                    else if (classType2.decl == litTypes.string_.decl && classType3.decl == litTypes.string_.decl && new List<string> { "+", "+=" }.includes(binExpr.operator_))
                        binExpr.setActualType(litTypes.string_);
                    else if (classType2.decl == litTypes.string_.decl && classType3.decl == litTypes.string_.decl && new List<string> { "<=" }.includes(binExpr.operator_))
                        binExpr.setActualType(litTypes.boolean);
                    else if (classType2.decl == litTypes.boolean.decl && classType3.decl == litTypes.boolean.decl && new List<string> { "||", "&&" }.includes(binExpr.operator_))
                        binExpr.setActualType(litTypes.boolean);
                    else if (classType2.decl == litTypes.string_.decl && classType3.decl == litTypes.map.decl && binExpr.operator_ == "in")
                        binExpr.setActualType(litTypes.boolean);
                    else { }
                }
                else if (leftType is EnumType enumType && rightType is EnumType enumType2) {
                    if (enumType.decl == enumType2.decl && isEqOrNeq)
                        binExpr.setActualType(litTypes.boolean);
                    else { }
                }
                else if (leftType is AnyType && rightType is AnyType)
                    binExpr.setActualType(AnyType.instance);
                else { }
            }
            else if (expr is ConditionalExpression condExpr) {
                var trueType = condExpr.whenTrue.getType();
                var falseType = condExpr.whenFalse.getType();
                if (condExpr.expectedType != null) {
                    if (!TypeHelper.isAssignableTo(trueType, condExpr.expectedType))
                        throw new Error($"Conditional expression expects {condExpr.expectedType.repr()} but got {trueType.repr()} as true branch");
                    if (!TypeHelper.isAssignableTo(falseType, condExpr.expectedType))
                        throw new Error($"Conditional expression expects {condExpr.expectedType.repr()} but got {falseType.repr()} as false branch");
                    condExpr.setActualType(condExpr.expectedType);
                }
                else if (TypeHelper.isAssignableTo(trueType, falseType))
                    condExpr.setActualType(falseType);
                else if (TypeHelper.isAssignableTo(falseType, trueType))
                    condExpr.setActualType(trueType);
                else
                    throw new Error($"Different types in the whenTrue ({trueType.repr()}) and whenFalse ({falseType.repr()}) expressions of a conditional expression");
            }
            else if (expr is NullCoalesceExpression nullCoalExpr) {
                var defaultType = nullCoalExpr.defaultExpr.getType();
                var ifNullType = nullCoalExpr.exprIfNull.getType();
                if (!TypeHelper.isAssignableTo(ifNullType, defaultType))
                    this.errorMan.throw_($"Null-coalescing operator tried to assign incompatible type \"{ifNullType.repr()}\" to \"{defaultType.repr()}\"");
                else
                    nullCoalExpr.setActualType(defaultType);
            }
            else if (expr is AwaitExpression awaitExpr) {
                var exprType = awaitExpr.expr.getType();
                if (exprType is ClassType classType4 && classType4.decl == litTypes.promise.decl)
                    awaitExpr.setActualType((((ClassType)classType4)).typeArguments.get(0), true);
                else
                    this.errorMan.throw_($"Expected promise type ({litTypes.promise.repr()}) for await expression, but got {exprType.repr()}");
            }
            else
                return false;
            
            return true;
        }
    }
}