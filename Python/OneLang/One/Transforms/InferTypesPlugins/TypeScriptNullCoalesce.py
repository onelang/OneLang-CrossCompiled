from OneLangStdLib import *
import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin as inferTypesPlug
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.AstTypes as astTypes

class TypeScriptNullCoalesce(inferTypesPlug.InferTypesPlugin):
    def __init__(self):
        super().__init__("TypeScriptNullCoalesce")
    
    def can_transform(self, expr):
        return isinstance(expr, exprs.BinaryExpression) and expr.operator == "||"
    
    def transform(self, expr):
        if isinstance(expr, exprs.BinaryExpression) and expr.operator == "||":
            lit_types = self.main.current_file.literal_types
            
            expr.left = self.main.run_plugins_on(expr.left)
            left_type = expr.left.get_type()
            
            if isinstance(expr.right, exprs.ArrayLiteral) and len(expr.right.items) == 0:
                if isinstance(left_type, astTypes.ClassType) and left_type.decl == lit_types.array.decl:
                    expr.right.set_actual_type(left_type)
                    return exprs.NullCoalesceExpression(expr.left, expr.right)
            
            if isinstance(expr.right, exprs.MapLiteral) and len(expr.right.items) == 0:
                if isinstance(left_type, astTypes.ClassType) and left_type.decl == lit_types.map.decl:
                    expr.right.set_actual_type(left_type)
                    return exprs.NullCoalesceExpression(expr.left, expr.right)
            
            expr.right = self.main.run_plugins_on(expr.right)
            right_type = expr.right.get_type()
            
            if isinstance(expr.right, exprs.NullLiteral):
                # something-which-can-be-undefined || null
                return expr.left
            elif astTypes.TypeHelper.is_assignable_to(right_type, left_type) and not astTypes.TypeHelper.equals(right_type, self.main.current_file.literal_types.boolean):
                return exprs.NullCoalesceExpression(expr.left, expr.right)
        return expr