from onelang_core import *
import OneLang.One.AstTransformer as astTrans
import OneLang.One.Ast.AstTypes as astTypes
import OneLang.One.Ast.Types as types
import OneLang.One.Ast.Interfaces as ints

class ResolveGenericTypeIdentifiers(astTrans.AstTransformer):
    def __init__(self):
        super().__init__("ResolveGenericTypeIdentifiers")
    
    def visit_type(self, type):
        super().visit_type(type)
        
        #console.log(type && type.constructor.name, JSON.stringify(type));
        if isinstance(type, astTypes.UnresolvedType) and ((isinstance(self.current_interface, types.Class) and type.type_name in self.current_interface.type_arguments) or (isinstance(self.current_method, types.Method) and type.type_name in self.current_method.type_arguments)):
            return astTypes.GenericsType(type.type_name)
        
        return type