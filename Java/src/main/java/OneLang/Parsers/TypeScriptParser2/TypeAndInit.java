package OneLang.Parsers.TypeScriptParser2;

import OneLang.Parsers.Common.Reader.Reader;
import OneLang.Parsers.Common.Reader.IReaderHooks;
import OneLang.Parsers.Common.Reader.ParseError;
import OneLang.Parsers.Common.ExpressionParser.ExpressionParser;
import OneLang.Parsers.Common.ExpressionParser.IExpressionParserHooks;
import OneLang.Parsers.Common.NodeManager.NodeManager;
import OneLang.Parsers.Common.IParser.IParser;
import OneLang.One.Ast.AstTypes.AnyType;
import OneLang.One.Ast.AstTypes.VoidType;
import OneLang.One.Ast.AstTypes.UnresolvedType;
import OneLang.One.Ast.AstTypes.LambdaType;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.TemplateString;
import OneLang.One.Ast.Expressions.TemplateStringPart;
import OneLang.One.Ast.Expressions.NewExpression;
import OneLang.One.Ast.Expressions.Identifier;
import OneLang.One.Ast.Expressions.CastExpression;
import OneLang.One.Ast.Expressions.NullLiteral;
import OneLang.One.Ast.Expressions.BooleanLiteral;
import OneLang.One.Ast.Expressions.BinaryExpression;
import OneLang.One.Ast.Expressions.UnaryExpression;
import OneLang.One.Ast.Expressions.UnresolvedCallExpression;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Ast.Expressions.InstanceOfExpression;
import OneLang.One.Ast.Expressions.RegexLiteral;
import OneLang.One.Ast.Expressions.AwaitExpression;
import OneLang.One.Ast.Expressions.ParenthesizedExpression;
import OneLang.One.Ast.Expressions.UnresolvedNewExpression;
import OneLang.One.Ast.Statements.VariableDeclaration;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.Statements.UnsetStatement;
import OneLang.One.Ast.Statements.IfStatement;
import OneLang.One.Ast.Statements.WhileStatement;
import OneLang.One.Ast.Statements.ForeachStatement;
import OneLang.One.Ast.Statements.ForStatement;
import OneLang.One.Ast.Statements.ReturnStatement;
import OneLang.One.Ast.Statements.ThrowStatement;
import OneLang.One.Ast.Statements.BreakStatement;
import OneLang.One.Ast.Statements.ExpressionStatement;
import OneLang.One.Ast.Statements.ForeachVariable;
import OneLang.One.Ast.Statements.ForVariable;
import OneLang.One.Ast.Statements.DoStatement;
import OneLang.One.Ast.Statements.ContinueStatement;
import OneLang.One.Ast.Statements.TryStatement;
import OneLang.One.Ast.Statements.CatchVariable;
import OneLang.One.Ast.Statements.Block;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Types.MethodParameter;
import OneLang.One.Ast.Types.Field;
import OneLang.One.Ast.Types.Visibility;
import OneLang.One.Ast.Types.SourceFile;
import OneLang.One.Ast.Types.Property;
import OneLang.One.Ast.Types.Constructor;
import OneLang.One.Ast.Types.Interface;
import OneLang.One.Ast.Types.EnumMember;
import OneLang.One.Ast.Types.Enum;
import OneLang.One.Ast.Types.IMethodBase;
import OneLang.One.Ast.Types.Import;
import OneLang.One.Ast.Types.SourcePath;
import OneLang.One.Ast.Types.ExportScopeRef;
import OneLang.One.Ast.Types.Package;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.Ast.Types.UnresolvedImport;
import OneLang.One.Ast.Types.GlobalFunction;
import OneLang.One.Ast.Interfaces.IType;

import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.Expressions.Expression;

public class TypeAndInit {
    public IType type;
    public Expression init;
    
    public TypeAndInit(IType type, Expression init)
    {
        this.type = type;
        this.init = init;
    }
}