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
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Statements.Block;

public class WhileStatement extends Statement {
    public Expression condition;
    public Block body;
    
    public WhileStatement(Expression condition, Block body)
    {
        super();
        this.condition = condition;
        this.body = body;
    }
}