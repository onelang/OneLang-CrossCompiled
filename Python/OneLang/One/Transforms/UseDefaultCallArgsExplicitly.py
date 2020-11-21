from onelang_core import *
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.Types as types
import OneLang.One.AstTransformer as astTrans

class UseDefaultCallArgsExplicitly(astTrans.AstTransformer):
    def __init__(self):
        super().__init__("UseDefaultCallArgsExplicitly")
    
    def get_new_args(self, args, method):
        if "UseDefaultCallArgsExplicitly" in method.attributes and method.attributes.get("UseDefaultCallArgsExplicitly") == "disable":
            return args
        if len(args) >= len(method.parameters):
            return args
        
        new_args = []
        i = 0
        
        while i < len(method.parameters):
            init = method.parameters[i].initializer
            if i >= len(args) and init == None:
                self.error_man.throw(f'''Missing default value for parameter #{i + 1}!''')
                break
            new_args.append(args[i] if i < len(args) else init)
            i = i + 1
        return new_args
    
    def visit_expression(self, expr):
        super().visit_expression(expr)
        if isinstance(expr, exprs.NewExpression) and expr.cls_.decl.constructor_ != None:
            expr.args = self.get_new_args(expr.args, expr.cls_.decl.constructor_)
        elif isinstance(expr, exprs.InstanceMethodCallExpression):
            expr.args = self.get_new_args(expr.args, expr.method)
        elif isinstance(expr, exprs.StaticMethodCallExpression):
            expr.args = self.get_new_args(expr.args, expr.method)
        return expr