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

import OneLang.Generator.ProjectGenerator.OneProjectFile;
import OneLang.Generator.IGenerator.IGenerator;
import OneLang.Generator.JavaGenerator.JavaGenerator;
import OneLang.Generator.CsharpGenerator.CsharpGenerator;
import OneLang.Generator.PythonGenerator.PythonGenerator;
import OneLang.Generator.PhpGenerator.PhpGenerator;
import OneLang.Generator.ProjectGenerator.ProjectTemplate;
import OneStd.Objects;
import java.util.Arrays;
import OneLang.One.Ast.Types.SourceFile;
import OneStd.console;
import OneLang.StdLib.PackageManager.ImplementationPackage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import OneLang.StdLib.PackageManager.ImplPkgNativeDependency;
import OneLang.Generator.ProjectGenerator.ObjectValue;
import java.util.Map;
import OneLang.Generator.ProjectGenerator.IVMValue;
import OneLang.Generator.ProjectGenerator.ArrayValue;
import OneLang.Generator.ProjectGenerator.StringValue;

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
        var generators = new IGenerator[] { ((IGenerator)new JavaGenerator()), ((IGenerator)new CsharpGenerator()), ((IGenerator)new PythonGenerator()), ((IGenerator)new PhpGenerator()) };
        for (var tmplName : this.projectFile.projectTemplates) {
            var compiler = CompilerHelper.initProject(this.projectFile.name, this.srcDir, this.projectFile.sourceLang, null);
            compiler.processWorkspace();
            
            var projTemplate = new ProjectTemplate(this.baseDir + "/project-templates/" + tmplName);
            var langId = projTemplate.meta.language;
            var generator = Arrays.stream(generators).filter(x -> Objects.equals(x.getLangName().toLowerCase(), langId)).findFirst().orElse(null);
            var langName = generator.getLangName();
            
            for (var trans : generator.getTransforms())
                trans.visitFiles(compiler.projectPkg.files.values().toArray(SourceFile[]::new));
            
            var outDir = this.outDir + "/" + langName;
            console.log("Generating " + langName + " code...");
            var files = generator.generate(compiler.projectPkg);
            for (var file : files)
                OneFile.writeText(outDir + "/" + projTemplate.meta.destinationDir != null ? projTemplate.meta.destinationDir : "" + "/" + file.path, file.content);
            
            var oneDeps = new ArrayList<ImplementationPackage>();
            var nativeDeps = new LinkedHashMap<String, String>();
            for (var dep : this.projectFile.dependencies) {
                var impl = compiler.pacMan.implementationPkgs.stream().filter(x -> Objects.equals(x.content.id.name, dep.name)).findFirst().orElse(null);
                oneDeps.add(impl);
                var langData = impl.implementationYaml.languages.get(langId);
                if (langData == null)
                    continue;
                
                for (var natDep : langData.nativeDependencies != null ? langData.nativeDependencies : new ImplPkgNativeDependency[0])
                    nativeDeps.put(natDep.name, natDep.version);
                
                if (langData.nativeSrcDir != null) {
                    if (projTemplate.meta.packageDir == null)
                        throw new Error("Package directory is empty in project template!");
                    var srcDir = langData.nativeSrcDir + (langData.nativeSrcDir.endsWith("/") ? "" : "/");
                    var dstDir = outDir + "/" + projTemplate.meta.packageDir + "/" + impl.content.id.name;
                    var depFiles = Arrays.stream(Arrays.stream(impl.content.files.keySet().toArray(String[]::new)).filter(x -> x.startsWith(srcDir)).toArray(String[]::new)).map(x -> x.substring(srcDir.length())).toArray(String[]::new);
                    for (var fn : depFiles)
                        OneFile.writeText(dstDir + "/" + fn, impl.content.files.get(srcDir + fn));
                }
            }
            
            var model = new ObjectValue(Map.of("dependencies", ((IVMValue)new ArrayValue(Arrays.stream(nativeDeps.keySet().toArray(String[]::new)).map(name -> new ObjectValue(Map.of("name", ((IVMValue)new StringValue(name)), "version", ((IVMValue)new StringValue(nativeDeps.get(name)))))).toArray(ObjectValue[]::new))), "onepackages", ((IVMValue)new ArrayValue(oneDeps.stream().map(dep -> new ObjectValue(Map.of("vendor", ((IVMValue)new StringValue(dep.implementationYaml.vendor)), "id", ((IVMValue)new StringValue(dep.implementationYaml.name))))).toArray(ObjectValue[]::new)))));
            projTemplate.generate(outDir, model);
        }
    }
}