using One.Ast;
using One;

namespace One.Transforms
{
    public class FillMutabilityInfo : AstTransformer {
        public FillMutabilityInfo(): base("FillMutabilityInfo")
        {
            
        }
        
        protected IVariable getVar(VariableReference varRef)
        {
            var v = varRef.getVariable();
            if (v.mutability == null)
                v.mutability = new MutabilityInfo(true, false, false);
            return v;
        }
        
        protected override VariableReference visitVariableReference(VariableReference varRef)
        {
            this.getVar(varRef).mutability.unused = false;
            return varRef;
        }
        
        protected override VariableDeclaration visitVariableDeclaration(VariableDeclaration stmt)
        {
            base.visitVariableDeclaration(stmt);
            if (stmt.attributes != null && stmt.attributes.get("mutated") == "true")
                stmt.mutability.mutated = true;
            return stmt;
        }
        
        protected override Expression visitExpression(Expression expr)
        {
            expr = base.visitExpression(expr);
            
            if (expr is BinaryExpression binExpr && binExpr.left is VariableReference varRef && binExpr.operator_ == "=")
                this.getVar(varRef).mutability.reassigned = true;
            else if (expr is InstanceMethodCallExpression instMethCallExpr && instMethCallExpr.object_ is VariableReference varRef2 && instMethCallExpr.method.attributes.hasKey("mutates"))
                this.getVar(varRef2).mutability.mutated = true;
            return expr;
        }
        
        protected override IVariable visitVariable(IVariable variable)
        {
            if (variable.mutability == null)
                variable.mutability = new MutabilityInfo(true, false, false);
            return variable;
        }
    }
}