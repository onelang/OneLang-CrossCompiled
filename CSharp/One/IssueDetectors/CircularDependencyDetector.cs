using One.Ast;

namespace One.IssueDetectors
{
    public enum DetectionMode { AllImports, AllInheritence, BaseClassesOnly }
    
    public interface IGraphVisitor<TNode> {
        void processNode(TNode node);
    }
    
    public class GraphCycleDetector<TNode> {
        public Map<TNode, bool> nodeIsInPath;
        public IGraphVisitor<TNode> visitor;
        
        public GraphCycleDetector(IGraphVisitor<TNode> visitor)
        {
            this.visitor = visitor;
            this.nodeIsInPath = null;
        }
        
        public void findCycles(TNode[] nodes)
        {
            this.nodeIsInPath = new Map<TNode, bool>();
            foreach (var node in nodes)
                this.visitNode(node);
        }
        
        public bool visitNode(TNode node)
        {
            if (!this.nodeIsInPath.has(node)) {
                // untouched node
                this.nodeIsInPath.set(node, true);
                this.visitor.processNode(node);
                this.nodeIsInPath.set(node, false);
                return false;
            }
            else
                // true = node used in current path = cycle
                // false = node was already scanned previously (not a cycle)
                return this.nodeIsInPath.get(node);
        }
    }
    
    public class CircularDependencyDetector : IGraphVisitor<SourceFile> {
        public GraphCycleDetector<SourceFile> detector;
        public DetectionMode detectionMode;
        
        public CircularDependencyDetector(DetectionMode detectionMode)
        {
            this.detectionMode = detectionMode;
            this.detector = new GraphCycleDetector<SourceFile>(this);
        }
        
        public void processIntfs(SourceFile file, string type, IInterface[] intfs)
        {
            foreach (var intf in intfs)
                foreach (var baseIntf in intf.getAllBaseInterfaces()) {
                    if (baseIntf.parentFile != file && this.detector.visitNode(baseIntf.parentFile))
                        console.error($"Circular dependency found in file '{file.exportScope.getId()}': {type} '{intf.name}' inherited from '{baseIntf.name}' (from '{baseIntf.parentFile.exportScope.getId()}')");
                }
        }
        
        public void processNode(SourceFile file)
        {
            if (this.detectionMode == DetectionMode.AllImports)
                foreach (var imp in file.imports)
                    foreach (var impSym in imp.imports) {
                        var impFile = (((IResolvedImportable)impSym)).parentFile;
                        if (this.detector.visitNode(impFile))
                            console.error($"Circular dependency found in file '{file.exportScope.getId()}' via the import '{impSym.name}' imported from '{impFile.exportScope.getId()}'");
                    }
            else if (this.detectionMode == DetectionMode.AllInheritence) {
                this.processIntfs(file, "class", file.classes);
                this.processIntfs(file, "interface", file.interfaces);
            }
            else if (this.detectionMode == DetectionMode.BaseClassesOnly)
                foreach (var cls in file.classes) {
                    var baseClass = (((ClassType)cls.baseClass)).decl;
                    if (baseClass.parentFile != file && this.detector.visitNode(baseClass.parentFile))
                        console.error($"Circular dependency found in file '{file.exportScope.getId()}': class '{cls.name}' inherited from '{baseClass.name}' (from '{baseClass.parentFile.exportScope.getId()}')");
                }
        }
        
        public void processPackage(Package pkg)
        {
            this.detector.findCycles(Object.values(pkg.files));
        }
        
        public void processWorkspace(Workspace ws)
        {
            foreach (var pkg in Object.values(ws.packages))
                this.processPackage(pkg);
        }
    }
}