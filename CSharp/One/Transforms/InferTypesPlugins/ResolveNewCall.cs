using One.Ast;
using One.Transforms.InferTypesPlugins.Helpers;

namespace One.Transforms.InferTypesPlugins
{
    public class ResolveNewCalls : InferTypesPlugin {
        public ResolveNewCalls(): base("ResolveNewCalls")
        {
            
        }
        
        public override bool canTransform(Expression expr)
        {
            return expr is NewExpression;
        }
        
        public override Expression transform(Expression expr)
        {
            var newExpr = ((NewExpression)expr);
            for (int i = 0; i < newExpr.args.length(); i++) {
                newExpr.args.get(i).setExpectedType(newExpr.cls.decl.constructor_.parameters.get(i).type);
                newExpr.args.set(i, this.main.runPluginsOn(newExpr.args.get(i)));
            }
            expr.setActualType(newExpr.cls);
            return expr;
        }
    }
}