using Generator;
using One.Ast;
using One;

namespace Generator
{
    public interface IGenerator {
        string getLangName();
        
        string getExtension();
        
        ITransformer[] getTransforms();
        
        GeneratedFile[] generate(Package pkg);
    }
}