package OneLang.Template.TemplateParser;

import OneLang.Parsers.Common.Reader.Reader;
import OneLang.Parsers.Common.ExpressionParser.ExpressionParser;
import OneLang.Template.Nodes.ExpressionNode;
import OneLang.Template.Nodes.ForNode;
import OneLang.Template.Nodes.ITemplateNode;
import OneLang.Template.Nodes.LiteralNode;
import OneLang.Template.Nodes.TemplateBlock;
import OneLang.One.Ast.Expressions.Identifier;
import OneLang.Parsers.TypeScriptParser.TypeScriptParser2;

import OneLang.Parsers.Common.Reader.Reader;
import OneLang.Parsers.TypeScriptParser.TypeScriptParser2;
import java.util.Map;
import java.util.LinkedHashMap;
import OneLang.Template.Nodes.TemplateBlock;
import OneLang.Template.Nodes.ITemplateNode;
import java.util.ArrayList;
import OneLang.Template.Nodes.ExpressionNode;
import OneLang.One.Ast.Expressions.Identifier;
import OneLang.Template.Nodes.ForNode;
import io.onelang.std.core.Objects;
import OneLang.Template.Nodes.LiteralNode;

public class TemplateParser {
    public Reader reader;
    public TypeScriptParser2 parser;
    public String template;
    
    public TemplateParser(String template)
    {
        this.template = template;
        this.parser = new TypeScriptParser2(template, null);
        this.parser.allowDollarIds = true;
        this.reader = this.parser.reader;
    }
    
    public Map<String, String> parseAttributes() {
        var result = new LinkedHashMap<String, String>();
        while (this.reader.readToken(",")) {
            var key = this.reader.expectIdentifier(null);
            var value = this.reader.readToken("=") ? this.reader.expectString(null) : null;
            result.put(key, value);
        }
        return result;
    }
    
    public TemplateBlock parseBlock() {
        var items = new ArrayList<ITemplateNode>();
        while (!this.reader.getEof()) {
            if (this.reader.peekExactly("{{/"))
                break;
            if (this.reader.readExactly("${")) {
                var expr = this.parser.parseExpression();
                items.add(new ExpressionNode(expr));
                this.reader.expectToken("}", null);
            }
            else if (this.reader.readExactly("$")) {
                var id = this.reader.expectIdentifier(null);
                items.add(new ExpressionNode(new Identifier(id)));
            }
            else if (this.reader.readExactly("{{")) {
                if (this.reader.readToken("for")) {
                    var varName = this.reader.expectIdentifier(null);
                    this.reader.expectToken("of", null);
                    var itemsExpr = this.parser.parseExpression();
                    var attrs = this.parseAttributes();
                    this.reader.expectToken("}}", null);
                    var body = this.parseBlock();
                    this.reader.expectToken("{{/for}}", null);
                    items.add(new ForNode(varName, itemsExpr, body, attrs.get("joiner")));
                }
                else {
                    var expr = this.parser.parseExpression();
                    items.add(new ExpressionNode(expr));
                    this.reader.expectToken("}}", null);
                }
            }
            else {
                var literal = this.reader.readRegex("([^\\\\]\\\\(\\{\\{|\\$)|\r|\n|(?!\\{\\{|\\$\\{|\\$).)*")[0];
                if (Objects.equals(literal, ""))
                    throw new Error("This should not happen!");
                items.add(new LiteralNode(literal));
            }
        }
        return new TemplateBlock(items.toArray(ITemplateNode[]::new));
    }
    
    public TemplateBlock parse() {
        return this.parseBlock();
    }
}