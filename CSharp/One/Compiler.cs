using One.Ast;
using One.Transforms;
using One;
using Parsers;
using StdLib;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace One
{
    public interface ICompilerHooks {
        void afterStage(string stageName);
    }
    
    public class Compiler {
        public PackageManager pacMan;
        public Workspace workspace;
        public SourceFile nativeFile;
        public ExportedScope nativeExports;
        public Package projectPkg;
        public ICompilerHooks hooks;
        
        public Compiler()
        {
            this.pacMan = null;
            this.workspace = null;
            this.nativeFile = null;
            this.nativeExports = null;
            this.projectPkg = null;
            this.hooks = null;
        }
        
        public async Task init(string packagesDir)
        {
            this.pacMan = new PackageManager(new PackagesFolderSource(packagesDir));
            await this.pacMan.loadAllCached();
        }
        
        public ITransformer[] getTransformers(bool forDeclarationFile)
        {
            var transforms = new List<ITransformer>();
            if (forDeclarationFile) {
                transforms.push(new FillParent());
                transforms.push(new FillAttributesFromTrivia());
                transforms.push(new ResolveImports(this.workspace));
                transforms.push(new ResolveGenericTypeIdentifiers());
                transforms.push(new ResolveUnresolvedTypes());
                transforms.push(new FillMutabilityInfo());
            }
            else {
                transforms.push(new FillParent());
                transforms.push(new FillAttributesFromTrivia());
                transforms.push(new ResolveImports(this.workspace));
                transforms.push(new ResolveGenericTypeIdentifiers());
                transforms.push(new ConvertToMethodCall());
                transforms.push(new ResolveUnresolvedTypes());
                transforms.push(new ResolveIdentifiers());
                transforms.push(new InstanceOfImplicitCast());
                transforms.push(new DetectMethodCalls());
                transforms.push(new InferTypes());
                transforms.push(new CollectInheritanceInfo());
                transforms.push(new FillMutabilityInfo());
                transforms.push(new LambdaCaptureCollector());
            }
            return transforms.ToArray();
        }
        
        public void setupNativeResolver(string content)
        {
            this.nativeFile = TypeScriptParser2.parseFile(content);
            this.nativeExports = Package.collectExportsFromFile(this.nativeFile, true);
            foreach (var trans in this.getTransformers(true))
                trans.visitFiles(new SourceFile[] { this.nativeFile });
        }
        
        public void newWorkspace(string pkgName = "@")
        {
            this.workspace = new Workspace();
            foreach (var intfPkg in this.pacMan.interfacesPkgs) {
                var libName = $"{intfPkg.interfaceYaml.vendor}.{intfPkg.interfaceYaml.name}-v{intfPkg.interfaceYaml.version}";
                this.addInterfacePackage(libName, intfPkg.definition);
            }
            
            this.projectPkg = new Package(pkgName, false);
            this.workspace.addPackage(this.projectPkg);
        }
        
        public void addInterfacePackage(string libName, string definitionFileContent)
        {
            var libPkg = new Package(libName, true);
            var file = TypeScriptParser2.parseFile(definitionFileContent, new SourcePath(libPkg, Package.INDEX));
            this.setupFile(file);
            libPkg.addFile(file, true);
            this.workspace.addPackage(libPkg);
        }
        
        public void setupFile(SourceFile file)
        {
            file.addAvailableSymbols(this.nativeExports.getAllExports());
            file.literalTypes = new LiteralTypes((((Class)file.availableSymbols.get("TsBoolean"))).type, (((Class)file.availableSymbols.get("TsNumber"))).type, (((Class)file.availableSymbols.get("TsString"))).type, (((Class)file.availableSymbols.get("RegExp"))).type, (((Class)file.availableSymbols.get("TsArray"))).type, (((Class)file.availableSymbols.get("TsMap"))).type, (((Class)file.availableSymbols.get("Error"))).type, (((Class)file.availableSymbols.get("Promise"))).type);
            file.arrayTypes = new ClassType[] { (((Class)file.availableSymbols.get("TsArray"))).type, (((Class)file.availableSymbols.get("IterableIterator"))).type, (((Class)file.availableSymbols.get("RegExpExecArray"))).type, (((Class)file.availableSymbols.get("TsString"))).type, (((Class)file.availableSymbols.get("Set"))).type };
        }
        
        public void addProjectFile(string fn, string content)
        {
            var file = TypeScriptParser2.parseFile(content, new SourcePath(this.projectPkg, fn));
            this.setupFile(file);
            this.projectPkg.addFile(file);
        }
        
        public void processFiles(SourceFile[] files)
        {
            foreach (var trans in this.getTransformers(false)) {
                trans.visitFiles(files);
                if (this.hooks != null)
                    this.hooks.afterStage(trans.name);
            }
        }
        
        public void processWorkspace()
        {
            foreach (var pkg in Object.values(this.workspace.packages).filter(x => x.definitionOnly))
                foreach (var trans in this.getTransformers(true))
                    trans.visitFiles(Object.values(pkg.files));
            
            this.processFiles(Object.values(this.projectPkg.files));
        }
    }
}