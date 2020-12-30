using One.Ast;
using One;
using System.Collections.Generic;

namespace One.Transforms
{
    public class FillAttributesFromTrivia : AstTransformer
    {
        public FillAttributesFromTrivia(): base("FillAttributesFromTrivia")
        {
            
        }
        
        protected override void visitAttributesAndTrivia(IHasAttributesAndTrivia node)
        {
            node.attributes = FillAttributesFromTrivia.processTrivia(node.leadingTrivia);
        }
        
        protected override Expression visitExpression(Expression expr)
        {
            return expr;
        }
        
        public static Dictionary<string, string> processTrivia(string trivia)
        {
            var result = new Dictionary<string, string> {};
            if (trivia != null && trivia != "") {
                var regex = new RegExp("(?:\\n|^)\\s*(?://|#|/\\*\\*?)\\s*@([A-Za-z0-9_.-]+) ?((?!\\n|\\*/|$).+)?");
                while (true) {
                    var match = regex.exec(trivia);
                    if (match == null)
                        break;
                    if (result.hasKey(match.get(1)))
                        // @php $result[$match[1]] .= "\n" . $match[2];
                        // @python result[match[1]] += "\n" + match[2]
                        // @csharp result[match[1]] += "\n" + match[2];
                        // @java result.put(match[1], result.get(match[1]) + "\n" + match[2]);
                        result[match[1]] += "\n" + match[2];
                    else
                        result.set(match.get(1), (match.get(2) ?? "") == "" ? "true" : match.get(2) ?? "");
                }
            }
            return result;
        }
    }
}