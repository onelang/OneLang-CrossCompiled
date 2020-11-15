from OneLangStdLib import *
import OneLang.One.AstTransformer as astTrans
import OneLang.One.Ast.Types as types
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.References as refs
import OneLang.One.Ast.Statements as stats

class FillMutabilityInfo(astTrans.AstTransformer):
    def __init__(self):
        super().__init__("FillMutabilityInfo")
    
    def get_var(self, var_ref):
        v = var_ref.get_variable()
        if v.mutability == None:
            v.mutability = types.MutabilityInfo(True, False, False)
        return v
    
    def visit_variable_reference(self, var_ref):
        self.get_var(var_ref).mutability.unused = False
        return var_ref
    
    def visit_variable_declaration(self, stmt):
        super().visit_variable_declaration(stmt)
        if stmt.attributes != None and stmt.attributes.get("mutated") == "true":
            stmt.mutability.mutated = True
        return stmt
    
    def visit_expression(self, expr):
        expr = super().visit_expression(expr)
        
        if isinstance(expr, exprs.BinaryExpression) and isinstance(expr.left, refs.VariableReference) and expr.operator == "=":
            self.get_var(expr.left).mutability.reassigned = True
        elif isinstance(expr, exprs.InstanceMethodCallExpression) and isinstance(expr.object, refs.VariableReference) and "mutates" in expr.method.attributes:
            self.get_var(expr.object).mutability.mutated = True
        return expr
    
    def visit_variable(self, variable):
        if variable.mutability == None:
            variable.mutability = types.MutabilityInfo(True, False, False)
        return variable