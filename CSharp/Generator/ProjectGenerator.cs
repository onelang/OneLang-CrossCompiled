using Generator;
using One.Ast;
using One;
using Parsers.Common;
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
        public string destionationDir;
        public string[] templateFiles;
        
        public ProjectTemplateMeta(string language, string destionationDir, string[] templateFiles)
        {
            this.language = language;
            this.destionationDir = destionationDir;
            this.templateFiles = templateFiles;
        }
        
        public static ProjectTemplateMeta fromYaml(YamlValue obj)
        {
            return new ProjectTemplateMeta(obj.str("language"), obj.str("destination-dir"), obj.strArr("template-files"));
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
        
        public ForNode(string variableName, Expression itemsExpr, TemplateBlock body)
        {
            this.variableName = variableName;
            this.itemsExpr = itemsExpr;
            this.body = body;
        }
        
        public string format(ObjectValue model)
        {
            var items = new ExprVM(model).evaluate(this.itemsExpr);
            if (!(items is ArrayValue))
                throw new Error($"ForNode items ({TSOverviewGenerator.preview.expr(this.itemsExpr)}) return a non-array result!");
            
            var result = "";
            foreach (var item in (((ArrayValue)items)).items) {
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
                        this.reader.expectToken("}}");
                        var body = this.parseBlock();
                        this.reader.expectToken("{{/for}}");
                        items.push(new ForNode(varName, itemsExpr, body));
                    }
                    else {
                        var expr = this.exprParser.parse();
                        items.push(new ExpressionNode(expr));
                        this.reader.expectToken("}}");
                    }
                }
                else {
                    var literal = this.reader.readUntil("{{", true);
                    if (literal.endsWith("\\"))
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
        
        public ProjectDependency(string name)
        {
            this.name = name;
        }
    }
    
    public class OneProjectFile {
        public string name;
        public ProjectDependency[] dependencies;
        public string sourceDir;
        public string sourceLang;
        public string outputDir;
        public string[] projectTemplates;
        
        public OneProjectFile(string name, ProjectDependency[] dependencies, string sourceDir, string sourceLang, string outputDir, string[] projectTemplates)
        {
            this.name = name;
            this.dependencies = dependencies;
            this.sourceDir = sourceDir;
            this.sourceLang = sourceLang;
            this.outputDir = outputDir;
            this.projectTemplates = projectTemplates;
        }
        
        public static OneProjectFile fromJson(OneJObject json)
        {
            return new OneProjectFile(json.get("name").asString(), json.get("dependencies").getArrayItems().map(dep => dep.asObject()).map(dep => new ProjectDependency(dep.get("name").asString())), json.get("sourceDir").asString(), json.get("sourceLang").asString(), json.get("outputDir").asString(), json.get("projectTemplates").getArrayItems().map(x => x.asString()));
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
                
                console.log($"Generating {langName} code...");
                var files = generator.generate(compiler.projectPkg);
                foreach (var file in files)
                    OneFile.writeText($"{this.outDir}/{langName}/{projTemplate.meta.destionationDir ?? ""}/{file.path}", file.content);
                
                var nativeDeps = new Dictionary<string, string> {};
                foreach (var dep in this.projectFile.dependencies) {
                    var impl = compiler.pacMan.implementationPkgs.find(x => x.content.id.name == dep.name);
                    var langData = impl.implementationYaml.languages.find(x => x.id == langId);
                    if (langData == null)
                        continue;
                    foreach (var natDep in langData.nativeDependencies ?? new ImplPkgNativeDependency[0])
                        nativeDeps.set(natDep.name, natDep.version);
                }
                
                var oneDeps = new Set<string>();
                oneDeps.add("OneCore");
                foreach (var file in Object.values(compiler.projectPkg.files))
                    foreach (var imp in file.imports.filter(x => x.exportScope.packageName != compiler.projectPkg.name))
                        oneDeps.add(imp.exportScope.packageName.split(new RegExp("-")).get(0).replace(new RegExp("\\."), ""));
                
                var model = new ObjectValue(new Dictionary<string, ArrayValue> {
                    ["dependencies"] = new ArrayValue(Object.keys(nativeDeps).map(name => new ObjectValue(new Dictionary<string, StringValue> {
                        ["name"] = new StringValue(name),
                        ["version"] = new StringValue(nativeDeps.get(name))
                    }))),
                    ["onepackages"] = new ArrayValue(Array.from(oneDeps.values()).map(dep => new ObjectValue(new Dictionary<string, StringValue> { ["name"] = new StringValue(dep) })))
                });
                projTemplate.generate($"{this.outDir}/{langName}", model);
            }
        }
    }
}