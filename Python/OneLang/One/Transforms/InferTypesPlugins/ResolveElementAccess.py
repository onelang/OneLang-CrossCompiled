from OneLangStdLib import *
import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin as inferTypesPlug
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Transforms.InferTypesPlugins.ResolveMethodCalls as resMethCalls
import OneLang.One.Ast.Interfaces as ints
import OneLang.One.Ast.AstTypes as astTypes

class ResolveElementAccess(inferTypesPlug.InferTypesPlugin):
    def __init__(self):
        super().__init__("ResolveElementAccess")
    
    def can_transform(self, expr):
        is_set = isinstance(expr, exprs.BinaryExpression) and isinstance(expr.left, exprs.ElementAccessExpression) and expr.operator in ["="]
        return isinstance(expr, exprs.ElementAccessExpression) or is_set
    
    def is_map_or_array_type(self, type):
        return astTypes.TypeHelper.is_assignable_to(type, self.main.current_file.literal_types.map) or ArrayHelper.some(lambda x: astTypes.TypeHelper.is_assignable_to(type, x), self.main.current_file.array_types)
    
    def transform(self, expr):
        # TODO: convert ElementAccess to ElementGet and ElementSet expressions
        if isinstance(expr, exprs.BinaryExpression) and isinstance(expr.left, exprs.ElementAccessExpression):
            expr.left.object = self.main.run_plugins_on(expr.left.object)
            if self.is_map_or_array_type(expr.left.object.get_type()):
                #const right = expr.operator === "=" ? expr.right : new BinaryExpression(<Expression>expr.left.clone(), expr.operator === "+=" ? "+" : "-", expr.right);
                return exprs.UnresolvedMethodCallExpression(expr.left.object, "set", [], [expr.left.element_expr, expr.right])
        elif isinstance(expr, exprs.ElementAccessExpression):
            expr.object = self.main.run_plugins_on(expr.object)
            if self.is_map_or_array_type(expr.object.get_type()):
                return exprs.UnresolvedMethodCallExpression(expr.object, "get", [], [expr.element_expr])
            elif isinstance(expr.element_expr, exprs.StringLiteral):
                return exprs.PropertyAccessExpression(expr.object, expr.element_expr.string_value)
        return expr