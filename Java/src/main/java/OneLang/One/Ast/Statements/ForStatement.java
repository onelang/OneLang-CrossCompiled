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
import OneLang.One.Ast.Statements.ForVariable;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Statements.Block;

public class ForStatement extends Statement {
    public ForVariable itemVar;
    public Expression condition;
    public Expression incrementor;
    public Block body;
    
    public ForStatement(ForVariable itemVar, Expression condition, Expression incrementor, Block body)
    {
        super();
        this.itemVar = itemVar;
        this.condition = condition;
        this.incrementor = incrementor;
        this.body = body;
    }
}