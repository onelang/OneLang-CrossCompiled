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

import OneLang.Generator.ProjectGenerator.ProjectTemplateMeta;
import java.util.Arrays;
import OneLang.Generator.ProjectGenerator.TemplateFile;
import OneLang.Generator.ProjectGenerator.ObjectValue;

public class ProjectTemplate {
    public ProjectTemplateMeta meta;
    public String[] srcFiles;
    public String templateDir;
    
    public ProjectTemplate(String templateDir)
    {
        this.templateDir = templateDir;
        this.meta = ProjectTemplateMeta.fromYaml(OneYaml.load(OneFile.readText(templateDir + "/index.yaml")));
        this.srcFiles = OneFile.listFiles(templateDir + "/src", true);
    }
    
    public void generate(String dstDir, ObjectValue model) {
        for (var fn : this.srcFiles) {
            var srcFn = this.templateDir + "/src/" + fn;
            var dstFn = dstDir + "/" + fn;
            if (Arrays.stream(this.meta.templateFiles).anyMatch(fn::equals)) {
                var tmplFile = new TemplateFile(OneFile.readText(srcFn));
                var dstFile = tmplFile.format(model);
                OneFile.writeText(dstFn, dstFile);
            }
            else
                OneFile.copy(srcFn, dstFn);
        }
    }
}