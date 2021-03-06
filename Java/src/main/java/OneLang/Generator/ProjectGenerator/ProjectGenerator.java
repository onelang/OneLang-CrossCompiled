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

import OneLang.Generator.ProjectGenerator.OneProjectFile;
import OneLang.Generator.IGenerator.IGenerator;
import OneLang.Generator.JavaGenerator.JavaGenerator;
import OneLang.Generator.CsharpGenerator.CsharpGenerator;
import OneLang.Generator.PythonGenerator.PythonGenerator;
import OneLang.Generator.PhpGenerator.PhpGenerator;
import OneLang.Generator.ProjectGenerator.ProjectTemplate;
import java.util.Arrays;
import io.onelang.std.core.Objects;
import OneLang.One.Ast.Types.SourceFile;
import OneLang.StdLib.PackageManager.ImplementationPackage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import OneLang.StdLib.PackageManager.ImplPkgNativeDependency;
import OneLang.Generator.TemplateFileGeneratorPlugin.TemplateFileGeneratorPlugin;
import OneLang.VM.Values.ObjectValue;
import java.util.Map;
import OneLang.VM.Values.IVMValue;
import OneLang.VM.Values.ArrayValue;
import OneLang.VM.Values.StringValue;

public class ProjectGenerator {
    public OneProjectFile projectFile;
    public String srcDir;
    public String outDir;
    public String baseDir;
    public String projDir;
    
    public ProjectGenerator(String baseDir, String projDir)
    {
        this.baseDir = baseDir;
        this.projDir = projDir;
        this.projectFile = null;
        this.projectFile = OneProjectFile.fromJson(OneJson.parse(OneFile.readText(projDir + "/one.json")).asObject());
        this.srcDir = this.projDir + "/" + this.projectFile.sourceDir;
        this.outDir = this.projDir + "/" + this.projectFile.outputDir;
    }
    
    public void generate() {
        // copy native source codes from one project
        var nativeSrcDir = this.projDir + "/" + this.projectFile.nativeSourceDir;
        for (var fn : OneFile.listFiles(nativeSrcDir, true))
            OneFile.copy(nativeSrcDir + "/" + fn, this.outDir + "/" + fn);
        
        var generators = new IGenerator[] { ((IGenerator)new JavaGenerator()), ((IGenerator)new CsharpGenerator()), ((IGenerator)new PythonGenerator()), ((IGenerator)new PhpGenerator()) };
        for (var tmplName : this.projectFile.projectTemplates) {
            var compiler = CompilerHelper.initProject(this.projectFile.name, this.srcDir, this.projectFile.sourceLang, null);
            compiler.processWorkspace();
            
            var projTemplate = new ProjectTemplate(this.baseDir + "/project-templates/" + tmplName);
            var langId = projTemplate.meta.language;
            var generator = Arrays.stream(generators).filter(x -> Objects.equals(x.getLangName().toLowerCase(), langId)).findFirst().orElse(null);
            var langName = generator.getLangName();
            var outDir = this.outDir + "/" + langName;
            
            for (var trans : generator.getTransforms())
                trans.visitFiles(compiler.projectPkg.files.values().toArray(SourceFile[]::new));
                
            // copy implementation native sources
            var oneDeps = new ArrayList<ImplementationPackage>();
            var nativeDeps = new LinkedHashMap<String, String>();
            for (var dep : this.projectFile.dependencies) {
                var impl = compiler.pacMan.implementationPkgs.stream().filter(x -> Objects.equals(x.content.id.name, dep.name)).findFirst().orElse(null);
                oneDeps.add(impl);
                var langData = impl.implementationYaml.languages.get(langId);
                if (langData == null)
                    continue;
                
                for (var natDep : (langData.nativeDependencies != null ? (langData.nativeDependencies) : (new ImplPkgNativeDependency[0])))
                    nativeDeps.put(natDep.name, natDep.version);
                
                if (langData.nativeSrcDir != null) {
                    if (projTemplate.meta.packageDir == null)
                        throw new Error("Package directory is empty in project template!");
                    var srcDir = langData.nativeSrcDir + (langData.nativeSrcDir.endsWith("/") ? "" : "/");
                    var dstDir = outDir + "/" + projTemplate.meta.packageDir + "/" + (langData.packageDir != null ? (langData.packageDir) : (impl.content.id.name));
                    var depFiles = Arrays.stream(Arrays.stream(impl.content.files.keySet().toArray(String[]::new)).filter(x -> x.startsWith(srcDir)).toArray(String[]::new)).map(x -> x.substring(srcDir.length())).toArray(String[]::new);
                    for (var fn : depFiles)
                        OneFile.writeText(dstDir + "/" + fn, impl.content.files.get(srcDir + fn));
                }
                
                if (langData.generatorPlugins != null)
                    for (var genPlugFn : langData.generatorPlugins)
                        generator.addPlugin(new TemplateFileGeneratorPlugin(generator, impl.content.files.get(genPlugFn)));
            }
            
            // generate cross compiled source code
            System.out.println("Generating " + langName + " code...");
            var files = generator.generate(compiler.projectPkg);
            for (var file : files)
                OneFile.writeText(outDir + "/" + (projTemplate.meta.destinationDir != null ? (projTemplate.meta.destinationDir) : ("")) + "/" + file.path, file.content);
            
            // generate files from project template
            var model = new ObjectValue(new LinkedHashMap<>(Map.of("dependencies", ((IVMValue)new ArrayValue(Arrays.stream(nativeDeps.keySet().toArray(String[]::new)).map(name -> new ObjectValue(new LinkedHashMap<>(Map.of("name", ((IVMValue)new StringValue(name)), "version", ((IVMValue)new StringValue(nativeDeps.get(name))))))).toArray(ObjectValue[]::new))), "onepackages", ((IVMValue)new ArrayValue(oneDeps.stream().map(dep -> new ObjectValue(new LinkedHashMap<>(Map.of("vendor", ((IVMValue)new StringValue(dep.implementationYaml.vendor)), "id", ((IVMValue)new StringValue(dep.implementationYaml.name)))))).toArray(ObjectValue[]::new))))));
            projTemplate.generate(outDir, model);
        }
    }
}