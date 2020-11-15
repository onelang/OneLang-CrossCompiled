using One.Ast;
using One;

namespace One.Transforms
{
    public class ConvertToMethodCall : AstTransformer {
        public ConvertToMethodCall(): base("ConvertToMethodCall")
        {
            
        }
        
        protected override Expression visitExpression(Expression expr)
        {
            var origExpr = expr;
            
            expr = base.visitExpression(expr);
            
            if (expr is BinaryExpression binExpr && binExpr.operator_ == "in")
                expr = new UnresolvedCallExpression(new PropertyAccessExpression(binExpr.right, "hasKey"), new IType[0], new Expression[] { binExpr.left });
            
            expr.parentNode = origExpr.parentNode;
            return expr;
        }
    }
}