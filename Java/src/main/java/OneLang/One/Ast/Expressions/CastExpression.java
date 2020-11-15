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
import OneLang.One.Ast.Expressions.InstanceOfExpression;

public class CastExpression extends Expression {
    public IType newType;
    public Expression expression;
    public InstanceOfExpression instanceOfCast;
    
    public CastExpression(IType newType, Expression expression)
    {
        super();
        this.newType = newType;
        this.expression = expression;
        this.instanceOfCast = null;
    }
}