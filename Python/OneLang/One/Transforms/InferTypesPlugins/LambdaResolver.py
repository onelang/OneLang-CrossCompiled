from onelang_core import *
import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin as inferTypesPlug
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.Types as types
import OneLang.One.Ast.AstTypes as astTypes

class LambdaResolver(inferTypesPlug.InferTypesPlugin):
    def __init__(self):
        super().__init__("LambdaResolver")
    
    def setup_lambda_parameter_types(self, lambda_):
        if lambda_.expected_type == None:
            return
        
        if isinstance(lambda_.expected_type, astTypes.LambdaType):
            decl_params = lambda_.expected_type.parameters
            if len(decl_params) != len(lambda_.parameters):
                self.error_man.throw(f'''Expected {len(lambda_.parameters)} parameters for lambda, but got {len(decl_params)}!''')
            else:
                i = 0
                
                while i < len(decl_params):
                    if lambda_.parameters[i].type == None:
                        lambda_.parameters[i].type = decl_params[i].type
                    elif not astTypes.TypeHelper.is_assignable_to(lambda_.parameters[i].type, decl_params[i].type):
                        self.error_man.throw(f'''Parameter type {lambda_.parameters[i].type.repr()} cannot be assigned to {decl_params[i].type.repr()}.''')
                    i = i + 1
        else:
            self.error_man.throw("Expected LambdaType as Lambda's type!")
    
    def visit_lambda(self, lambda_):
        self.setup_lambda_parameter_types(lambda_)
    
    def can_transform(self, expr):
        return isinstance(expr, types.Lambda)
    
    def transform(self, expr):
        self.visit_lambda(expr)
        # does not transform actually
        return expr