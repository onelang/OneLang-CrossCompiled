package OneLang.One.IssueDetectors.CircularDependencyDetector;

import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.Types.IInterface;
import OneLang.One.Ast.Types.IResolvedImportable;
import OneLang.One.Ast.Types.Package;
import OneLang.One.Ast.Types.SourceFile;
import OneLang.One.Ast.Types.Workspace;

public interface IGraphVisitor<TNode> {
    void processNode(TNode node);
}