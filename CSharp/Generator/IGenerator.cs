using Generator;
using One.Ast;
using One;

namespace Generator
{
    public interface IGenerator {
        string getLangName();
        
        string getExtension();
        
        ITransformer[] getTransforms();
        
        void addPlugin(IGeneratorPlugin plugin);
        
        void addInclude(string include);
        
        string expr(IExpression expr);
        
        GeneratedFile[] generate(Package pkg);
    }
}