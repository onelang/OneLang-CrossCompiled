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

public class InstanceMethodCallExpression extends Expression implements IMethodCallExpression {
    public Expression object;
    
    Method method;
    public Method getMethod() { return this.method; }
    public void setMethod(Method value) { this.method = value; }
    
    IType[] typeArgs;
    public IType[] getTypeArgs() { return this.typeArgs; }
    public void setTypeArgs(IType[] value) { this.typeArgs = value; }
    
    Expression[] args;
    public Expression[] getArgs() { return this.args; }
    public void setArgs(Expression[] value) { this.args = value; }
    
    public InstanceMethodCallExpression(Expression object, Method method, IType[] typeArgs, Expression[] args)
    {
        super();
        this.object = object;
        this.setMethod(method);
        this.setTypeArgs(typeArgs);
        this.setArgs(args);
    }
}