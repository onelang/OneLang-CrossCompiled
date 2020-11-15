package OneLang.One.Transforms.InferTypesPlugins.ResolveFuncCalls;

import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.UnresolvedCallExpression;
import OneLang.One.Ast.Expressions.GlobalFunctionCallExpression;
import OneLang.One.Ast.Expressions.LambdaCallExpression;
import OneLang.One.Ast.References.GlobalFunctionReference;
import OneLang.One.Ast.AstTypes.LambdaType;

import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import OneLang.One.Ast.Expressions.UnresolvedCallExpression;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.References.GlobalFunctionReference;
import OneLang.One.Ast.Expressions.GlobalFunctionCallExpression;
import java.util.Arrays;
import OneLang.One.Ast.AstTypes.LambdaType;
import OneLang.One.Ast.Expressions.LambdaCallExpression;

public class ResolveFuncCalls extends InferTypesPlugin {
    public ResolveFuncCalls()
    {
        super("ResolveFuncCalls");
        
    }
    
    public Boolean canTransform(Expression expr) {
        return expr instanceof UnresolvedCallExpression;
    }
    
    public Expression transform(Expression expr) {
        var callExpr = ((UnresolvedCallExpression)expr);
        if (callExpr.func instanceof GlobalFunctionReference) {
            var newExpr = new GlobalFunctionCallExpression(((GlobalFunctionReference)callExpr.func).decl, callExpr.args);
            callExpr.args = Arrays.stream(callExpr.args).map(arg -> this.main.runPluginsOn(arg)).toArray(Expression[]::new);
            newExpr.setActualType(((GlobalFunctionReference)callExpr.func).decl.returns, false, false);
            return newExpr;
        }
        else {
            this.main.processExpression(expr);
            if (callExpr.func.actualType instanceof LambdaType) {
                var newExpr = new LambdaCallExpression(callExpr.func, callExpr.args);
                newExpr.setActualType(((LambdaType)callExpr.func.actualType).returnType, false, false);
                return newExpr;
            }
            else
                return expr;
        }
    }
}