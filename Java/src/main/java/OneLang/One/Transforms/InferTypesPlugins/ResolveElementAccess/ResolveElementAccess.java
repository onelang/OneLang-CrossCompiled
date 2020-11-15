package OneLang.One.Transforms.InferTypesPlugins.ResolveElementAccess;

import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.ElementAccessExpression;
import OneLang.One.Ast.Expressions.UnresolvedMethodCallExpression;
import OneLang.One.Ast.Expressions.InstanceMethodCallExpression;
import OneLang.One.Ast.Expressions.StringLiteral;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Ast.Expressions.BinaryExpression;
import OneLang.One.Transforms.InferTypesPlugins.ResolveMethodCalls.ResolveMethodCalls;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.AstTypes.TypeHelper;

import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import OneLang.One.Ast.Expressions.BinaryExpression;
import OneLang.One.Ast.Expressions.ElementAccessExpression;
import java.util.List;
import java.util.ArrayList;
import OneLang.One.Ast.Expressions.Expression;
import java.util.Arrays;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.Expressions.UnresolvedMethodCallExpression;
import OneLang.One.Ast.Expressions.StringLiteral;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;

public class ResolveElementAccess extends InferTypesPlugin {
    public ResolveElementAccess()
    {
        super("ResolveElementAccess");
        
    }
    
    public Boolean canTransform(Expression expr) {
        var isSet = expr instanceof BinaryExpression && ((BinaryExpression)expr).left instanceof ElementAccessExpression && new ArrayList<>(List.of("=")).stream().anyMatch(((BinaryExpression)expr).operator::equals);
        return expr instanceof ElementAccessExpression || isSet;
    }
    
    public Boolean isMapOrArrayType(IType type) {
        return TypeHelper.isAssignableTo(type, this.main.currentFile.literalTypes.map) || Arrays.stream(this.main.currentFile.arrayTypes).anyMatch(x -> TypeHelper.isAssignableTo(type, x));
    }
    
    public Expression transform(Expression expr) {
        // TODO: convert ElementAccess to ElementGet and ElementSet expressions
        if (expr instanceof BinaryExpression && ((BinaryExpression)expr).left instanceof ElementAccessExpression) {
            ((ElementAccessExpression)((BinaryExpression)expr).left).object = this.main.runPluginsOn(((ElementAccessExpression)((BinaryExpression)expr).left).object);
            if (this.isMapOrArrayType(((ElementAccessExpression)((BinaryExpression)expr).left).object.getType()))
                //const right = expr.operator === "=" ? expr.right : new BinaryExpression(<Expression>expr.left.clone(), expr.operator === "+=" ? "+" : "-", expr.right);
                return new UnresolvedMethodCallExpression(((ElementAccessExpression)((BinaryExpression)expr).left).object, "set", new IType[0], new Expression[] { ((ElementAccessExpression)((BinaryExpression)expr).left).elementExpr, ((BinaryExpression)expr).right });
        }
        else if (expr instanceof ElementAccessExpression) {
            ((ElementAccessExpression)expr).object = this.main.runPluginsOn(((ElementAccessExpression)expr).object);
            if (this.isMapOrArrayType(((ElementAccessExpression)expr).object.getType()))
                return new UnresolvedMethodCallExpression(((ElementAccessExpression)expr).object, "get", new IType[0], new Expression[] { ((ElementAccessExpression)expr).elementExpr });
            else if (((ElementAccessExpression)expr).elementExpr instanceof StringLiteral)
                return new PropertyAccessExpression(((ElementAccessExpression)expr).object, ((StringLiteral)((ElementAccessExpression)expr).elementExpr).stringValue);
        }
        return expr;
    }
}