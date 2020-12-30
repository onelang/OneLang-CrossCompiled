using Generator;
using One;
using StdLib;
using System.Collections.Generic;
using System.Threading.Tasks;
using Template;
using VM;

namespace Generator
{
    public class ProjectTemplateMeta
    {
        public string language;
        public string destinationDir;
        public string packageDir;
        public string[] templateFiles;
        
        public ProjectTemplateMeta(string language, string destinationDir, string packageDir, string[] templateFiles)
        {
            this.language = language;
            this.destinationDir = destinationDir;
            this.packageDir = packageDir;
            this.templateFiles = templateFiles;
        }
        
        public static ProjectTemplateMeta fromYaml(YamlValue obj)
        {
            return new ProjectTemplateMeta(obj.str("language"), obj.str("destination-dir"), obj.str("package-dir"), obj.strArr("template-files"));
        }
    }
    
    public class ProjectTemplate
    {
        public ProjectTemplateMeta meta;
        public string[] srcFiles;
        public string templateDir;
        
        public ProjectTemplate(string templateDir)
        {
            this.templateDir = templateDir;
            this.meta = ProjectTemplateMeta.fromYaml(OneYaml.load(OneFile.readText($"{templateDir}/index.yaml")));
            this.srcFiles = OneFile.listFiles($"{templateDir}/src", true);
        }
        
        public void generate(string dstDir, ObjectValue model)
        {
            foreach (var fn in this.srcFiles) {
                var srcFn = $"{this.templateDir}/src/{fn}";
                var dstFn = $"{dstDir}/{fn}";
                if (this.meta.templateFiles.includes(fn)) {
                    var tmpl = new TemplateParser(OneFile.readText(srcFn)).parse();
                    var dstFile = tmpl.format(new VMContext(model));
                    OneFile.writeText(dstFn, dstFile);
                }
                else
                    OneFile.copy(srcFn, dstFn);
            }
        }
    }
    
    public class ProjectDependency
    {
        public string name;
        public string version;
        
        public ProjectDependency(string name, string version)
        {
            this.name = name;
            this.version = version;
        }
    }
    
    public class OneProjectFile
    {
        public string name;
        public ProjectDependency[] dependencies;
        public string sourceDir;
        public string sourceLang;
        public string nativeSourceDir;
        public string outputDir;
        public string[] projectTemplates;
        
        public OneProjectFile(string name, ProjectDependency[] dependencies, string sourceDir, string sourceLang, string nativeSourceDir, string outputDir, string[] projectTemplates)
        {
            this.name = name;
            this.dependencies = dependencies;
            this.sourceDir = sourceDir;
            this.sourceLang = sourceLang;
            this.nativeSourceDir = nativeSourceDir;
            this.outputDir = outputDir;
            this.projectTemplates = projectTemplates;
        }
        
        public static OneProjectFile fromJson(OneJObject json)
        {
            return new OneProjectFile(json.get("name").asString(), json.get("dependencies").getArrayItems().map(dep => dep.asObject()).map(dep => new ProjectDependency(dep.get("name").asString(), dep.get("version").asString())), json.get("sourceDir").asString(), json.get("sourceLang").asString(), json.get("nativeSourceDir").asString(), json.get("outputDir").asString(), json.get("projectTemplates").getArrayItems().map(x => x.asString()));
        }
    }
    
    public class ProjectGenerator
    {
        public OneProjectFile projectFile;
        public string srcDir;
        public string outDir;
        public string baseDir;
        public string projDir;
        
        public ProjectGenerator(string baseDir, string projDir)
        {
            this.baseDir = baseDir;
            this.projDir = projDir;
            this.projectFile = null;
            this.projectFile = OneProjectFile.fromJson(OneJson.parse(OneFile.readText($"{projDir}/one.json")).asObject());
            this.srcDir = $"{this.projDir}/{this.projectFile.sourceDir}";
            this.outDir = $"{this.projDir}/{this.projectFile.outputDir}";
        }
        
