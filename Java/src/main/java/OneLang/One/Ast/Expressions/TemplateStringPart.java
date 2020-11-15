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

import OneLang.One.Ast.Types.IAstNode;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.TemplateStringPart;

public class TemplateStringPart implements IAstNode {
    public Boolean isLiteral;
    public String literalText;
    public Expression expression;
    
    public TemplateStringPart(Boolean isLiteral, String literalText, Expression expression)
    {
        this.isLiteral = isLiteral;
        this.literalText = literalText;
        this.expression = expression;
    }
    
    public static TemplateStringPart Literal(String literalText) {
        return new TemplateStringPart(true, literalText, null);
    }
    
    public static TemplateStringPart Expression(Expression expr) {
        return new TemplateStringPart(false, null, expr);
    }
}