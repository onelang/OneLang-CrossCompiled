package OneLang.One.Transforms.ResolveImports;

import OneLang.One.Ast.Types.Workspace;
import OneLang.One.Ast.Types.UnresolvedImport;
import OneLang.One.Ast.Types.SourceFile;
import OneLang.One.Ast.Types.Package;
import OneLang.One.AstTransformer.AstTransformer;

import OneLang.One.AstTransformer.AstTransformer;
import OneLang.One.Ast.Types.Workspace;
import OneLang.One.Ast.Types.SourceFile;
import OneLang.One.Ast.Types.UnresolvedImport;
import java.util.Arrays;
import OneLang.One.Ast.Types.IImportable;
import OneLang.One.Ast.Types.Package;

public class ResolveImports extends AstTransformer {
    public Workspace workspace;
    
    public ResolveImports(Workspace workspace)
    {
        super("ResolveImports");
        this.workspace = workspace;
    }
    
    public void visitFile(SourceFile sourceFile) {
        ResolveImports.processFile(this.workspace, sourceFile);
    }
    
    public static void processFile(Workspace ws, SourceFile file) {
        for (var imp : file.imports) {
            var impPkg = ws.getPackage(imp.exportScope.packageName);
            var scope = impPkg.getExportedScope(imp.exportScope.scopeName);
            imp.imports = imp.importAll ? scope.getAllExports() : Arrays.stream(imp.imports).map(x -> x instanceof UnresolvedImport ? scope.getExport(((UnresolvedImport)x).getName()) : x).toArray(IImportable[]::new);
            file.addAvailableSymbols(imp.imports);
        }
    }
    
    public static void processWorkspace(Workspace ws) {
        for (var pkg : ws.packages.values().toArray(Package[]::new))
            for (var file : pkg.files.values().toArray(SourceFile[]::new))
                ResolveImports.processFile(ws, file);
    }
}