package OneLang.One.Transforms.InferTypesPlugins.NullabilityCheckWithNot;

import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.UnaryExpression;
import OneLang.One.Ast.Expressions.BinaryExpression;
import OneLang.One.Ast.Expressions.NullLiteral;
import OneLang.One.Ast.AstTypes.ClassType;

import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import OneLang.One.Ast.Expressions.UnaryExpression;
import OneStd.Objects;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.Expressions.BinaryExpression;
import OneLang.One.Ast.Expressions.NullLiteral;

public class NullabilityCheckWithNot extends InferTypesPlugin {
    public NullabilityCheckWithNot()
    {
        super("NullabilityCheckWithNot");
        
    }
    
    public Boolean canTransform(Expression expr) {
        return expr instanceof UnaryExpression ? Objects.equals(((UnaryExpression)expr).operator, "!") : false;
    }
    
    public Expression transform(Expression expr) {
        var unaryExpr = ((UnaryExpression)expr);
        if (Objects.equals(unaryExpr.operator, "!")) {
            this.main.processExpression(expr);
            var type = unaryExpr.operand.actualType;
            var litTypes = this.main.currentFile.literalTypes;
            if (type instanceof ClassType && ((ClassType)type).decl != litTypes.boolean_.decl && ((ClassType)type).decl != litTypes.numeric.decl)
                return new BinaryExpression(unaryExpr.operand, "==", new NullLiteral());
        }
        
        return expr;
    }
}