package OneLang.One.Ast.Expressions;

import OneLang.One.Ast.AstTypes.VoidType;
import OneLang.One.Ast.AstTypes.UnresolvedType;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.TypeHelper;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Types.GlobalFunction;
import OneLang.One.Ast.Types.IAstNode;
import OneLang.One.Ast.Interfaces.IExpression;
import OneLang.One.Ast.Interfaces.IType;

import OneLang.One.Ast.Expressions.Expression;

public class RegexLiteral extends Expression {
    public String pattern;
    public Boolean caseInsensitive;
    public Boolean global;
    
    public RegexLiteral(String pattern, Boolean caseInsensitive, Boolean global)
    {
        super();
        this.pattern = pattern;
        this.caseInsensitive = caseInsensitive;
        this.global = global;
    }
}