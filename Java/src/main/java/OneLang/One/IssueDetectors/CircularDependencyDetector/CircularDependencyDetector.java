package OneLang.One.IssueDetectors.CircularDependencyDetector;

import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.Types.IInterface;
import OneLang.One.Ast.Types.IResolvedImportable;
import OneLang.One.Ast.Types.Package;
import OneLang.One.Ast.Types.SourceFile;
import OneLang.One.Ast.Types.Workspace;

import OneLang.One.IssueDetectors.CircularDependencyDetector.IGraphVisitor;
import OneLang.One.Ast.Types.SourceFile;
import OneLang.One.IssueDetectors.CircularDependencyDetector.GraphCycleDetector;
import OneStd.console;
import OneLang.One.Ast.Types.IInterface;
import OneLang.One.Ast.Types.IResolvedImportable;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.Types.Package;
import OneLang.One.Ast.Types.Workspace;

public class CircularDependencyDetector implements IGraphVisitor<SourceFile> {
    public GraphCycleDetector<SourceFile> detector;
    public DetectionMode detectionMode;
    
    public CircularDependencyDetector(DetectionMode detectionMode)
    {
        this.detectionMode = detectionMode;
        this.detector = new GraphCycleDetector<SourceFile>(this);
    }
    
    public void processIntfs(SourceFile file, String type, IInterface[] intfs) {
        for (var intf : intfs)
            for (var baseIntf : intf.getAllBaseInterfaces()) {
                if (baseIntf.getParentFile() != file && this.detector.visitNode(baseIntf.getParentFile()))
                    console.error("Circular dependency found in file '" + file.exportScope.getId() + "': " + type + " '" + intf.getName() + "' inherited from '" + baseIntf.getName() + "' (from '" + baseIntf.getParentFile().exportScope.getId() + "')");
            }
    }
    
    public void processNode(SourceFile file) {
        if (this.detectionMode == DetectionMode.AllImports)
            for (var imp : file.imports)
                for (var impSym : imp.imports) {
                    var impFile = (((IResolvedImportable)impSym)).getParentFile();
                    if (this.detector.visitNode(impFile))
                        console.error("Circular dependency found in file '" + file.exportScope.getId() + "' via the import '" + impSym.getName() + "' imported from '" + impFile.exportScope.getId() + "'");
                }
        else if (this.detectionMode == DetectionMode.AllInheritence) {
            this.processIntfs(file, "class", file.classes);
            this.processIntfs(file, "interface", file.interfaces);
        }
        else if (this.detectionMode == DetectionMode.BaseClassesOnly)
            for (var cls : file.classes) {
                var baseClass = (((ClassType)cls.baseClass)).decl;
                if (baseClass.getParentFile() != file && this.detector.visitNode(baseClass.getParentFile()))
                    console.error("Circular dependency found in file '" + file.exportScope.getId() + "': class '" + cls.getName() + "' inherited from '" + baseClass.getName() + "' (from '" + baseClass.getParentFile().exportScope.getId() + "')");
            }
    }
    
    public void processPackage(Package pkg) {
        this.detector.findCycles(pkg.files.values().toArray(SourceFile[]::new));
    }
    
    public void processWorkspace(Workspace ws) {
        for (var pkg : ws.packages.values().toArray(Package[]::new))
            this.processPackage(pkg);
    }
}