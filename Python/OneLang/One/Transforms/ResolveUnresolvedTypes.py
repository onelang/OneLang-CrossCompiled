from onelang_core import *
import OneLang.One.AstTransformer as astTrans
import OneLang.One.Ast.AstTypes as astTypes
import OneLang.One.Ast.Types as types
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.Interfaces as ints

class ResolveUnresolvedTypes(astTrans.AstTransformer):
    def __init__(self):
        super().__init__("ResolveUnresolvedTypes")
    
    def visit_type(self, type):
        super().visit_type(type)
        if isinstance(type, astTypes.UnresolvedType):
            if self.current_interface != None and type.type_name in self.current_interface.type_arguments:
                return astTypes.GenericsType(type.type_name)
            
            symbol = self.current_file.available_symbols.get(type.type_name)
            if symbol == None:
                self.error_man.throw(f'''Unresolved type \'{type.type_name}\' was not found in available symbols''')
                return type
            
            if isinstance(symbol, types.Class):
                return astTypes.ClassType(symbol, type.type_arguments)
            elif isinstance(symbol, types.Interface):
                return astTypes.InterfaceType(symbol, type.type_arguments)
            elif isinstance(symbol, types.Enum):
                return astTypes.EnumType(symbol)
            else:
                self.error_man.throw(f'''Unknown symbol type: {symbol}''')
                return type
        else:
            return type
    
    def visit_expression(self, expr):
        if isinstance(expr, exprs.UnresolvedNewExpression):
            cls_type = self.visit_type(expr.cls_)
            if isinstance(cls_type, astTypes.ClassType):
                new_expr = exprs.NewExpression(cls_type, expr.args)
                new_expr.parent_node = expr.parent_node
                super().visit_expression(new_expr)
                return new_expr
            else:
                self.error_man.throw(f'''Excepted ClassType, but got {cls_type}''')
                return expr
        else:
            return super().visit_expression(expr)