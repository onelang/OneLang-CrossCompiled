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
import OneLang.Template.Nodes.TemplateContext;

public class ProjectDependency {
    public String name;
    public String version;
    
    public ProjectDependency(String name, String version)
    {
        this.name = name;
        this.version = version;
    }
}