package OneLang.Parsers.Common.ExpressionParser;

import OneLang.Parsers.Common.Reader.Reader;
import OneLang.Parsers.Common.NodeManager.NodeManager;
import OneLang.One.Ast.AstTypes.UnresolvedType;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.MapLiteral;
import OneLang.One.Ast.Expressions.ArrayLiteral;
import OneLang.One.Ast.Expressions.UnaryType;
import OneLang.One.Ast.Expressions.Identifier;
import OneLang.One.Ast.Expressions.NumericLiteral;
import OneLang.One.Ast.Expressions.StringLiteral;
import OneLang.One.Ast.Expressions.UnresolvedCallExpression;
import OneLang.One.Ast.Expressions.CastExpression;
import OneLang.One.Ast.Expressions.BinaryExpression;
import OneLang.One.Ast.Expressions.UnaryExpression;
import OneLang.One.Ast.Expressions.ParenthesizedExpression;
import OneLang.One.Ast.Expressions.ConditionalExpression;
import OneLang.One.Ast.Expressions.ElementAccessExpression;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Ast.Expressions.MapLiteralItem;
import OneLang.Utils.ArrayHelper.ArrayHelper;
import OneLang.One.Ast.Interfaces.IType;

public class Operator {
    public String text;
    public Integer precedence;
    public Boolean isBinary;
    public Boolean isRightAssoc;
    public Boolean isPostfix;
    
    public Operator(String text, Integer precedence, Boolean isBinary, Boolean isRightAssoc, Boolean isPostfix)
    {
        this.text = text;
        this.precedence = precedence;
        this.isBinary = isBinary;
        this.isRightAssoc = isRightAssoc;
        this.isPostfix = isPostfix;
    }
}