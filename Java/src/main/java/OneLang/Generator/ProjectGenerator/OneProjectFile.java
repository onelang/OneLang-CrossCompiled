package OneLang.Generator.ProjectGenerator;

import io.onelang.std.file.OneFile;
import io.onelang.std.yaml.OneYaml;
import io.onelang.std.yaml.YamlValue;
import io.onelang.std.json.OneJObject;
import io.onelang.std.json.OneJson;
import OneLang.Generator.IGenerator.IGenerator;
import OneLang.Generator.JavaGenerator.JavaGenerator;
import OneLang.Generator.CsharpGenerator.CsharpGenerator;
import OneLang.Generator.PythonGenerator.PythonGenerator;
import OneLang.Generator.PhpGenerator.PhpGenerator;
import OneLang.One.CompilerHelper.CompilerHelper;
import OneLang.StdLib.PackageManager.ImplementationPackage;
import OneLang.VM.Values.ArrayValue;
import OneLang.VM.Values.IVMValue;
import OneLang.VM.Values.ObjectValue;
import OneLang.VM.Values.StringValue;
import OneLang.Template.TemplateParser.TemplateParser;
import OneLang.Generator.TemplateFileGeneratorPlugin.TemplateFileGeneratorPlugin;
import OneLang.VM.ExprVM.VMContext;

import OneLang.Generator.ProjectGenerator.ProjectDependency;
import OneLang.Generator.ProjectGenerator.OneProjectFile;
import java.util.Arrays;
import io.onelang.std.json.OneJObject;

public class OneProjectFile {
    public String name;
    public ProjectDependency[] dependencies;
    public String sourceDir;
    public String sourceLang;
    public String nativeSourceDir;
    public String outputDir;
    public String[] projectTemplates;
    
    public OneProjectFile(String name, ProjectDependency[] dependencies, String sourceDir, String sourceLang, String nativeSourceDir, String outputDir, String[] projectTemplates)
    {
        this.name = name;
        this.dependencies = dependencies;
        this.sourceDir = sourceDir;
        this.sourceLang = sourceLang;
        this.nativeSourceDir = nativeSourceDir;
        this.outputDir = outputDir;
        this.projectTemplates = projectTemplates;
    }
    
    public static OneProjectFile fromJson(OneJObject json) {
        return new OneProjectFile(json.get("name").asString(), Arrays.stream(Arrays.stream(json.get("dependencies").getArrayItems()).map(dep -> dep.asObject()).toArray(OneJObject[]::new)).map(dep -> new ProjectDependency(dep.get("name").asString(), dep.get("version").asString())).toArray(ProjectDependency[]::new), json.get("sourceDir").asString(), json.get("sourceLang").asString(), json.get("nativeSourceDir").asString(), json.get("outputDir").asString(), Arrays.stream(json.get("projectTemplates").getArrayItems()).map(x -> x.asString()).toArray(String[]::new));
    }
}