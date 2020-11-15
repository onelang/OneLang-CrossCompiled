package OneLang.One.Transforms.LambdaCaptureCollector;

import OneLang.One.AstTransformer.AstTransformer;
import OneLang.One.Ast.AstTypes.UnresolvedType;
import OneLang.One.Ast.AstTypes.GenericsType;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.IVariable;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.References.InstanceFieldReference;
import OneLang.One.Ast.References.InstancePropertyReference;
import OneLang.One.Ast.References.StaticFieldReference;
import OneLang.One.Ast.References.StaticPropertyReference;
import OneLang.One.Ast.References.VariableReference;

import OneLang.One.AstTransformer.AstTransformer;
import java.util.List;
import OneLang.One.Ast.Types.IVariable;
import java.util.Set;
import java.util.ArrayList;
import OneLang.One.Ast.Types.Lambda;
import java.util.LinkedHashSet;
import OneLang.One.Ast.References.VariableReference;
import OneLang.One.Ast.References.StaticFieldReference;
import OneLang.One.Ast.References.InstanceFieldReference;
import OneLang.One.Ast.References.StaticPropertyReference;
import OneLang.One.Ast.References.InstancePropertyReference;

public class LambdaCaptureCollector extends AstTransformer {
    public List<Set<IVariable>> scopeVarStack;
    public Set<IVariable> scopeVars;
    public Set<IVariable> capturedVars;
    
    public LambdaCaptureCollector()
    {
        super("LambdaCaptureCollector");
        this.scopeVarStack = new ArrayList<Set<IVariable>>();
        this.scopeVars = null;
        this.capturedVars = null;
    }
    
    protected Lambda visitLambda(Lambda lambda) {
        if (this.scopeVars != null)
            this.scopeVarStack.add(this.scopeVars);
        
        this.scopeVars = new LinkedHashSet<IVariable>();
        this.capturedVars = new LinkedHashSet<IVariable>();
        
        super.visitLambda(lambda);
        lambda.captures = new ArrayList<IVariable>();
        for (var capture : this.capturedVars.toArray(IVariable[]::new))
            lambda.captures.add(capture);
        
        this.scopeVars = this.scopeVarStack.size() > 0 ? this.scopeVarStack.remove(this.scopeVarStack.size() - 1) : null;
        return lambda;
    }
    
    protected IVariable visitVariable(IVariable variable) {
        if (this.scopeVars != null)
            this.scopeVars.add(variable);
        return variable;
    }
    
    protected VariableReference visitVariableReference(VariableReference varRef) {
        if (varRef instanceof StaticFieldReference || varRef instanceof InstanceFieldReference || varRef instanceof StaticPropertyReference || varRef instanceof InstancePropertyReference || this.scopeVars == null)
            return varRef;
        
        var vari = varRef.getVariable();
        if (!this.scopeVars.contains(vari))
            this.capturedVars.add(vari);
        
        return varRef;
    }
}