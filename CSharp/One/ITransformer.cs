using One.Ast;

namespace One
{
    public interface ITransformer {
        string name { get; set; }
        
        void visitFiles(SourceFile[] files);
    }
}