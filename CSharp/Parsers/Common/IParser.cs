using One.Ast;
using Parsers.Common;

namespace Parsers.Common
{
    public interface IParser {
        NodeManager nodeManager { get; set; }
        
        SourceFile parse();
    }
}