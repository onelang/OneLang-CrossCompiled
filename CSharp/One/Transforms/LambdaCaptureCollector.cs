using One.Ast;
using One;
using System.Collections.Generic;

namespace One.Transforms
{
    public class LambdaCaptureCollector : AstTransformer
    {
        public List<Set<IVariable>> scopeVarStack;
        public Set<IVariable> scopeVars;
        public Set<IVariable> capturedVars;
        
        public LambdaCaptureCollector(): base("LambdaCaptureCollector")
        {
            this.scopeVarStack = new List<Set<IVariable>>();
            this.scopeVars = null;
            this.capturedVars = null;
        }
        
        protected override Lambda visitLambda(Lambda lambda)
        {
            if (this.scopeVars != null)
                this.scopeVarStack.push(this.scopeVars);
            
            this.scopeVars = new Set<IVariable>();
            this.capturedVars = new Set<IVariable>();
            
            base.visitLambda(lambda);
            lambda.captures = new List<IVariable>();
            foreach (var capture in this.capturedVars.values())
                lambda.captures.push(capture);
            
            this.scopeVars = this.scopeVarStack.length() > 0 ? this.scopeVarStack.pop() : null;
            return lambda;
        }
        
        protected override IVariable visitVariable(IVariable variable)
        {
            if (this.scopeVars != null)
                this.scopeVars.add(variable);
            return variable;
        }
        
        protected override VariableReference visitVariableReference(VariableReference varRef)
        {
            if (varRef is StaticFieldReference statFieldRef || varRef is InstanceFieldReference || varRef is StaticPropertyReference || varRef is InstancePropertyReference || this.scopeVars == null)
                return varRef;
            
            var vari = varRef.getVariable();
            if (!this.scopeVars.has(vari))
                this.capturedVars.add(vari);
            
            return varRef;
        }
    }
}