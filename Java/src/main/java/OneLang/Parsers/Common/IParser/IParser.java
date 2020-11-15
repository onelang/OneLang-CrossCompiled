package OneLang.Parsers.Common.IParser;

import OneLang.Parsers.Common.NodeManager.NodeManager;
import OneLang.One.Ast.Types.SourceFile;

import OneLang.Parsers.Common.NodeManager.NodeManager;
import OneLang.One.Ast.Types.SourceFile;

public interface IParser {
    NodeManager getNodeManager();
    void setNodeManager(NodeManager value);
    
    SourceFile parse();
}