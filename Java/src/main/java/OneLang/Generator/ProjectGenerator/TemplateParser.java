package OneLang.Generator.ProjectGenerator;

import io.onelang.std.file.OneFile;
import io.onelang.std.yaml.OneYaml;
import io.onelang.std.yaml.YamlValue;
import io.onelang.std.json.OneJObject;
import io.onelang.std.json.OneJson;
import io.onelang.std.json.OneJValue;
import OneLang.Parsers.Common.Reader.Reader;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.Identifier;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Compiler.Compiler;
import OneLang.Generator.IGenerator.IGenerator;
import OneLang.Parsers.Common.ExpressionParser.ExpressionParser;
import OneLang.Utils.TSOverviewGenerator.TSOverviewGenerator;
import OneLang.Generator.JavaGenerator.JavaGenerator;
import OneLang.Generator.CsharpGenerator.CsharpGenerator;
import OneLang.Generator.PythonGenerator.PythonGenerator;
import OneLang.Generator.PhpGenerator.PhpGenerator;
import OneLang.One.CompilerHelper.CompilerHelper;
import OneLang.StdLib.PackageManager.ImplementationPackage;

import OneLang.Parsers.Common.Reader.Reader;
import OneLang.Parsers.Common.ExpressionParser.ExpressionParser;
import java.util.Map;
import java.util.LinkedHashMap;
import OneLang.Generator.ProjectGenerator.TemplateBlock;
import OneLang.Generator.ProjectGenerator.ITemplateNode;
import java.util.ArrayList;
import OneLang.Generator.ProjectGenerator.ForNode;
import OneLang.Generator.ProjectGenerator.ExpressionNode;
import io.onelang.std.core.Objects;
import OneLang.Generator.ProjectGenerator.LiteralNode;

public class TemplateParser {
    public Reader reader;
    public ExpressionParser exprParser;
    public String template;
    
    public TemplateParser(String template)
    {
        this.template = template;
        this.reader = new Reader(template);
        this.exprParser = new ExpressionParser(this.reader, null, null, null);
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
            if (this.reader.peekToken("{{/"))
                break;
            if (this.reader.readToken("{{")) {
                if (this.reader.readToken("for")) {
                    var varName = this.reader.readIdentifier();
                    this.reader.expectToken("of", null);
                    var itemsExpr = this.exprParser.parse(0, true);
                    var attrs = this.parseAttributes();
                    this.reader.expectToken("}}", null);
                    var body = this.parseBlock();
                    this.reader.expectToken("{{/for}}", null);
                    items.add(new ForNode(varName, itemsExpr, body, attrs.get("joiner")));
                }
                else {
                    var expr = this.exprParser.parse(0, true);
                    items.add(new ExpressionNode(expr));
                    this.reader.expectToken("}}", null);
                }
            }
            else {
                var literal = this.reader.readUntil("{{", true);
                if (literal.endsWith("\\") && !literal.endsWith("\\\\"))
                    literal = literal.substring(0, literal.length() - 1) + "{{";
                if (!Objects.equals(literal, ""))
                    items.add(new LiteralNode(literal));
            }
        }
        return new TemplateBlock(items.toArray(ITemplateNode[]::new));
    }
    
    public TemplateBlock parse() {
        return this.parseBlock();
    }
}