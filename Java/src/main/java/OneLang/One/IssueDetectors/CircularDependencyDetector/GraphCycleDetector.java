package OneLang.One.IssueDetectors.CircularDependencyDetector;

import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.Types.IInterface;
import OneLang.One.Ast.Types.IResolvedImportable;
import OneLang.One.Ast.Types.Package;
import OneLang.One.Ast.Types.SourceFile;
import OneLang.One.Ast.Types.Workspace;

import java.util.Map;
import OneLang.One.IssueDetectors.CircularDependencyDetector.IGraphVisitor;
import java.util.LinkedHashMap;

public class GraphCycleDetector<TNode> {
    public Map<TNode, Boolean> nodeIsInPath;
    public IGraphVisitor<TNode> visitor;
    
    public GraphCycleDetector(IGraphVisitor<TNode> visitor)
    {
        this.visitor = visitor;
        this.nodeIsInPath = null;
    }
    
    public void findCycles(TNode[] nodes) {
        this.nodeIsInPath = new LinkedHashMap<TNode, Boolean>();
        for (var node : nodes)
            this.visitNode(node);
    }
    
    public Boolean visitNode(TNode node) {
        if (!this.nodeIsInPath.containsKey(node)) {
            // untouched node
            this.nodeIsInPath.put(node, true);
            this.visitor.processNode(node);
            this.nodeIsInPath.put(node, false);
            return false;
        }
        else
            // true = node used in current path = cycle
            // false = node was already scanned previously (not a cycle)
            return this.nodeIsInPath.get(node);
    }
}