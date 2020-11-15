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

import OneLang.One.Ast.Types.IAstNode;

public class CompilationError {
    public String msg;
    public Boolean isWarning;
    public String transformerName;
    public IAstNode node;
    
    public CompilationError(String msg, Boolean isWarning, String transformerName, IAstNode node)
    {
        this.msg = msg;
        this.isWarning = isWarning;
        this.transformerName = transformerName;
        this.node = node;
    }
}