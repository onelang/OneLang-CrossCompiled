package OneLang.One.Compiler;

import OneLang.One.Ast.Types.Workspace;
import OneLang.One.Ast.Types.Package;
import OneLang.One.Ast.Types.SourcePath;
import OneLang.One.Ast.Types.SourceFile;
import OneLang.One.Ast.Types.ExportedScope;
import OneLang.One.Ast.Types.LiteralTypes;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.ExportScopeRef;
import OneLang.Parsers.TypeScriptParser2.TypeScriptParser2;
import OneLang.StdLib.PackageManager.PackageManager;
import OneLang.StdLib.PackagesFolderSource.PackagesFolderSource;
import OneLang.One.Transforms.FillParent.FillParent;
import OneLang.One.Transforms.FillAttributesFromTrivia.FillAttributesFromTrivia;
import OneLang.One.Transforms.ResolveGenericTypeIdentifiers.ResolveGenericTypeIdentifiers;
import OneLang.One.Transforms.ResolveUnresolvedTypes.ResolveUnresolvedTypes;
import OneLang.One.Transforms.ResolveImports.ResolveImports;
import OneLang.One.Transforms.ConvertToMethodCall.ConvertToMethodCall;
import OneLang.One.Transforms.ResolveIdentifiers.ResolveIdentifiers;
import OneLang.One.Transforms.InstanceOfImplicitCast.InstanceOfImplicitCast;
import OneLang.One.Transforms.DetectMethodCalls.DetectMethodCalls;
import OneLang.One.Transforms.InferTypes.InferTypes;
import OneLang.One.Transforms.CollectInheritanceInfo.CollectInheritanceInfo;
import OneLang.One.Transforms.FillMutabilityInfo.FillMutabilityInfo;
import OneLang.One.AstTransformer.AstTransformer;
import OneLang.One.ITransformer.ITransformer;
import OneLang.One.Transforms.LambdaCaptureCollector.LambdaCaptureCollector;

