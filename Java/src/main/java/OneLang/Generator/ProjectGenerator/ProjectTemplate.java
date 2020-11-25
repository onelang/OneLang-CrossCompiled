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

import OneLang.Generator.ProjectGenerator.ProjectTemplateMeta;
import java.util.Arrays;
import OneLang.Template.TemplateParser.TemplateParser;
import OneLang.VM.ExprVM.VMContext;
import OneLang.VM.Values.ObjectValue;

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
                var tmpl = new TemplateParser(OneFile.readText(srcFn)).parse();
                var dstFile = tmpl.format(new VMContext(model, null));
                OneFile.writeText(dstFn, dstFile);
            }
            else
                OneFile.copy(srcFn, dstFn);
        }
    }
}