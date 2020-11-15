using One.Ast;
using One;

namespace One.Transforms
{
    public class ResolveImports : AstTransformer {
        public Workspace workspace;
        
        public ResolveImports(Workspace workspace): base("ResolveImports")
        {
            this.workspace = workspace;
        }
        
        public override void visitFile(SourceFile sourceFile)
        {
            ResolveImports.processFile(this.workspace, sourceFile);
        }
        
        public static void processFile(Workspace ws, SourceFile file)
        {
            foreach (var imp in file.imports) {
                var impPkg = ws.getPackage(imp.exportScope.packageName);
                var scope = impPkg.getExportedScope(imp.exportScope.scopeName);
                imp.imports = imp.importAll ? scope.getAllExports() : imp.imports.map(x => x is UnresolvedImport unrImp ? scope.getExport(unrImp.name) : x);
                file.addAvailableSymbols(imp.imports);
            }
        }
        
        public static void processWorkspace(Workspace ws)
        {
            foreach (var pkg in Object.values(ws.packages))
                foreach (var file in Object.values(pkg.files))
                    ResolveImports.processFile(ws, file);
        }
    }
}