import OneLang.StdLib.PackageManager.PackageManager;
import OneLang.One.Ast.Types.Workspace;
import OneLang.One.Ast.Types.SourceFile;
import OneLang.One.Ast.Types.ExportedScope;
import OneLang.One.Ast.Types.Package;
import OneLang.One.Compiler.ICompilerHooks;
import OneLang.StdLib.PackagesFolderSource.PackagesFolderSource;
import OneLang.One.ITransformer.ITransformer;
import java.util.ArrayList;
import OneLang.One.Transforms.FillParent.FillParent;
import OneLang.One.Transforms.FillAttributesFromTrivia.FillAttributesFromTrivia;
import OneLang.One.Transforms.ResolveImports.ResolveImports;
import OneLang.One.Transforms.ResolveGenericTypeIdentifiers.ResolveGenericTypeIdentifiers;
import OneLang.One.Transforms.ResolveUnresolvedTypes.ResolveUnresolvedTypes;
import OneLang.One.Transforms.FillMutabilityInfo.FillMutabilityInfo;
import OneLang.One.Transforms.ConvertToMethodCall.ConvertToMethodCall;
import OneLang.One.Transforms.ResolveIdentifiers.ResolveIdentifiers;
import OneLang.One.Transforms.InstanceOfImplicitCast.InstanceOfImplicitCast;
import OneLang.One.Transforms.DetectMethodCalls.DetectMethodCalls;
import OneLang.One.Transforms.InferTypes.InferTypes;
import OneLang.One.Transforms.CollectInheritanceInfo.CollectInheritanceInfo;
import OneLang.One.Transforms.LambdaCaptureCollector.LambdaCaptureCollector;
import OneLang.One.Ast.Types.SourcePath;
import OneLang.One.Ast.Types.LiteralTypes;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.AstTypes.ClassType;
import java.util.Arrays;

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
    
    public void init(String packagesDir) {
        this.pacMan = new PackageManager(new PackagesFolderSource(packagesDir));
        this.pacMan.loadAllCached();
    }
    
    public ITransformer[] getTransformers(Boolean forDeclarationFile) {
        var transforms = new ArrayList<ITransformer>();
        if (forDeclarationFile) {
            transforms.add(new FillParent());
            transforms.add(new FillAttributesFromTrivia());
            transforms.add(new ResolveImports(this.workspace));
            transforms.add(new ResolveGenericTypeIdentifiers());
            transforms.add(new ResolveUnresolvedTypes());
            transforms.add(new FillMutabilityInfo());
        }
        else {
            transforms.add(new FillParent());
            transforms.add(new FillAttributesFromTrivia());
            transforms.add(new ResolveImports(this.workspace));
            transforms.add(new ResolveGenericTypeIdentifiers());
            transforms.add(new ConvertToMethodCall());
            transforms.add(new ResolveUnresolvedTypes());
            transforms.add(new ResolveIdentifiers());
            transforms.add(new InstanceOfImplicitCast());
            transforms.add(new DetectMethodCalls());
            transforms.add(new InferTypes());
            transforms.add(new CollectInheritanceInfo());
            transforms.add(new FillMutabilityInfo());
            transforms.add(new LambdaCaptureCollector());
        }
        return transforms.toArray(ITransformer[]::new);
    }
    
    public void setupNativeResolver(String content) {
        this.nativeFile = TypeScriptParser2.parseFile(content, null);
        this.nativeExports = Package.collectExportsFromFile(this.nativeFile, true, null);
        for (var trans : this.getTransformers(true))
            trans.visitFiles(new SourceFile[] { this.nativeFile });
    }
    
    public void newWorkspace(String pkgName) {
        this.workspace = new Workspace();
        for (var intfPkg : this.pacMan.interfacesPkgs) {
            var libName = intfPkg.interfaceYaml.vendor + "." + intfPkg.interfaceYaml.name + "-v" + intfPkg.interfaceYaml.version;
            this.addInterfacePackage(libName, intfPkg.definition);
        }
        
        this.projectPkg = new Package(pkgName, false);
        this.workspace.addPackage(this.projectPkg);
    }
    
    public void addInterfacePackage(String libName, String definitionFileContent) {
        var libPkg = new Package(libName, true);
        var file = TypeScriptParser2.parseFile(definitionFileContent, new SourcePath(libPkg, Package.INDEX));
        this.setupFile(file);
        libPkg.addFile(file, true);
        this.workspace.addPackage(libPkg);
    }
    
    public void setupFile(SourceFile file) {
        file.addAvailableSymbols(this.nativeExports.getAllExports());
        file.literalTypes = new LiteralTypes((((Class)file.availableSymbols.get("TsBoolean"))).type, (((Class)file.availableSymbols.get("TsNumber"))).type, (((Class)file.availableSymbols.get("TsString"))).type, (((Class)file.availableSymbols.get("RegExp"))).type, (((Class)file.availableSymbols.get("TsArray"))).type, (((Class)file.availableSymbols.get("TsMap"))).type, (((Class)file.availableSymbols.get("Error"))).type, (((Class)file.availableSymbols.get("Promise"))).type);
        file.arrayTypes = new ClassType[] { (((Class)file.availableSymbols.get("TsArray"))).type, (((Class)file.availableSymbols.get("IterableIterator"))).type, (((Class)file.availableSymbols.get("RegExpExecArray"))).type, (((Class)file.availableSymbols.get("TsString"))).type, (((Class)file.availableSymbols.get("Set"))).type };
    }
    
    public void addProjectFile(String fn, String content) {
        var file = TypeScriptParser2.parseFile(content, new SourcePath(this.projectPkg, fn));
        this.setupFile(file);
        this.projectPkg.addFile(file, false);
    }
    
    public void processFiles(SourceFile[] files) {
        for (var trans : this.getTransformers(false)) {
            trans.visitFiles(files);
            if (this.hooks != null)
                this.hooks.afterStage(trans.getName());
        }
    }
    
    public void processWorkspace() {
        for (var pkg : Arrays.stream(this.workspace.packages.values().toArray(Package[]::new)).filter(x -> x.definitionOnly).toArray(Package[]::new))
            for (var trans : this.getTransformers(true))
                trans.visitFiles(pkg.files.values().toArray(SourceFile[]::new));
        
        this.processFiles(this.projectPkg.files.values().toArray(SourceFile[]::new));
    }
}