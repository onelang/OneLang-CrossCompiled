from onelang_core import *
import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin as inferTypesPlug
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.References as refs
import OneLang.One.Ast.AstTypes as astTypes

class ResolveFuncCalls(inferTypesPlug.InferTypesPlugin):
    def __init__(self):
        super().__init__("ResolveFuncCalls")
    
    def can_transform(self, expr):
        return isinstance(expr, exprs.UnresolvedCallExpression)
    
    def transform(self, expr):
        call_expr = expr
        if isinstance(call_expr.func, refs.GlobalFunctionReference):
            new_expr = exprs.GlobalFunctionCallExpression(call_expr.func.decl, call_expr.args)
            call_expr.args = list(map(lambda arg: self.main.run_plugins_on(arg), call_expr.args))
            new_expr.set_actual_type(call_expr.func.decl.returns)
            return new_expr
        else:
            self.main.process_expression(expr)
            if isinstance(call_expr.func.actual_type, astTypes.LambdaType):
                new_expr = exprs.LambdaCallExpression(call_expr.func, call_expr.args)
                new_expr.set_actual_type(call_expr.func.actual_type.return_type)
                return new_expr
            else:
                return expr