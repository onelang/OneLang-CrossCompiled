using One.Ast;

namespace Generator
{
    public interface IGeneratorPlugin {
        string expr(IExpression expr);
        
        string stmt(Statement stmt);
    }
}