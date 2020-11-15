package OneLang.One.ErrorManager;

import OneLang.One.Ast.Types.SourceFile;
import OneLang.One.Ast.Types.IInterface;
import OneLang.One.Ast.Types.IMethodBase;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Types.IAstNode;
import OneLang.One.Ast.Types.Field;
import OneLang.One.Ast.Types.Property;
import OneLang.One.Ast.Types.Constructor;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.AstTransformer.AstTransformer;
import OneLang.One.Ast.Statements.Statement;
import OneLang.Utils.TSOverviewGenerator.TSOverviewGenerator;
import OneLang.One.Ast.Expressions.Expression;

public enum LogType { Info, Warning, Error }