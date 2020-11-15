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

public class UnresolvedCallExpression extends Expression {
    public Expression func;
    public IType[] typeArgs;
    public Expression[] args;
    
    public UnresolvedCallExpression(Expression func, IType[] typeArgs, Expression[] args)
    {
        super();
        this.func = func;
        this.typeArgs = typeArgs;
        this.args = args;
    }
}