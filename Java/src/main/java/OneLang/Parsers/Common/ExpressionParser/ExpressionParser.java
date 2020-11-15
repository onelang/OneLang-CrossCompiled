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

import OneLang.Parsers.Common.ExpressionParser.Operator;
import java.util.Map;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.Parsers.Common.Reader.Reader;
import OneLang.Parsers.Common.ExpressionParser.IExpressionParserHooks;
import OneLang.Parsers.Common.NodeManager.NodeManager;
import OneLang.Parsers.Common.ExpressionParser.ExpressionParserConfig;
import OneLang.Parsers.Common.ExpressionParser.PrecedenceLevel;
import OneStd.Objects;
import java.util.Arrays;
import java.util.LinkedHashMap;
import OneLang.One.Ast.Expressions.MapLiteral;
import OneLang.One.Ast.Expressions.MapLiteralItem;
import java.util.ArrayList;
import OneLang.One.Ast.Expressions.ArrayLiteral;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.UnaryExpression;
import OneLang.One.Ast.Expressions.Identifier;
import OneLang.One.Ast.Expressions.NumericLiteral;
import OneLang.One.Ast.Expressions.StringLiteral;
import OneLang.One.Ast.Expressions.ParenthesizedExpression;
import OneLang.One.Ast.Expressions.BinaryExpression;
import OneLang.One.Ast.Expressions.ConditionalExpression;
import OneLang.One.Ast.Expressions.UnresolvedCallExpression;
import OneLang.One.Ast.Expressions.ElementAccessExpression;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Ast.Expressions.CastExpression;
import OneLang.One.Ast.AstTypes.UnresolvedType;

public class ExpressionParser {
    public Map<String, Operator> operatorMap;
    public String[] operators;
    public Integer prefixPrecedence;
    public IType stringLiteralType;
    public IType numericLiteralType;
    public Reader reader;
    public IExpressionParserHooks hooks;
    public NodeManager nodeManager;
    public ExpressionParserConfig config;
    
    public ExpressionParser(Reader reader, IExpressionParserHooks hooks, NodeManager nodeManager, ExpressionParserConfig config)
    {
        this.reader = reader;
        this.hooks = hooks;
        this.nodeManager = nodeManager;
        this.config = config;
        this.stringLiteralType = null;
        this.numericLiteralType = null;
        if (this.config == null)
            this.config = ExpressionParser.defaultConfig();
        this.reconfigure();
    }
    
    public static ExpressionParserConfig defaultConfig() {
        var config = new ExpressionParserConfig();
        config.unary = new String[] { "++", "--", "!", "not", "+", "-", "~" };
        config.precedenceLevels = new PrecedenceLevel[] { new PrecedenceLevel("assignment", new String[] { "=", "+=", "-=", "*=", "/=", "<<=", ">>=" }, true), new PrecedenceLevel("conditional", new String[] { "?" }, false), new PrecedenceLevel("or", new String[] { "||", "or" }, true), new PrecedenceLevel("and", new String[] { "&&", "and" }, true), new PrecedenceLevel("comparison", new String[] { ">=", "!=", "===", "!==", "==", "<=", ">", "<" }, true), new PrecedenceLevel("sum", new String[] { "+", "-" }, true), new PrecedenceLevel("product", new String[] { "*", "/", "%" }, true), new PrecedenceLevel("bitwise", new String[] { "|", "&", "^" }, true), new PrecedenceLevel("exponent", new String[] { "**" }, true), new PrecedenceLevel("shift", new String[] { "<<", ">>" }, true), new PrecedenceLevel("range", new String[] { "..." }, true), new PrecedenceLevel("in", new String[] { "in" }, true), new PrecedenceLevel("prefix", new String[0], false), new PrecedenceLevel("postfix", new String[] { "++", "--" }, false), new PrecedenceLevel("call", new String[] { "(" }, false), new PrecedenceLevel("propertyAccess", new String[0], false), new PrecedenceLevel("elementAccess", new String[] { "[" }, false) };
        config.rightAssoc = new String[] { "**" };
        config.aliases = Map.of("===", "==", "!==", "!=", "not", "!", "and", "&&", "or", "||");
        config.propertyAccessOps = new String[] { ".", "::" };
        return config;
    }
    
    public void reconfigure() {
        Arrays.stream(this.config.precedenceLevels).filter(x -> Objects.equals(x.name, "propertyAccess")).findFirst().orElse(null).operators = this.config.propertyAccessOps;
        
        this.operatorMap = new LinkedHashMap<String, Operator>();
        
        for (Integer i = 0; i < this.config.precedenceLevels.length; i++) {
            var level = this.config.precedenceLevels[i];
            var precedence = i + 1;
            if (Objects.equals(level.name, "prefix"))
                this.prefixPrecedence = precedence;
            
            if (level.operators == null)
                continue;
            
            for (var opText : level.operators) {
                var op = new Operator(opText, precedence, level.binary, Arrays.stream(this.config.rightAssoc).anyMatch(opText::equals), Objects.equals(level.name, "postfix"));
                
                this.operatorMap.put(opText, op);
            }
        }
        
        this.operators = ArrayHelper.sortBy(this.operatorMap.keySet().toArray(String[]::new), x -> -x.length());
    }
    
