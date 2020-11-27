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
        for (Integer i = 0; i < newExpr.getArgs().length; i++) {
            newExpr.getArgs()[i].setExpectedType(newExpr.cls.decl.constructor_.getParameters()[i].getType(), false);
            newExpr.getArgs()[i] = this.main.runPluginsOn(newExpr.getArgs()[i]);
        }
        expr.setActualType(newExpr.cls, false, false);
        return expr;
    }
}