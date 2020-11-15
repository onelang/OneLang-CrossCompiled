package OneLang.One.Ast.Statements;

import OneLang.One.Ast.Types.IVariableWithInitializer;
import OneLang.One.Ast.Types.IVariable;
import OneLang.One.Ast.Types.IHasAttributesAndTrivia;
import OneLang.One.Ast.Types.IAstNode;
import OneLang.One.Ast.Types.MutabilityInfo;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.References.ForVariableReference;
import OneLang.One.Ast.References.ForeachVariableReference;
import OneLang.One.Ast.References.VariableDeclarationReference;
import OneLang.One.Ast.References.IReferencable;
import OneLang.One.Ast.References.Reference;
import OneLang.One.Ast.References.CatchVariableReference;
import OneLang.One.Ast.Interfaces.IType;

import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.Types.IVariableWithInitializer;
import OneLang.One.Ast.References.IReferencable;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.Expressions.Expression;
import java.util.List;
import OneLang.One.Ast.References.VariableDeclarationReference;
import OneLang.One.Ast.Types.MutabilityInfo;
import java.util.ArrayList;
import OneLang.One.Ast.References.Reference;

public class VariableDeclaration extends Statement implements IVariableWithInitializer, IReferencable {
    public List<VariableDeclarationReference> references;
    
    String name;
    public String getName() { return this.name; }
    public void setName(String value) { this.name = value; }
    
    IType type;
    public IType getType() { return this.type; }
    public void setType(IType value) { this.type = value; }
    
    Expression initializer;
    public Expression getInitializer() { return this.initializer; }
    public void setInitializer(Expression value) { this.initializer = value; }
    
    MutabilityInfo mutability = null;
    public MutabilityInfo getMutability() { return this.mutability; }
    public void setMutability(MutabilityInfo value) { this.mutability = value; }
    
    public VariableDeclaration(String name, IType type, Expression initializer)
    {
        super();
        this.setName(name);
        this.setType(type);
        this.setInitializer(initializer);
        this.references = new ArrayList<VariableDeclarationReference>();
    }
    
    public Reference createReference() {
        return new VariableDeclarationReference(this);
    }
}