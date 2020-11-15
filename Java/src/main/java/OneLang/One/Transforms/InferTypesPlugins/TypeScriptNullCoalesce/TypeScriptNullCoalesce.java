package OneLang.One.Transforms.InferTypesPlugins.TypeScriptNullCoalesce;

import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.BinaryExpression;
import OneLang.One.Ast.Expressions.NullLiteral;
import OneLang.One.Ast.Expressions.NullCoalesceExpression;
import OneLang.One.Ast.Expressions.ArrayLiteral;
import OneLang.One.Ast.Expressions.MapLiteral;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.TypeHelper;

import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import OneLang.One.Ast.Expressions.BinaryExpression;
import OneStd.Objects;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.ArrayLiteral;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.Expressions.NullCoalesceExpression;
import OneLang.One.Ast.Expressions.MapLiteral;
import OneLang.One.Ast.Expressions.NullLiteral;

public class TypeScriptNullCoalesce extends InferTypesPlugin {
    public TypeScriptNullCoalesce()
    {
        super("TypeScriptNullCoalesce");
        
    }
    
    public Boolean canTransform(Expression expr) {
        return expr instanceof BinaryExpression && Objects.equals(((BinaryExpression)expr).operator, "||");
    }
    
    public Expression transform(Expression expr) {
        if (expr instanceof BinaryExpression && Objects.equals(((BinaryExpression)expr).operator, "||")) {
            var litTypes = this.main.currentFile.literalTypes;
            
            ((BinaryExpression)expr).left = this.main.runPluginsOn(((BinaryExpression)expr).left);
            var leftType = ((BinaryExpression)expr).left.getType();
            
            if (((BinaryExpression)expr).right instanceof ArrayLiteral && ((ArrayLiteral)((BinaryExpression)expr).right).items.length == 0) {
                if (leftType instanceof ClassType && ((ClassType)leftType).decl == litTypes.array.decl) {
                    ((ArrayLiteral)((BinaryExpression)expr).right).setActualType(((ClassType)leftType), false, false);
                    return new NullCoalesceExpression(((BinaryExpression)expr).left, ((ArrayLiteral)((BinaryExpression)expr).right));
                }
            }
            
            if (((BinaryExpression)expr).right instanceof MapLiteral && ((MapLiteral)((BinaryExpression)expr).right).items.length == 0) {
                if (leftType instanceof ClassType && ((ClassType)leftType).decl == litTypes.map.decl) {
                    ((MapLiteral)((BinaryExpression)expr).right).setActualType(((ClassType)leftType), false, false);
                    return new NullCoalesceExpression(((BinaryExpression)expr).left, ((MapLiteral)((BinaryExpression)expr).right));
                }
            }
            
            ((BinaryExpression)expr).right = this.main.runPluginsOn(((BinaryExpression)expr).right);
            var rightType = ((BinaryExpression)expr).right.getType();
            
            if (((BinaryExpression)expr).right instanceof NullLiteral)
                // something-which-can-be-undefined || null
                return ((BinaryExpression)expr).left;
            else if (TypeHelper.isAssignableTo(rightType, leftType) && !TypeHelper.equals(rightType, this.main.currentFile.literalTypes.boolean_))
                return new NullCoalesceExpression(((BinaryExpression)expr).left, ((BinaryExpression)expr).right);
        }
        return expr;
    }
}