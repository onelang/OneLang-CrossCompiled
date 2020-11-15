using One.Ast;
using One;
using System.Collections.Generic;

namespace One.Transforms
{
    public class UseDefaultCallArgsExplicitly : AstTransformer {
        public UseDefaultCallArgsExplicitly(): base("UseDefaultCallArgsExplicitly")
        {
            
        }
        
        protected Expression[] getNewArgs(Expression[] args, IMethodBaseWithTrivia method)
        {
            if (method.attributes.hasKey("UseDefaultCallArgsExplicitly") && method.attributes.get("UseDefaultCallArgsExplicitly") == "disable")
                return args;
            if (args.length() >= method.parameters.length())
                return args;
            
            var newArgs = new List<Expression>();
            for (int i = 0; i < method.parameters.length(); i++) {
                var init = method.parameters.get(i).initializer;
                if (i >= args.length() && init == null) {
                    this.errorMan.throw_($"Missing default value for parameter #{i + 1}!");
                    break;
                }
                newArgs.push(i < args.length() ? args.get(i) : init);
            }
            return newArgs.ToArray();
        }
        
        protected override Expression visitExpression(Expression expr)
        {
            base.visitExpression(expr);
            if (expr is NewExpression newExpr && newExpr.cls.decl.constructor_ != null)
                newExpr.args = this.getNewArgs(newExpr.args, newExpr.cls.decl.constructor_);
            else if (expr is InstanceMethodCallExpression instMethCallExpr)
                instMethCallExpr.args = this.getNewArgs(instMethCallExpr.args, instMethCallExpr.method);
            else if (expr is StaticMethodCallExpression statMethCallExpr)
                statMethCallExpr.args = this.getNewArgs(statMethCallExpr.args, statMethCallExpr.method);
            return expr;
        }
    }
}