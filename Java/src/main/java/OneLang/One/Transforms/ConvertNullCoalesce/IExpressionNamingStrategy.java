package OneLang.One.Transforms.ConvertNullCoalesce;

import OneLang.Utils.TSOverviewGenerator.TSOverviewGenerator;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.IMethodCallExpression;
import OneLang.One.Ast.Expressions.InstanceMethodCallExpression;
import OneLang.One.Ast.Expressions.NullCoalesceExpression;
import OneLang.One.Ast.Expressions.StaticMethodCallExpression;
import OneLang.One.Ast.References.InstanceFieldReference;
import OneLang.One.Ast.References.StaticFieldReference;
import OneLang.One.Ast.References.VariableDeclarationReference;
import OneLang.One.Ast.Statements.Block;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.Statements.VariableDeclaration;
import OneLang.One.Ast.Types.IMethodBase;
import OneLang.One.Ast.Types.IVariable;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.Ast.Types.MutabilityInfo;
import OneLang.One.AstTransformer.AstTransformer;

import OneLang.One.Ast.Expressions.Expression;

public interface IExpressionNamingStrategy {
    String getNameFor(Expression expr);
}