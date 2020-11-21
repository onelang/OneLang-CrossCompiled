from onelang_core import *
import OneLang.One.Ast.Types as types
import OneLang.One.Ast.Statements as stats
import OneLang.One.AstTransformer as astTrans
import OneLang.One.Ast.Expressions as exprs

class FillAttributesFromTrivia(astTrans.AstTransformer):
    def __init__(self):
        super().__init__("FillAttributesFromTrivia")
    
    def visit_attributes_and_trivia(self, node):
        node.attributes = FillAttributesFromTrivia.process_trivia(node.leading_trivia)
    
    def visit_expression(self, expr):
        return expr
    
    @classmethod
    def process_trivia(cls, trivia):
        result = {}
        if trivia != None and trivia != "":
            regex = RegExp("(?:\\n|^)\\s*(?://|#|/\\*\\*?)\\s*@([A-Za-z0-9_.-]+) ?((?!\\n|\\*/|$).+)?")
            while True:
                match = regex.exec(trivia)
                if match == None:
                    break
                if match[1] in result:
                    # @php $result[$match[1]] .= "\n" . $match[2];
                    # @python result[match[1]] += "\n" + match[2]
                    # @csharp result[match[1]] += "\n" + match[2];
                    # @java result.put(match[1], result.get(match[1]) + "\n" + match[2]);
                    result[match[1]] += "\n" + match[2]
                else:
                    result[match[1]] = "true" if (match[2] or "") == "" else match[2] or ""
        return result