from onelang_core import *
import OneLang.One.AstTransformer as astTrans
import OneLang.One.Ast.AstTypes as astTypes
import OneLang.One.Ast.Types as types
import OneLang.One.Ast.Interfaces as ints
import OneLang.One.Ast.References as refs

class LambdaCaptureCollector(astTrans.AstTransformer):
    def __init__(self):
        self.scope_var_stack = []
        self.scope_vars = None
        self.captured_vars = None
        super().__init__("LambdaCaptureCollector")
    
    def visit_lambda(self, lambda_):
        if self.scope_vars != None:
            self.scope_var_stack.append(self.scope_vars)
        
        self.scope_vars = dict()
        self.captured_vars = dict()
        
        super().visit_lambda(lambda_)
        lambda_.captures = []
        for capture in self.captured_vars.keys():
            lambda_.captures.append(capture)
        
        self.scope_vars = self.scope_var_stack.pop() if len(self.scope_var_stack) > 0 else None
        return lambda_
    
    def visit_variable(self, variable):
        if self.scope_vars != None:
            self.scope_vars[variable] = None
        return variable
    
    def visit_variable_reference(self, var_ref):
        if isinstance(var_ref, refs.StaticFieldReference) or isinstance(var_ref, refs.InstanceFieldReference) or isinstance(var_ref, refs.StaticPropertyReference) or isinstance(var_ref, refs.InstancePropertyReference) or self.scope_vars == None:
            return var_ref
        
        vari = var_ref.get_variable()
        if not vari in self.scope_vars:
            self.captured_vars[vari] = None
        
        return var_ref