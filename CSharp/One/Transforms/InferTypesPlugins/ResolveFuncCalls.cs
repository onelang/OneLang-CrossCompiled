using One.Ast;
using One.Transforms.InferTypesPlugins.Helpers;

namespace One.Transforms.InferTypesPlugins
{
    public class ResolveFuncCalls : InferTypesPlugin {
        public ResolveFuncCalls(): base("ResolveFuncCalls")
        {
            
        }
        
        public override bool canTransform(Expression expr)
        {
            return expr is UnresolvedCallExpression;
        }
        
        public override Expression transform(Expression expr)
        {
            var callExpr = ((UnresolvedCallExpression)expr);
            if (callExpr.func is GlobalFunctionReference globFunctRef) {
                var newExpr = new GlobalFunctionCallExpression(globFunctRef.decl, callExpr.args);
                callExpr.args = callExpr.args.map(arg => this.main.runPluginsOn(arg));
                newExpr.setActualType(globFunctRef.decl.returns);
                return newExpr;
            }
            else {
                this.main.processExpression(expr);
                if (callExpr.func.actualType is LambdaType lambdType) {
                    var newExpr = new LambdaCallExpression(callExpr.func, callExpr.args);
                    newExpr.setActualType(lambdType.returnType);
                    return newExpr;
                }
                else
                    return expr;
            }
        }
    }
}