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

import OneLang.Generator.ProjectGenerator.ProjectTemplateMeta;
import io.onelang.std.yaml.YamlValue;

public class ProjectTemplateMeta {
    public String language;
    public String destinationDir;
    public String packageDir;
    public String[] templateFiles;
    
    public ProjectTemplateMeta(String language, String destinationDir, String packageDir, String[] templateFiles)
    {
        this.language = language;
        this.destinationDir = destinationDir;
        this.packageDir = packageDir;
        this.templateFiles = templateFiles;
    }
    
    public static ProjectTemplateMeta fromYaml(YamlValue obj) {
        return new ProjectTemplateMeta(obj.str("language"), obj.str("destination-dir"), obj.str("package-dir"), obj.strArr("template-files"));
    }
}