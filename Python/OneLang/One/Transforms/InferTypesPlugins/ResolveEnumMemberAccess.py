from onelang_core import *
import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin as inferTypesPlug
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.References as refs

class ResolveEnumMemberAccess(inferTypesPlug.InferTypesPlugin):
    def __init__(self):
        super().__init__("ResolveEnumMemberAccess")
    
    def can_transform(self, expr):
        return isinstance(expr, exprs.PropertyAccessExpression) and isinstance(expr.object, refs.EnumReference)
    
    def transform(self, expr):
        pa = expr
        enum_member_ref = pa.object
        member = next(filter(lambda x: x.name == pa.property_name, enum_member_ref.decl.values), None)
        if member == None:
            self.error_man.throw(f'''Enum member was not found: {enum_member_ref.decl.name}::{pa.property_name}''')
            return expr
        return refs.EnumMemberReference(member)
    
    def can_detect_type(self, expr):
        return isinstance(expr, refs.EnumMemberReference)
    
    def detect_type(self, expr):
        expr.set_actual_type((expr).decl.parent_enum.type)
        return True