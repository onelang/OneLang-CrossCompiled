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

public class UnresolvedMethodCallExpression extends Expression {
    public Expression object;
    public String methodName;
    public IType[] typeArgs;
    public Expression[] args;
    
    public UnresolvedMethodCallExpression(Expression object, String methodName, IType[] typeArgs, Expression[] args)
    {
        super();
        this.object = object;
        this.methodName = methodName;
        this.typeArgs = typeArgs;
        this.args = args;
    }
}