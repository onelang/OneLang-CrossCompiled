using One.Ast;
using Parsers.Common;
using Parsers;
using System.Collections.Generic;
using Template;

namespace Template
{
    public class BlockIndentManager
    {
        public int deindentLen;
        public TemplateBlock block;
        public int indentLen;
        
        public BlockIndentManager(TemplateBlock block, int indentLen)
        {
            this.block = block;
            this.indentLen = indentLen;
            this.deindentLen = -1;
        }
        
        public int removePrevIndent()
        {
            if (this.block.items.length() == 0)
                return 0;
            var lastItem = this.block.items.get(this.block.items.length() - 1);
            
            if (!(lastItem is LiteralNode))
                return 0;
            var lit = ((LiteralNode)lastItem);
            
            for (int pos = lit.value.length() - 1; pos >= 0; pos--) {
                if (lit.value.get(pos) == "\n") {
                    var indent = lit.value.length() - pos - 1;
                    lit.value = lit.value.substring(0, pos);
                    if (indent < 0) { }
                    return indent;
                }
                
                if (lit.value.get(pos) != " ")
                    break;
            }
            
            return 0;
        }
        
        public LiteralNode deindent(LiteralNode lit)
        {
            if (this.indentLen == 0)
                return lit;
            // do not deindent root nodes
                   
            var lines = lit.value.split(new RegExp("\\r?\\n"));
            if (lines.length() == 1)
                return lit;
            
            var newLines = new List<string> { lines.get(0) };
            for (int iLine = 1; iLine < lines.length(); iLine++) {
                var line = lines.get(iLine);
                
                if (this.deindentLen == -1)
                    for (int i = 0; i < line.length(); i++) {
                        if (line.get(i) != " ") {
                            this.deindentLen = i;
                            if (this.deindentLen - this.indentLen < 0) { }
                            break;
                        }
                    }
                
                if (this.deindentLen == -1)
                    newLines.push(line);
                else {
                    var spaceLen = line.length() < this.deindentLen ? line.length() : this.deindentLen;
                    for (int i = 0; i < spaceLen; i++) {
                        if (line.get(i) != " ")
                            throw new Error("invalid indent");
                    }
                    newLines.push(line.substr(this.deindentLen - this.indentLen));
                }
            }
            lit.value = newLines.join("\n");
            return lit;
        }
    }
    
    public class TemplateParser
    {
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
        
        public TemplateBlock parseBlock(int indentLen = 0)
        {
            var block = new TemplateBlock(new ITemplateNode[0]);
            var indentMan = new BlockIndentManager(block, indentLen);
            while (!this.reader.eof) {
                if (this.reader.peekExactly("{{/"))
                    break;
                if (this.reader.readExactly("${")) {
                    var expr = this.parser.parseExpression();
                    block.items.push(new ExpressionNode(expr));
                    this.reader.expectToken("}");
                }
                else if (this.reader.readExactly("$")) {
                    var id = this.reader.expectIdentifier();
                    block.items.push(new ExpressionNode(new Identifier(id)));
                }
                else if (this.reader.readExactly("{{")) {
                    var blockIndentLen = indentMan.removePrevIndent();
                    
                    if (this.reader.readToken("for")) {
                        var varName = this.reader.expectIdentifier();
                        this.reader.expectToken("of");
                        var itemsExpr = this.parser.parseExpression();
                        var attrs = this.parseAttributes();
                        this.reader.expectToken("}}");
                        var body = this.parseBlock(blockIndentLen);
                        this.reader.expectToken("{{/for}}");
                        block.items.push(new ForNode(varName, itemsExpr, body, attrs.get("joiner")));
                    }
                    else {
                        var expr = this.parser.parseExpression();
                        block.items.push(new ExpressionNode(expr));
                        this.reader.expectToken("}}");
                    }
                }
                else {
                    var literal = this.reader.readRegex("([^\\\\]\\\\(\\{\\{|\\$)|\r|\n|(?!\\{\\{|\\$\\{|\\$).)*").get(0);
                    if (literal == "")
                        throw new Error("This should not happen!");
                    block.items.push(indentMan.deindent(new LiteralNode(literal)));
                }
            }
            
            if (indentLen != 0)
                indentMan.removePrevIndent();
            
            return block;
        }
        
        public TemplateBlock parse()
        {
            return this.parseBlock();
        }
    }
}