package OneLang.One.Transforms.FillMutabilityInfo;

import OneLang.One.AstTransformer.AstTransformer;
import OneLang.One.Ast.Types.IVariable;
import OneLang.One.Ast.Types.MutabilityInfo;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.BinaryExpression;
import OneLang.One.Ast.Expressions.InstanceMethodCallExpression;
import OneLang.One.Ast.References.VariableReference;
import OneLang.One.Ast.Statements.VariableDeclaration;

import OneLang.One.AstTransformer.AstTransformer;
import OneLang.One.Ast.Types.IVariable;
import OneLang.One.Ast.Types.MutabilityInfo;
import OneLang.One.Ast.References.VariableReference;
import OneLang.One.Ast.Statements.VariableDeclaration;
import io.onelang.std.core.Objects;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.BinaryExpression;
import OneLang.One.Ast.Expressions.InstanceMethodCallExpression;

public class FillMutabilityInfo extends AstTransformer {
    public FillMutabilityInfo()
    {
        super("FillMutabilityInfo");
        
    }
    
    protected IVariable getVar(VariableReference varRef) {
        var v = varRef.getVariable();
        if (v.getMutability() == null)
            v.setMutability(new MutabilityInfo(true, false, false));
        return v;
    }
    
    protected VariableReference visitVariableReference(VariableReference varRef) {
        this.getVar(varRef).getMutability().unused = false;
        return varRef;
    }
    
    protected VariableDeclaration visitVariableDeclaration(VariableDeclaration stmt) {
        super.visitVariableDeclaration(stmt);
        if (stmt.getAttributes() != null && Objects.equals(stmt.getAttributes().get("mutated"), "true"))
            stmt.getMutability().mutated = true;
        return stmt;
    }
    
    protected Expression visitExpression(Expression expr) {
        expr = super.visitExpression(expr);
        
        if (expr instanceof BinaryExpression && ((BinaryExpression)expr).left instanceof VariableReference && Objects.equals(((BinaryExpression)expr).operator, "="))
            this.getVar(((VariableReference)((BinaryExpression)expr).left)).getMutability().reassigned = true;
        else if (expr instanceof InstanceMethodCallExpression && ((InstanceMethodCallExpression)expr).object instanceof VariableReference && ((InstanceMethodCallExpression)expr).getMethod().getAttributes().containsKey("mutates"))
            this.getVar(((VariableReference)((InstanceMethodCallExpression)expr).object)).getMutability().mutated = true;
        return expr;
    }
    
    protected IVariable visitVariable(IVariable variable) {
        if (variable.getMutability() == null)
            variable.setMutability(new MutabilityInfo(true, false, false));
        return variable;
    }
}