        public async Task generate()
        {
            // copy native source codes from one project
            var nativeSrcDir = $"{this.projDir}/{this.projectFile.nativeSourceDir}";
            foreach (var fn in OneFile.listFiles(nativeSrcDir, true))
                OneFile.copy($"{nativeSrcDir}/{fn}", $"{this.outDir}/{fn}");
            
            var generators = new IGenerator[] { ((IGenerator)new JavaGenerator()), ((IGenerator)new CsharpGenerator()), ((IGenerator)new PythonGenerator()), ((IGenerator)new PhpGenerator()) };
            foreach (var tmplName in this.projectFile.projectTemplates) {
                var compiler = await CompilerHelper.initProject(this.projectFile.name, this.srcDir, this.projectFile.sourceLang, null);
                compiler.processWorkspace();
                
                var projTemplate = new ProjectTemplate($"{this.baseDir}/project-templates/{tmplName}");
                var langId = projTemplate.meta.language;
                var generator = generators.find(x => x.getLangName().toLowerCase() == langId);
                var langName = generator.getLangName();
                var outDir = $"{this.outDir}/{langName}";
                
                foreach (var trans in generator.getTransforms())
                    trans.visitFiles(Object.values(compiler.projectPkg.files));
                    
                // copy implementation native sources
                var oneDeps = new List<ImplementationPackage>();
                var nativeDeps = new Dictionary<string, string> {};
                foreach (var dep in this.projectFile.dependencies) {
                    var impl = compiler.pacMan.implementationPkgs.find(x => x.content.id.name == dep.name);
                    oneDeps.push(impl);
                    var langData = impl.implementationYaml.languages.get(langId);
                    if (langData == null)
                        continue;
                    
                    foreach (var natDep in langData.nativeDependencies ?? new ImplPkgNativeDependency[0])
                        nativeDeps.set(natDep.name, natDep.version);
                    
                    if (langData.nativeSrcDir != null) {
                        if (projTemplate.meta.packageDir == null)
                            throw new Error("Package directory is empty in project template!");
                        var srcDir = langData.nativeSrcDir + (langData.nativeSrcDir.endsWith("/") ? "" : "/");
                        var dstDir = $"{outDir}/{projTemplate.meta.packageDir}/{langData.packageDir ?? impl.content.id.name}";
                        var depFiles = Object.keys(impl.content.files).filter(x => x.startsWith(srcDir)).map(x => x.substr(srcDir.length()));
                        foreach (var fn in depFiles)
                            OneFile.writeText($"{dstDir}/{fn}", impl.content.files.get($"{srcDir}{fn}"));
                    }
                    
                    if (langData.generatorPlugins != null)
                        foreach (var genPlugFn in langData.generatorPlugins)
                            generator.addPlugin(new TemplateFileGeneratorPlugin(generator, impl.content.files.get(genPlugFn)));
                }
                
                // generate cross compiled source code
                console.log($"Generating {langName} code...");
                var files = generator.generate(compiler.projectPkg);
                foreach (var file in files)
                    OneFile.writeText($"{outDir}/{projTemplate.meta.destinationDir ?? ""}/{file.path}", file.content);
                
                // generate files from project template
                var model = new ObjectValue(new Dictionary<string, IVMValue> {
                    ["dependencies"] = ((IVMValue)new ArrayValue(Object.keys(nativeDeps).map(name => new ObjectValue(new Dictionary<string, IVMValue> {
                        ["name"] = ((IVMValue)new StringValue(name)),
                        ["version"] = ((IVMValue)new StringValue(nativeDeps.get(name)))
                    })))),
                    ["onepackages"] = ((IVMValue)new ArrayValue(oneDeps.map(dep => new ObjectValue(new Dictionary<string, IVMValue> {
                        ["vendor"] = ((IVMValue)new StringValue(dep.implementationYaml.vendor)),
                        ["id"] = ((IVMValue)new StringValue(dep.implementationYaml.name))
                    }))))
                });
                projTemplate.generate($"{outDir}", model);
            }
        }
    }
}