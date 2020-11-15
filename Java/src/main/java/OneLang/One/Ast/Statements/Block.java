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

import java.util.List;
import OneLang.One.Ast.Statements.Statement;
import java.util.Arrays;
import java.util.ArrayList;

public class Block {
    public List<Statement> statements;
    
    public Block(Statement[] statements)
    {
        this.statements = new ArrayList<>(Arrays.asList(statements));
    }
}