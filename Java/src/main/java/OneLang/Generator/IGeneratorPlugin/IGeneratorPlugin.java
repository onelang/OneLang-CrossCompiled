package OneLang.Generator.IGeneratorPlugin;

import OneLang.One.Ast.Interfaces.IExpression;
import OneLang.One.Ast.Statements.Statement;

import OneLang.One.Ast.Interfaces.IExpression;
import OneLang.One.Ast.Statements.Statement;

public interface IGeneratorPlugin {
    String expr(IExpression expr);
    String stmt(Statement stmt);
}