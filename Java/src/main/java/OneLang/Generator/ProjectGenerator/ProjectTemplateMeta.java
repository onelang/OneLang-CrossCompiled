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
import OneStd.YamlValue;

public class ProjectTemplateMeta {
    public String language;
    public String destionationDir;
    public String[] templateFiles;
    
    public ProjectTemplateMeta(String language, String destionationDir, String[] templateFiles)
    {
        this.language = language;
        this.destionationDir = destionationDir;
        this.templateFiles = templateFiles;
    }
    
    public static ProjectTemplateMeta fromYaml(YamlValue obj) {
        return new ProjectTemplateMeta(obj.str("language"), obj.str("destination-dir"), obj.strArr("template-files"));
    }
}