using Generator;
using One.Ast;
using One;
using Parsers.Common;
using StdLib;
using System.Collections.Generic;
using Utils;

namespace Generator
{
    public interface IVMValue {
        
    }
    
    public interface ITemplateNode {
        string format(ObjectValue model);
    }
    
    public class ProjectTemplateMeta {
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
    
    public class ObjectValue : IVMValue {
        public Dictionary<string, IVMValue> props;
        
        public ObjectValue(Dictionary<string, IVMValue> props)
        {
            this.props = props;
        }
    }
    
    public class StringValue : IVMValue {
        public string value;
        
        public StringValue(string value)
        {
            this.value = value;
        }
    }
    
    public class ArrayValue : IVMValue {
        public IVMValue[] items;
        
        public ArrayValue(IVMValue[] items)
        {
            this.items = items;
        }
    }
    
    public class TemplateBlock : ITemplateNode {
        public ITemplateNode[] items;
        
        public TemplateBlock(ITemplateNode[] items)
        {
            this.items = items;
        }
        
        public string format(ObjectValue model)
        {
            return this.items.map(x => x.format(model)).join("");
        }
    }
    
    public class LiteralNode : ITemplateNode {
        public string value;
        
        public LiteralNode(string value)
        {
            this.value = value;
        }
        
        public string format(ObjectValue model)
        {
            return this.value;
        }
    }
    
    public class ExprVM {
        public ObjectValue model;
        
        public ExprVM(ObjectValue model)
        {
            this.model = model;
        }
        
        public static IVMValue propAccess(IVMValue obj, string propName)
        {
            if (!(obj is ObjectValue))
                throw new Error("You can only access a property of an object!");
            if (!((((ObjectValue)obj)).props.hasKey(propName)))
                throw new Error($"Property '{propName}' does not exists on this object!");
            return (((ObjectValue)obj)).props.get(propName);
        }
        
        public IVMValue evaluate(Expression expr)
        {
            if (expr is Identifier ident)
                return ExprVM.propAccess(this.model, ident.text);
            else if (expr is PropertyAccessExpression propAccExpr) {
                var objValue = this.evaluate(propAccExpr.object_);
                return ExprVM.propAccess(objValue, propAccExpr.propertyName);
            }
            else
                throw new Error("Unsupported expression!");
        }
    }
    
    public class ExpressionNode : ITemplateNode {
        public Expression expr;
        
        public ExpressionNode(Expression expr)
        {
            this.expr = expr;
        }
        
        public string format(ObjectValue model)
        {
            var result = new ExprVM(model).evaluate(this.expr);
            if (result is StringValue strValue)
                return strValue.value;
            else
                throw new Error($"ExpressionNode ({TSOverviewGenerator.preview.expr(this.expr)}) return a non-string result!");
        }
    }
    
    public class ForNode : ITemplateNode {
        public string variableName;
        public Expression itemsExpr;
        public TemplateBlock body;
        public string joiner;
        
        public ForNode(string variableName, Expression itemsExpr, TemplateBlock body, string joiner)
        {
            this.variableName = variableName;
            this.itemsExpr = itemsExpr;
            this.body = body;
            this.joiner = joiner;
        }
        
        public string format(ObjectValue model)
        {
            var items = new ExprVM(model).evaluate(this.itemsExpr);
            if (!(items is ArrayValue))
                throw new Error($"ForNode items ({TSOverviewGenerator.preview.expr(this.itemsExpr)}) return a non-array result!");
            
            var result = "";
            foreach (var item in (((ArrayValue)items)).items) {
                if (this.joiner != null && result != "")
                    result += this.joiner;
                
                model.props.set(this.variableName, item);
                result += this.body.format(model);
            }
            /* unset model.props.get(this.variableName); */
            return result;
        }
    }
    
    public class TemplateParser {
        public Reader reader;
        public ExpressionParser exprParser;
        public string template;
        
        public TemplateParser(string template)
        {
            this.template = template;
            this.reader = new Reader(template);
            this.exprParser = new ExpressionParser(this.reader);
        }
        
        public Dictionary<string, string> parseAttributes()
        {
            var result = new Dictionary<string, string> {};
            while (this.reader.readToken(",")) {
                var key = this.reader.expectIdentifier();
                var value = this.reader.readToken("=") ? this.reader.expectString() : null;
                result.set(key, value);
            }
            return result;
        }
        
        public TemplateBlock parseBlock()
        {
            var items = new List<ITemplateNode>();
            while (!this.reader.eof) {
                if (this.reader.peekToken("{{/"))
                    break;
                if (this.reader.readToken("{{")) {
                    if (this.reader.readToken("for")) {
                        var varName = this.reader.readIdentifier();
                        this.reader.expectToken("of");
                        var itemsExpr = this.exprParser.parse();
                        var attrs = this.parseAttributes();
                        this.reader.expectToken("}}");
                        var body = this.parseBlock();
                        this.reader.expectToken("{{/for}}");
                        items.push(new ForNode(varName, itemsExpr, body, attrs.get("joiner")));
                    }
                    else {
                        var expr = this.exprParser.parse();
                        items.push(new ExpressionNode(expr));
                        this.reader.expectToken("}}");
                    }
                }
                else {
                    var literal = this.reader.readUntil("{{", true);
                    if (literal.endsWith("\\") && !literal.endsWith("\\\\"))
                        literal = literal.substring(0, literal.length() - 1) + "{{";
                    if (literal != "")
                        items.push(new LiteralNode(literal));
                }
            }
            return new TemplateBlock(items.ToArray());
        }
        
        public TemplateBlock parse()
        {
            return this.parseBlock();
        }
    }
    
    public class TemplateFile {
        public TemplateBlock main;
        public string template;
        
        public TemplateFile(string template)
        {
            this.template = template;
            this.main = new TemplateParser(template).parse();
        }
        
        public string format(ObjectValue model)
        {
            return this.main.format(model);
        }
    }
    
    public class ProjectTemplate {
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
                    var tmplFile = new TemplateFile(OneFile.readText(srcFn));
                    var dstFile = tmplFile.format(model);
                    OneFile.writeText(dstFn, dstFile);
                }
                else
                    OneFile.copy(srcFn, dstFn);
            }
        }
    }
    
    public class ProjectDependency {
        public string name;
        public string version;
        
        public ProjectDependency(string name, string version)
        {
            this.name = name;
            this.version = version;
        }
    }
    
    public class OneProjectFile {
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
    
    public class ProjectGenerator {
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
        
        public async void generate()
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
                
                foreach (var trans in generator.getTransforms())
                    trans.visitFiles(Object.values(compiler.projectPkg.files));
                
                // generate cross compiled source code
                var outDir = $"{this.outDir}/{langName}";
                console.log($"Generating {langName} code...");
                var files = generator.generate(compiler.projectPkg);
                foreach (var file in files)
                    OneFile.writeText($"{outDir}/{projTemplate.meta.destinationDir ?? ""}/{file.path}", file.content);
                
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
                        var dstDir = $"{outDir}/{projTemplate.meta.packageDir}/{impl.content.id.name}";
                        var depFiles = Object.keys(impl.content.files).filter(x => x.startsWith(srcDir)).map(x => x.substr(srcDir.length()));
                        foreach (var fn in depFiles)
                            OneFile.writeText($"{dstDir}/{fn}", impl.content.files.get($"{srcDir}{fn}"));
                    }
                }
                
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