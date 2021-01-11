package OneLang.Template.TemplateParser;

import OneLang.Parsers.Common.Reader.Reader;
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
import OneLang.Template.TemplateParser.BlockIndentManager;
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
    
    public TemplateBlock parseBlock(Integer indentLen) {
        var block = new TemplateBlock(new ITemplateNode[0]);
        var indentMan = new BlockIndentManager(block, indentLen);
        while (!this.reader.getEof()) {
            if (this.reader.peekExactly("{{/"))
                break;
            if (this.reader.readExactly("${")) {
                var expr = this.parser.parseExpression();
                block.items.add(new ExpressionNode(expr));
                this.reader.expectToken("}", null);
            }
            else if (this.reader.readExactly("$")) {
                var id = this.reader.expectIdentifier(null);
                block.items.add(new ExpressionNode(new Identifier(id)));
            }
            else if (this.reader.readExactly("{{")) {
                var blockIndentLen = indentMan.removePrevIndent();
                
                if (this.reader.readToken("for")) {
                    var varName = this.reader.expectIdentifier(null);
                    this.reader.expectToken("of", null);
                    var itemsExpr = this.parser.parseExpression();
                    var attrs = this.parseAttributes();
                    this.reader.expectToken("}}", null);
                    var body = this.parseBlock(blockIndentLen);
                    this.reader.expectToken("{{/for}}", null);
                    block.items.add(new ForNode(varName, itemsExpr, body, attrs.get("joiner")));
                }
                else {
                    var expr = this.parser.parseExpression();
                    block.items.add(new ExpressionNode(expr));
                    this.reader.expectToken("}}", null);
                }
            }
            else {
                var literal = this.reader.readRegex("([^\\\\]\\\\(\\{\\{|\\$)|\r|\n|(?!\\{\\{|\\$\\{|\\$).)*")[0];
                if (Objects.equals(literal, ""))
                    throw new Error("This should not happen!");
                block.items.add(indentMan.deindent(new LiteralNode(literal)));
            }
        }
        
        if (indentLen != 0)
            indentMan.removePrevIndent();
        
        return block;
    }
    
    public TemplateBlock parse() {
        return this.parseBlock(0);
    }
}