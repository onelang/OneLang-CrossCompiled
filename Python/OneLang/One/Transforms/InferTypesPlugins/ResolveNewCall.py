from onelang_core import *
import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin as inferTypesPlug
import OneLang.One.Ast.Expressions as exprs

class ResolveNewCalls(inferTypesPlug.InferTypesPlugin):
    def __init__(self):
        super().__init__("ResolveNewCalls")
    
    def can_transform(self, expr):
        return isinstance(expr, exprs.NewExpression)
    
    def transform(self, expr):
        new_expr = expr
        i = 0
        
        while i < len(new_expr.args):
            new_expr.args[i].set_expected_type(new_expr.cls_.decl.constructor_.parameters[i].type)
            new_expr.args[i] = self.main.run_plugins_on(new_expr.args[i])
            i = i + 1
        expr.set_actual_type(new_expr.cls_)
        return expr