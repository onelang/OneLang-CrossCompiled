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

import OneLang.One.Ast.Interfaces.IExpression;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Types.IInterface;

public interface ICallExpression extends IExpression {
    IType[] getTypeArgs();
    void setTypeArgs(IType[] value);
    
    Expression[] getArgs();
    void setArgs(Expression[] value);
    
    String getName();
    IInterface getParentInterface();
}