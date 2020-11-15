using One.Ast;
using Parsers.Common;
using System.Collections.Generic;
using Utils;

namespace Parsers.Common
{
    public interface IExpressionParserHooks {
        Expression unaryPrehook();
        
        Expression infixPrehook(Expression left);
    }
    
    public class Operator {
        public string text;
        public int precedence;
        public bool isBinary;
        public bool isRightAssoc;
        public bool isPostfix;
        
        public Operator(string text, int precedence, bool isBinary, bool isRightAssoc, bool isPostfix)
        {
            this.text = text;
            this.precedence = precedence;
            this.isBinary = isBinary;
            this.isRightAssoc = isRightAssoc;
            this.isPostfix = isPostfix;
        }
    }
    
    public class PrecedenceLevel {
        public string name;
        public string[] operators;
        public bool binary;
        
        public PrecedenceLevel(string name, string[] operators, bool binary)
        {
            this.name = name;
            this.operators = operators;
            this.binary = binary;
        }
    }
    
    public class ExpressionParserConfig {
        public string[] unary;
        public PrecedenceLevel[] precedenceLevels;
        public string[] rightAssoc;
        public Dictionary<string, string> aliases;
        public string[] propertyAccessOps;
    }
    
    public class ExpressionParser {
        public Dictionary<string, Operator> operatorMap;
        public string[] operators;
        public int prefixPrecedence;
        public IType stringLiteralType;
        public IType numericLiteralType;
        public Reader reader;
        public IExpressionParserHooks hooks;
        public NodeManager nodeManager;
        public ExpressionParserConfig config;
        
        public ExpressionParser(Reader reader, IExpressionParserHooks hooks = null, NodeManager nodeManager = null, ExpressionParserConfig config = null)
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
        
        public static ExpressionParserConfig defaultConfig()
        {
            var config = new ExpressionParserConfig();
            config.unary = new string[] { "++", "--", "!", "not", "+", "-", "~" };
            config.precedenceLevels = new PrecedenceLevel[] { new PrecedenceLevel("assignment", new string[] { "=", "+=", "-=", "*=", "/=", "<<=", ">>=" }, true), new PrecedenceLevel("conditional", new string[] { "?" }, false), new PrecedenceLevel("or", new string[] { "||", "or" }, true), new PrecedenceLevel("and", new string[] { "&&", "and" }, true), new PrecedenceLevel("comparison", new string[] { ">=", "!=", "===", "!==", "==", "<=", ">", "<" }, true), new PrecedenceLevel("sum", new string[] { "+", "-" }, true), new PrecedenceLevel("product", new string[] { "*", "/", "%" }, true), new PrecedenceLevel("bitwise", new string[] { "|", "&", "^" }, true), new PrecedenceLevel("exponent", new string[] { "**" }, true), new PrecedenceLevel("shift", new string[] { "<<", ">>" }, true), new PrecedenceLevel("range", new string[] { "..." }, true), new PrecedenceLevel("in", new string[] { "in" }, true), new PrecedenceLevel("prefix", new string[0], false), new PrecedenceLevel("postfix", new string[] { "++", "--" }, false), new PrecedenceLevel("call", new string[] { "(" }, false), new PrecedenceLevel("propertyAccess", new string[0], false), new PrecedenceLevel("elementAccess", new string[] { "[" }, false) };
            config.rightAssoc = new string[] { "**" };
            config.aliases = new Dictionary<string, string> {
                ["==="] = "==",
                ["!=="] = "!=",
                ["not"] = "!",
                ["and"] = "&&",
                ["or"] = "||"
            };
            config.propertyAccessOps = new string[] { ".", "::" };
            return config;
        }
        
        public void reconfigure()
        {
            this.config.precedenceLevels.find(x => x.name == "propertyAccess").operators = this.config.propertyAccessOps;
            
            this.operatorMap = new Dictionary<string, Operator> {};
            
            for (int i = 0; i < this.config.precedenceLevels.length(); i++) {
                var level = this.config.precedenceLevels.get(i);
                var precedence = i + 1;
                if (level.name == "prefix")
                    this.prefixPrecedence = precedence;
                
                if (level.operators == null)
                    continue;
                
                foreach (var opText in level.operators) {
                    var op = new Operator(opText, precedence, level.binary, this.config.rightAssoc.includes(opText), level.name == "postfix");
                    
                    this.operatorMap.set(opText, op);
                }
            }
            
            this.operators = ArrayHelper.sortBy(Object.keys(this.operatorMap), x => -x.length());
        }
        
