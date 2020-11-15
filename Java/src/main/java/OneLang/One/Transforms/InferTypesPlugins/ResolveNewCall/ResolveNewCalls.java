package OneLang.One.Transforms.InferTypesPlugins.ResolveNewCall;

import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.NewExpression;

import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import OneLang.One.Ast.Expressions.NewExpression;
import OneLang.One.Ast.Expressions.Expression;

public class ResolveNewCalls extends InferTypesPlugin {
    public ResolveNewCalls()
    {
        super("ResolveNewCalls");
        
    }
    
    public Boolean canTransform(Expression expr) {
        return expr instanceof NewExpression;
    }
    
    public Expression transform(Expression expr) {
        var newExpr = ((NewExpression)expr);
        for (Integer i = 0; i < newExpr.args.length; i++) {
            newExpr.args[i].setExpectedType(newExpr.cls.decl.constructor_.getParameters()[i].getType(), false);
            newExpr.args[i] = this.main.runPluginsOn(newExpr.args[i]);
        }
        expr.setActualType(newExpr.cls, false, false);
        return expr;
    }
}