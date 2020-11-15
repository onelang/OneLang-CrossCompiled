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

public class BinaryExpression extends Expression {
    public Expression left;
    public String operator;
    public Expression right;
    
    public BinaryExpression(Expression left, String operator, Expression right)
    {
        super();
        this.left = left;
        this.operator = operator;
        this.right = right;
    }
}