        public MapLiteral parseMapLiteral(string keySeparator = ":", string startToken = "{", string endToken = "}")
        {
            if (!this.reader.readToken(startToken))
                return null;
            
            var items = new List<MapLiteralItem>();
            do {
                if (this.reader.peekToken(endToken))
                    break;
                
                var name = this.reader.readString();
                if (name == null)
                    name = this.reader.expectIdentifier("expected string or identifier as map key");
                
                this.reader.expectToken(keySeparator);
                var initializer = this.parse();
                items.push(new MapLiteralItem(name, initializer));
            } while (this.reader.readToken(","));
            
            this.reader.expectToken(endToken);
            return new MapLiteral(items.ToArray());
        }
        
        public ArrayLiteral parseArrayLiteral(string startToken = "[", string endToken = "]")
        {
            if (!this.reader.readToken(startToken))
                return null;
            
            var items = new List<Expression>();
            if (!this.reader.readToken(endToken)) {
                do {
                    var item = this.parse();
                    items.push(item);
                } while (this.reader.readToken(","));
                
                this.reader.expectToken(endToken);
            }
            return new ArrayLiteral(items.ToArray());
        }
        
        public Expression parseLeft(bool required = true)
        {
            var result = this.hooks != null ? this.hooks.unaryPrehook() : null;
            if (result != null)
                return result;
            
            var unary = this.reader.readAnyOf(this.config.unary);
            if (unary != null) {
                var right = this.parse(this.prefixPrecedence);
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
                var expr = this.parse();
                this.reader.expectToken(")");
                return new ParenthesizedExpression(expr);
            }
            
            if (required)
                this.reader.fail($"unknown (literal / unary) token in expression");
            
            return null;
        }
        
        public Operator parseOperator()
        {
            foreach (var opText in this.operators) {
                if (this.reader.peekToken(opText))
                    return this.operatorMap.get(opText);
            }
            
            return null;
        }
        
        public Expression[] parseCallArguments()
        {
            var args = new List<Expression>();
            
            if (!this.reader.readToken(")")) {
                do {
                    var arg = this.parse();
                    args.push(arg);
                } while (this.reader.readToken(","));
                
                this.reader.expectToken(")");
            }
            
            return args.ToArray();
        }
        
        public void addNode(object node, int start)
        {
            if (this.nodeManager != null)
                this.nodeManager.addNode(node, start);
        }
        
        public Expression parse(int precedence = 0, bool required = true)
        {
            this.reader.skipWhitespace();
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
                this.reader.expectToken(op.text);
                var opText = this.config.aliases.hasKey(op.text) ? this.config.aliases.get(op.text) : op.text;
                
                if (op.isBinary) {
                    var right = this.parse(op.isRightAssoc ? op.precedence - 1 : op.precedence);
                    left = new BinaryExpression(left, opText, right);
                }
                else if (op.isPostfix)
                    left = new UnaryExpression(UnaryType.Postfix, opText, left);
                else if (op.text == "?") {
                    var whenTrue = this.parse();
                    this.reader.expectToken(":");
                    var whenFalse = this.parse(op.precedence - 1);
                    left = new ConditionalExpression(left, whenTrue, whenFalse);
                }
                else if (op.text == "(") {
                    var args = this.parseCallArguments();
                    left = new UnresolvedCallExpression(left, new IType[0], args);
                }
                else if (op.text == "[") {
                    var elementExpr = this.parse();
                    this.reader.expectToken("]");
                    left = new ElementAccessExpression(left, elementExpr);
                }
                else if (this.config.propertyAccessOps.includes(op.text)) {
                    var prop = this.reader.expectIdentifier("expected identifier as property name");
                    left = new PropertyAccessExpression(left, prop);
                }
                else
                    this.reader.fail($"parsing '{op.text}' is not yet implemented");
                
                this.addNode(left, leftStart);
            }
            
            if (left is ParenthesizedExpression parExpr && parExpr.expression is Identifier ident) {
                var expr = this.parse(0, false);
                if (expr != null)
                    return new CastExpression(new UnresolvedType(ident.text, new IType[0]), expr);
            }
            
            return left;
        }
    }
}