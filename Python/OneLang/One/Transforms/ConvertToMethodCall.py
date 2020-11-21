from onelang_core import *
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.AstTransformer as astTrans

class ConvertToMethodCall(astTrans.AstTransformer):
    def __init__(self):
        super().__init__("ConvertToMethodCall")
    
    def visit_expression(self, expr):
        orig_expr = expr
        
        expr = super().visit_expression(expr)
        
        if isinstance(expr, exprs.BinaryExpression) and expr.operator == "in":
            expr = exprs.UnresolvedCallExpression(exprs.PropertyAccessExpression(expr.right, "hasKey"), [], [expr.left])
        
        expr.parent_node = orig_expr.parent_node
        return expr