package OneLang.Generator.ProjectGenerator;

import OneStd.OneFile;
import OneStd.OneYaml;
import OneStd.YamlValue;
import OneStd.OneJObject;
import OneStd.OneJson;
import OneStd.OneJValue;
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

import OneLang.Generator.ProjectGenerator.ITemplateNode;
import java.util.Arrays;
import java.util.stream.Collectors;
import OneLang.Generator.ProjectGenerator.ObjectValue;

public class TemplateBlock implements ITemplateNode {
    public ITemplateNode[] items;
    
    public TemplateBlock(ITemplateNode[] items)
    {
        this.items = items;
    }
    
    public String format(ObjectValue model) {
        return Arrays.stream(Arrays.stream(this.items).map(x -> x.format(model)).toArray(String[]::new)).collect(Collectors.joining(""));
    }
}