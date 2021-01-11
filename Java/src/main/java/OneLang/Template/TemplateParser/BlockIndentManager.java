package OneLang.Template.TemplateParser;

import OneLang.Parsers.Common.Reader.Reader;
import OneLang.Template.Nodes.ExpressionNode;
import OneLang.Template.Nodes.ForNode;
import OneLang.Template.Nodes.ITemplateNode;
import OneLang.Template.Nodes.LiteralNode;
import OneLang.Template.Nodes.TemplateBlock;
import OneLang.One.Ast.Expressions.Identifier;
import OneLang.Parsers.TypeScriptParser.TypeScriptParser2;

import OneLang.Template.Nodes.TemplateBlock;
import OneLang.Template.Nodes.LiteralNode;
import io.onelang.std.core.Objects;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class BlockIndentManager {
    public Integer deindentLen;
    public TemplateBlock block;
    public Integer indentLen;
    
    public BlockIndentManager(TemplateBlock block, Integer indentLen)
    {
        this.block = block;
        this.indentLen = indentLen;
        this.deindentLen = -1;
    }
    
    public Integer removePrevIndent() {
        if (this.block.items.size() == 0)
            return 0;
        var lastItem = this.block.items.get(this.block.items.size() - 1);
        
        if (!(lastItem instanceof LiteralNode))
            return 0;
        var lit = ((LiteralNode)lastItem);
        
        for (Integer pos = lit.value.length() - 1; pos >= 0; pos--) {
            if (Objects.equals(lit.value.substring(pos, pos + 1), "\n")) {
                var indent = lit.value.length() - pos - 1;
                lit.value = lit.value.substring(0, pos);
                if (indent < 0) { }
                return indent;
            }
            
            if (!Objects.equals(lit.value.substring(pos, pos + 1), " "))
                break;
        }
        
        return 0;
    }
    
    public LiteralNode deindent(LiteralNode lit) {
        if (this.indentLen == 0)
            return lit;
        // do not deindent root nodes
               
        var lines = lit.value.split("\\r?\\n", -1);
        if (lines.length == 1)
            return lit;
        
        var newLines = new ArrayList<>(List.of(lines[0]));
        for (Integer iLine = 1; iLine < lines.length; iLine++) {
            var line = lines[iLine];
            
            if (this.deindentLen == -1)
                for (Integer i = 0; i < line.length(); i++) {
                    if (!Objects.equals(line.substring(i, i + 1), " ")) {
                        this.deindentLen = i;
                        if (this.deindentLen - this.indentLen < 0) { }
                        break;
                    }
                }
            
            if (this.deindentLen == -1)
                newLines.add(line);
            else {
                var spaceLen = line.length() < this.deindentLen ? line.length() : this.deindentLen;
                for (Integer i = 0; i < spaceLen; i++) {
                    if (!Objects.equals(line.substring(i, i + 1), " "))
                        throw new Error("invalid indent");
                }
                newLines.add(line.substring(this.deindentLen - this.indentLen));
            }
        }
        lit.value = newLines.stream().collect(Collectors.joining("\n"));
        return lit;
    }
}