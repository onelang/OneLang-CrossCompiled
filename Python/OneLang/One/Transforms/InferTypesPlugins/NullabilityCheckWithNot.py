from onelang_core import *
import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin as inferTypesPlug
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.AstTypes as astTypes

class NullabilityCheckWithNot(inferTypesPlug.InferTypesPlugin):
    def __init__(self):
        super().__init__("NullabilityCheckWithNot")
    
    def can_transform(self, expr):
        return expr.operator == "!" if isinstance(expr, exprs.UnaryExpression) else False
    
    def transform(self, expr):
        unary_expr = expr
        if unary_expr.operator == "!":
            self.main.process_expression(expr)
            type = unary_expr.operand.actual_type
            lit_types = self.main.current_file.literal_types
            if isinstance(type, astTypes.ClassType) and type.decl != lit_types.boolean.decl and type.decl != lit_types.numeric.decl:
                return exprs.BinaryExpression(unary_expr.operand, "==", exprs.NullLiteral())
        
        return expr