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
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.Types.IInterface;

public class NewExpression extends Expression implements ICallExpression {
    public ClassType cls;
    
    Expression[] args;
    public Expression[] getArgs() { return this.args; }
    public void setArgs(Expression[] value) { this.args = value; }
    
    public NewExpression(ClassType cls, Expression[] args)
    {
        super();
        this.cls = cls;
        this.setArgs(args);
    }
    
    public String getMethodName() {
        return "constructor";
    }
    
    public IInterface getParentInterface() {
        return this.cls.decl;
    }
}