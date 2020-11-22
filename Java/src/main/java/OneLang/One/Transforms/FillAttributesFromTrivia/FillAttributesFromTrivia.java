package OneLang.One.Transforms.FillAttributesFromTrivia;

import OneLang.One.Ast.Types.SourceFile;
import OneLang.One.Ast.Types.IMethodBase;
import OneLang.One.Ast.Types.IHasAttributesAndTrivia;
import OneLang.One.Ast.Types.Package;
import OneLang.One.Ast.Types.IMethodBaseWithTrivia;
import OneLang.One.Ast.Statements.ForeachStatement;
import OneLang.One.Ast.Statements.ForStatement;
import OneLang.One.Ast.Statements.IfStatement;
import OneLang.One.Ast.Statements.Block;
import OneLang.One.Ast.Statements.WhileStatement;
import OneLang.One.Ast.Statements.DoStatement;
import OneLang.One.AstTransformer.AstTransformer;
import OneLang.One.Ast.Expressions.Expression;

import OneLang.One.AstTransformer.AstTransformer;
import OneLang.One.Ast.Types.IHasAttributesAndTrivia;
import OneLang.One.Ast.Expressions.Expression;
import java.util.Map;
import java.util.LinkedHashMap;
import io.onelang.std.core.Objects;
import io.onelang.std.core.RegExp;

public class FillAttributesFromTrivia extends AstTransformer {
    public FillAttributesFromTrivia()
    {
        super("FillAttributesFromTrivia");
        
    }
    
    protected void visitAttributesAndTrivia(IHasAttributesAndTrivia node) {
        node.setAttributes(FillAttributesFromTrivia.processTrivia(node.getLeadingTrivia()));
    }
    
    protected Expression visitExpression(Expression expr) {
        return expr;
    }
    
    public static Map<String, String> processTrivia(String trivia) {
        var result = new LinkedHashMap<String, String>();
        if (trivia != null && !Objects.equals(trivia, "")) {
            var regex = new RegExp("(?:\\n|^)\\s*(?://|#|/\\*\\*?)\\s*@([A-Za-z0-9_.-]+) ?((?!\\n|\\*/|$).+)?");
            while (true) {
                var match = regex.exec(trivia);
                if (match == null)
                    break;
                if (result.containsKey(match[1]))
                    // @php $result[$match[1]] .= "\n" . $match[2];
                    // @python result[match[1]] += "\n" + match[2]
                    // @csharp result[match[1]] += "\n" + match[2];
                    // @java result.put(match[1], result.get(match[1]) + "\n" + match[2]);
                    result.put(match[1], result.get(match[1]) + "\n" + match[2]);
                else {
                    var getResult = match[2];
                    var getResult2 = match[2];
                    result.put(match[1], Objects.equals((getResult != null ? getResult : ""), "") ? "true" : getResult2 != null ? getResult2 : "");
                }
            }
        }
        return result;
    }
}