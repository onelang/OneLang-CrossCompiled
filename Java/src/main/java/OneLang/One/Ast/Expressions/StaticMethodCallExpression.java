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
import OneLang.One.Ast.Expressions.IMethodCallExpression;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Interfaces.IType;

public class StaticMethodCallExpression extends Expression implements IMethodCallExpression {
    public Boolean isThisCall;
    
    Method method;
    public Method getMethod() { return this.method; }
    public void setMethod(Method value) { this.method = value; }
    
    IType[] typeArgs;
    public IType[] getTypeArgs() { return this.typeArgs; }
    public void setTypeArgs(IType[] value) { this.typeArgs = value; }
    
    Expression[] args;
    public Expression[] getArgs() { return this.args; }
    public void setArgs(Expression[] value) { this.args = value; }
    
    public StaticMethodCallExpression(Method method, IType[] typeArgs, Expression[] args, Boolean isThisCall)
    {
        super();
        this.setMethod(method);
        this.setTypeArgs(typeArgs);
        this.setArgs(args);
        this.isThisCall = isThisCall;
    }
}