using One.Ast;
using Parsers.Common;
using Parsers;
using System.Collections.Generic;
using Template;

namespace Template
{
    public class TemplateParser {
        public Reader reader;
        public TypeScriptParser2 parser;
        public string template;
        
        public TemplateParser(string template)
        {
            this.template = template;
            this.parser = new TypeScriptParser2(template);
            this.parser.allowDollarIds = true;
            this.reader = this.parser.reader;
        }
        
        public Dictionary<string, string> parseAttributes()
        {
            var result = new Dictionary<string, string> {};
            while (this.reader.readToken(",")) {
                var key = this.reader.expectIdentifier();
                var value = this.reader.readToken("=") ? this.reader.expectString() : null;
                result.set(key, value);
            }
            return result;
        }
        
        public TemplateBlock parseBlock()
        {
            var items = new List<ITemplateNode>();
            while (!this.reader.eof) {
                if (this.reader.peekExactly("{{/"))
                    break;
                if (this.reader.readExactly("${")) {
                    var expr = this.parser.parseExpression();
                    items.push(new ExpressionNode(expr));
                    this.reader.expectToken("}");
                }
                else if (this.reader.readExactly("$")) {
                    var id = this.reader.expectIdentifier();
                    items.push(new ExpressionNode(new Identifier(id)));
                }
                else if (this.reader.readExactly("{{")) {
                    if (this.reader.readToken("for")) {
                        var varName = this.reader.expectIdentifier();
                        this.reader.expectToken("of");
                        var itemsExpr = this.parser.parseExpression();
                        var attrs = this.parseAttributes();
                        this.reader.expectToken("}}");
                        var body = this.parseBlock();
                        this.reader.expectToken("{{/for}}");
                        items.push(new ForNode(varName, itemsExpr, body, attrs.get("joiner")));
                    }
                    else {
                        var expr = this.parser.parseExpression();
                        items.push(new ExpressionNode(expr));
                        this.reader.expectToken("}}");
                    }
                }
                else {
                    var literal = this.reader.readRegex("([^\\\\]\\\\(\\{\\{|\\$)|\r|\n|(?!\\{\\{|\\$\\{|\\$).)*").get(0);
                    if (literal == "")
                        throw new Error("This should not happen!");
                    items.push(new LiteralNode(literal));
                }
            }
            return new TemplateBlock(items.ToArray());
        }
        
        public TemplateBlock parse()
        {
            return this.parseBlock();
        }
    }
}