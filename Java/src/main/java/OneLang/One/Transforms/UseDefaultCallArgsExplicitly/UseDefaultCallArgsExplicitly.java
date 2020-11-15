package OneLang.One.Transforms.UseDefaultCallArgsExplicitly;

import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.IMethodCallExpression;
import OneLang.One.Ast.Expressions.InstanceMethodCallExpression;
import OneLang.One.Ast.Expressions.NewExpression;
import OneLang.One.Ast.Expressions.StaticMethodCallExpression;
import OneLang.One.Ast.Types.IMethodBase;
import OneLang.One.Ast.Types.IMethodBaseWithTrivia;
import OneLang.One.Ast.Types.MethodParameter;
import OneLang.One.AstTransformer.AstTransformer;

import OneLang.One.AstTransformer.AstTransformer;
import OneLang.One.Ast.Expressions.Expression;
import OneStd.Objects;
import java.util.ArrayList;
import OneLang.One.Ast.Types.IMethodBaseWithTrivia;
import OneLang.One.Ast.Expressions.NewExpression;
import OneLang.One.Ast.Expressions.InstanceMethodCallExpression;
import OneLang.One.Ast.Expressions.StaticMethodCallExpression;

public class UseDefaultCallArgsExplicitly extends AstTransformer {
    public UseDefaultCallArgsExplicitly()
    {
        super("UseDefaultCallArgsExplicitly");
        
    }
    
    protected Expression[] getNewArgs(Expression[] args, IMethodBaseWithTrivia method) {
        if (method.getAttributes().containsKey("UseDefaultCallArgsExplicitly") && Objects.equals(method.getAttributes().get("UseDefaultCallArgsExplicitly"), "disable"))
            return args;
        if (args.length >= method.getParameters().length)
            return args;
        
        var newArgs = new ArrayList<Expression>();
        for (Integer i = 0; i < method.getParameters().length; i++) {
            var init = method.getParameters()[i].getInitializer();
            if (i >= args.length && init == null) {
                this.errorMan.throw_("Missing default value for parameter #" + i + 1 + "!");
                break;
            }
            newArgs.add(i < args.length ? args[i] : init);
        }
        return newArgs.toArray(Expression[]::new);
    }
    
    protected Expression visitExpression(Expression expr) {
        super.visitExpression(expr);
        if (expr instanceof NewExpression && ((NewExpression)expr).cls.decl.constructor_ != null)
            ((NewExpression)expr).args = this.getNewArgs(((NewExpression)expr).args, ((NewExpression)expr).cls.decl.constructor_);
        else if (expr instanceof InstanceMethodCallExpression)
            ((InstanceMethodCallExpression)expr).setArgs(this.getNewArgs(((InstanceMethodCallExpression)expr).getArgs(), ((InstanceMethodCallExpression)expr).getMethod()));
        else if (expr instanceof StaticMethodCallExpression)
            ((StaticMethodCallExpression)expr).setArgs(this.getNewArgs(((StaticMethodCallExpression)expr).getArgs(), ((StaticMethodCallExpression)expr).getMethod()));
        return expr;
    }
}