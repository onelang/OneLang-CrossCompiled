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
import OneLang.StdLib.PackageManager.ImplementationPackage;

import OneLang.Generator.ProjectGenerator.ProjectDependency;
import OneLang.Generator.ProjectGenerator.OneProjectFile;
import java.util.Arrays;
import OneStd.OneJObject;

public class OneProjectFile {
    public String name;
    public ProjectDependency[] dependencies;
    public String sourceDir;
    public String sourceLang;
    public String outputDir;
    public String[] projectTemplates;
    
    public OneProjectFile(String name, ProjectDependency[] dependencies, String sourceDir, String sourceLang, String outputDir, String[] projectTemplates)
    {
        this.name = name;
        this.dependencies = dependencies;
        this.sourceDir = sourceDir;
        this.sourceLang = sourceLang;
        this.outputDir = outputDir;
        this.projectTemplates = projectTemplates;
    }
    
    public static OneProjectFile fromJson(OneJObject json) {
        return new OneProjectFile(json.get("name").asString(), Arrays.stream(Arrays.stream(json.get("dependencies").getArrayItems()).map(dep -> dep.asObject()).toArray(OneJObject[]::new)).map(dep -> new ProjectDependency(dep.get("name").asString(), dep.get("version").asString())).toArray(ProjectDependency[]::new), json.get("sourceDir").asString(), json.get("sourceLang").asString(), json.get("outputDir").asString(), Arrays.stream(json.get("projectTemplates").getArrayItems()).map(x -> x.asString()).toArray(String[]::new));
    }
}