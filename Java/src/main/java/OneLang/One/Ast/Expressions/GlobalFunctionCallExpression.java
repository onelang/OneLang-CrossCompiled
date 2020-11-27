package OneLang.One.Ast.Expressions;

import OneLang.One.Ast.AstTypes.VoidType;
import OneLang.One.Ast.AstTypes.UnresolvedType;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.TypeHelper;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Types.GlobalFunction;
import OneLang.One.Ast.Types.IAstNode;
import OneLang.One.Ast.Types.IInterface;
import OneLang.One.Ast.Interfaces.IExpression;
import OneLang.One.Ast.Interfaces.IType;

import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.ICallExpression;
import OneLang.One.Ast.Types.GlobalFunction;
import OneLang.One.Ast.Types.IInterface;

public class GlobalFunctionCallExpression extends Expression implements ICallExpression {
    public GlobalFunction func;
    
    Expression[] args;
    public Expression[] getArgs() { return this.args; }
    public void setArgs(Expression[] value) { this.args = value; }
    
    public GlobalFunctionCallExpression(GlobalFunction func, Expression[] args)
    {
        super();
        this.func = func;
        this.setArgs(args);
    }
    
    public String getMethodName() {
        return this.func.getName();
    }
    
    public IInterface getParentInterface() {
        return null;
    }
}