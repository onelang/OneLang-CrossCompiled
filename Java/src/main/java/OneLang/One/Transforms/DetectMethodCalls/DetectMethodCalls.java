package OneLang.One.Transforms.DetectMethodCalls;

import OneLang.One.AstTransformer.AstTransformer;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.UnresolvedCallExpression;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Ast.Expressions.UnresolvedMethodCallExpression;

import OneLang.One.AstTransformer.AstTransformer;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.UnresolvedCallExpression;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Ast.Expressions.UnresolvedMethodCallExpression;

public class DetectMethodCalls extends AstTransformer {
    public DetectMethodCalls()
    {
        super("DetectMethodCalls");
        
    }
    
    protected Expression visitExpression(Expression expr) {
        expr = super.visitExpression(expr);
        if (expr instanceof UnresolvedCallExpression && ((UnresolvedCallExpression)expr).func instanceof PropertyAccessExpression)
            return new UnresolvedMethodCallExpression(((PropertyAccessExpression)((UnresolvedCallExpression)expr).func).object, ((PropertyAccessExpression)((UnresolvedCallExpression)expr).func).propertyName, ((UnresolvedCallExpression)expr).typeArgs, ((UnresolvedCallExpression)expr).args);
        return expr;
    }
}