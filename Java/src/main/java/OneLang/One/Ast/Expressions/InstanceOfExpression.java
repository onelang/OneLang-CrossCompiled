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
import OneLang.One.Ast.Interfaces.IType;
import java.util.List;
import OneLang.One.Ast.Expressions.CastExpression;

public class InstanceOfExpression extends Expression {
    public Expression expr;
    public IType checkType;
    public List<CastExpression> implicitCasts;
    public String alias;
    
    public InstanceOfExpression(Expression expr, IType checkType)
    {
        super();
        this.expr = expr;
        this.checkType = checkType;
        this.implicitCasts = null;
        this.alias = null;
    }
}