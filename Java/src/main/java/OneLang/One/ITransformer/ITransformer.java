package OneLang.One.ITransformer;

import OneLang.One.Ast.Types.SourceFile;

import OneLang.One.Ast.Types.SourceFile;

public interface ITransformer {
    String getName();
    void setName(String value);
    
    void visitFiles(SourceFile[] files);
}