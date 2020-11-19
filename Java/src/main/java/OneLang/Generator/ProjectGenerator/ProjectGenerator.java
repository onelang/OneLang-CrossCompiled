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
import java.util.LinkedHashMap;
import OneLang.StdLib.PackageManager.ImplPkgNativeDependency;
import java.util.LinkedHashSet;
import OneLang.One.Ast.Types.Import;
import OneStd.RegExp;
import java.util.regex.Pattern;
import OneLang.Generator.ProjectGenerator.ObjectValue;
import java.util.Map;
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
            
            console.log("Generating " + langName + " code...");
            var files = generator.generate(compiler.projectPkg);
            for (var file : files)
                OneFile.writeText(this.outDir + "/" + langName + "/" + projTemplate.meta.destionationDir != null ? projTemplate.meta.destionationDir : "" + "/" + file.path, file.content);
            
            var nativeDeps = new LinkedHashMap<String, String>();
            for (var dep : this.projectFile.dependencies) {
                var impl = compiler.pacMan.implementationPkgs.stream().filter(x -> Objects.equals(x.content.id.name, dep.name)).findFirst().orElse(null);
                var langData = Arrays.stream(impl.implementationYaml.languages).filter(x -> Objects.equals(x.id, langId)).findFirst().orElse(null);
                if (langData == null)
                    continue;
                for (var natDep : langData.nativeDependencies != null ? langData.nativeDependencies : new ImplPkgNativeDependency[0])
                    nativeDeps.put(natDep.name, natDep.version);
            }
            
            var oneDeps = new LinkedHashSet<String>();
            oneDeps.add("OneCore");
            for (var file : compiler.projectPkg.files.values().toArray(SourceFile[]::new))
                for (var imp : Arrays.stream(file.imports).filter(x -> !Objects.equals(x.exportScope.packageName, compiler.projectPkg.name)).toArray(Import[]::new))
                    oneDeps.add(imp.exportScope.packageName.split("-", -1)[0].replaceAll("\\.", ""));
            
            var model = new ObjectValue(Map.of("dependencies", new ArrayValue(Arrays.stream(nativeDeps.keySet().toArray(String[]::new)).map(name -> new ObjectValue(Map.of("name", new StringValue(name), "version", new StringValue(nativeDeps.get(name))))).toArray(ObjectValue[]::new)), "onepackages", new ArrayValue(Arrays.stream(oneDeps.toArray(String[]::new)).map(dep -> new ObjectValue(Map.of("name", new StringValue(dep)))).toArray(ObjectValue[]::new))));
            projTemplate.generate(this.outDir + "/" + langName, model);
        }
    }
}