    public MapLiteral parseMapLiteral(String keySeparator, String startToken, String endToken) {
        if (!this.reader.readToken(startToken))
            return null;
        
        var items = new ArrayList<MapLiteralItem>();
        do {
            if (this.reader.peekToken(endToken))
                break;
            
            var name = this.reader.readString();
            if (name == null)
                name = this.reader.expectIdentifier("expected string or identifier as map key");
            
            this.reader.expectToken(keySeparator, null);
            var initializer = this.parse(0, true);
            items.add(new MapLiteralItem(name, initializer));
        } while (this.reader.readToken(","));
        
        this.reader.expectToken(endToken, null);
        return new MapLiteral(items.toArray(MapLiteralItem[]::new));
    }
    
    public ArrayLiteral parseArrayLiteral(String startToken, String endToken) {
        if (!this.reader.readToken(startToken))
            return null;
        
        var items = new ArrayList<Expression>();
        if (!this.reader.readToken(endToken)) {
            do {
                var item = this.parse(0, true);
                items.add(item);
            } while (this.reader.readToken(","));
            
            this.reader.expectToken(endToken, null);
        }
        return new ArrayLiteral(items.toArray(Expression[]::new));
    }
    
    public Expression parseLeft(Boolean required) {
        var result = this.hooks != null ? this.hooks.unaryPrehook() : null;
        if (result != null)
            return result;
        
        var unary = this.reader.readAnyOf(this.config.unary);
        if (unary != null) {
            var right = this.parse(this.prefixPrecedence, true);
            return new UnaryExpression(UnaryType.Prefix, unary, right);
        }
        
        var id = this.reader.readIdentifier();
        if (id != null)
            return new Identifier(id);
        
        var num = this.reader.readNumber();
        if (num != null)
            return new NumericLiteral(num);
        
        var str = this.reader.readString();
        if (str != null)
            return new StringLiteral(str);
        
        if (this.reader.readToken("(")) {
            var expr = this.parse(0, true);
            this.reader.expectToken(")", null);
            return new ParenthesizedExpression(expr);
        }
        
        if (required)
            this.reader.fail("unknown (literal / unary) token in expression", -1);
        
        return null;
    }
    
    public Operator parseOperator() {
        for (var opText : this.operators) {
            if (this.reader.peekToken(opText))
                return this.operatorMap.get(opText);
        }
        
        return null;
    }
    
    public Expression[] parseCallArguments() {
        var args = new ArrayList<Expression>();
        
        if (!this.reader.readToken(")")) {
            do {
                var arg = this.parse(0, true);
                args.add(arg);
            } while (this.reader.readToken(","));
            
            this.reader.expectToken(")", null);
        }
        
        return args.toArray(Expression[]::new);
    }
    
    public void addNode(Object node, Integer start) {
        if (this.nodeManager != null)
            this.nodeManager.addNode(node, start);
    }
    
    public Expression parse(Integer precedence, Boolean required) {
        this.reader.skipWhitespace(false);
        var leftStart = this.reader.offset;
        var left = this.parseLeft(required);
        if (left == null)
            return null;
        this.addNode(left, leftStart);
        
        while (true) {
            if (this.hooks != null) {
                var parsed = this.hooks.infixPrehook(left);
                if (parsed != null) {
                    left = parsed;
                    this.addNode(left, leftStart);
                    continue;
                }
            }
            
            var op = this.parseOperator();
            if (op == null || op.precedence <= precedence)
                break;
            this.reader.expectToken(op.text, null);
            var opText = this.config.aliases.containsKey(op.text) ? this.config.aliases.get(op.text) : op.text;
            
            if (op.isBinary) {
                var right = this.parse(op.isRightAssoc ? op.precedence - 1 : op.precedence, true);
                left = new BinaryExpression(left, opText, right);
            }
            else if (op.isPostfix)
                left = new UnaryExpression(UnaryType.Postfix, opText, left);
            else if (Objects.equals(op.text, "?")) {
                var whenTrue = this.parse(0, true);
                this.reader.expectToken(":", null);
                var whenFalse = this.parse(op.precedence - 1, true);
                left = new ConditionalExpression(left, whenTrue, whenFalse);
            }
            else if (Objects.equals(op.text, "(")) {
                var args = this.parseCallArguments();
                left = new UnresolvedCallExpression(left, new IType[0], args);
            }
            else if (Objects.equals(op.text, "[")) {
                var elementExpr = this.parse(0, true);
                this.reader.expectToken("]", null);
                left = new ElementAccessExpression(left, elementExpr);
            }
            else if (Arrays.stream(this.config.propertyAccessOps).anyMatch(op.text::equals)) {
                var prop = this.reader.expectIdentifier("expected identifier as property name");
                left = new PropertyAccessExpression(left, prop);
            }
            else
                this.reader.fail("parsing '" + op.text + "' is not yet implemented", -1);
            
            this.addNode(left, leftStart);
        }
        
        if (left instanceof ParenthesizedExpression && ((ParenthesizedExpression)left).expression instanceof Identifier) {
            var expr = this.parse(0, false);
            if (expr != null)
                return new CastExpression(new UnresolvedType(((Identifier)((ParenthesizedExpression)left).expression).text, new IType[0]), expr);
        }
        
        return left;
    }
}