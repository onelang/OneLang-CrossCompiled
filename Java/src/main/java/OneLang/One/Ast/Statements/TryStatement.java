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
import OneLang.One.Ast.Statements.Block;
import OneLang.One.Ast.Statements.CatchVariable;

public class TryStatement extends Statement {
    public Block tryBody;
    public CatchVariable catchVar;
    public Block catchBody;
    public Block finallyBody;
    
    public TryStatement(Block tryBody, CatchVariable catchVar, Block catchBody, Block finallyBody)
    {
        super();
        this.tryBody = tryBody;
        this.catchVar = catchVar;
        this.catchBody = catchBody;
        this.finallyBody = finallyBody;
        if (this.catchBody == null && this.finallyBody == null)
            throw new Error("try without catch and finally is not allowed");
    }
}