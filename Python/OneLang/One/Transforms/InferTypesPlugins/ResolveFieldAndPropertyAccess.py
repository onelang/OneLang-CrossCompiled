from OneLangStdLib import *
import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin as inferTypesPlug
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.References as refs
import OneLang.One.Ast.Types as types
import OneLang.One.Ast.AstTypes as astTypes
import OneLang.One.Transforms.InferTypesPlugins.Helpers.GenericsResolver as genRes

class ResolveFieldAndPropertyAccess(inferTypesPlug.InferTypesPlugin):
    def __init__(self):
        super().__init__("ResolveFieldAndPropertyAccess")
    
    def get_static_ref(self, cls_, member_name):
        field = next(filter(lambda x: x.name == member_name, cls_.fields), None)
        if field != None and field.is_static:
            return refs.StaticFieldReference(field)
        
        prop = next(filter(lambda x: x.name == member_name, cls_.properties), None)
        if prop != None and prop.is_static:
            return refs.StaticPropertyReference(prop)
        
        self.error_man.throw(f'''Could not resolve static member access of a class: {cls_.name}::{member_name}''')
        return None
    
    def get_instance_ref(self, cls_, member_name, obj):
        while True:
            field = next(filter(lambda x: x.name == member_name, cls_.fields), None)
            if field != None and not field.is_static:
                return refs.InstanceFieldReference(obj, field)
            
            prop = next(filter(lambda x: x.name == member_name, cls_.properties), None)
            if prop != None and not prop.is_static:
                return refs.InstancePropertyReference(obj, prop)
            
            if cls_.base_class == None:
                break
            
            cls_ = (cls_.base_class).decl
        
        self.error_man.throw(f'''Could not resolve instance member access of a class: {cls_.name}::{member_name}''')
        return None
    
    def get_interface_ref(self, intf, member_name, obj):
        field = next(filter(lambda x: x.name == member_name, intf.fields), None)
        if field != None and not field.is_static:
            return refs.InstanceFieldReference(obj, field)
        
        for base_intf in intf.base_interfaces:
            res = self.get_interface_ref((base_intf).decl, member_name, obj)
            if res != None:
                return res
        return None
    
    def transform_pa(self, expr):
        if isinstance(expr.object, refs.ClassReference):
            return self.get_static_ref(expr.object.decl, expr.property_name)
        
        if isinstance(expr.object, refs.StaticThisReference):
            return self.get_static_ref(expr.object.cls_, expr.property_name)
        
        expr.object = self.main.run_plugins_on(expr.object)
        
        if isinstance(expr.object, refs.ThisReference):
            return self.get_instance_ref(expr.object.cls_, expr.property_name, expr.object)
        
        type = expr.object.get_type()
        if isinstance(type, astTypes.ClassType):
            return self.get_instance_ref(type.decl, expr.property_name, expr.object)
        elif isinstance(type, astTypes.InterfaceType):
            ref = self.get_interface_ref(type.decl, expr.property_name, expr.object)
            if ref == None:
                self.error_man.throw(f'''Could not resolve instance member access of a interface: {type.repr()}::{expr.property_name}''')
            return ref
        elif type == None:
            self.error_man.throw(f'''Type was not inferred yet (prop="{expr.property_name}")''')
        elif isinstance(type, astTypes.AnyType):
            #this.errorMan.throw(`Object has any type (prop="${expr.propertyName}")`);
            expr.set_actual_type(astTypes.AnyType.instance)
        else:
            self.error_man.throw(f'''Expected class as variable type, but got: {type.repr()} (prop="{expr.property_name}")''')
        
        return expr
    
    def can_transform(self, expr):
        return isinstance(expr, exprs.PropertyAccessExpression) and not (isinstance(expr.object, refs.EnumReference)) and not (isinstance(expr.parent_node, exprs.UnresolvedCallExpression) and expr.parent_node.func == expr) and not (isinstance(expr.actual_type, astTypes.AnyType))
    
    def transform(self, expr):
        return self.transform_pa(expr)
    
    def can_detect_type(self, expr):
        return isinstance(expr, refs.InstanceFieldReference) or isinstance(expr, refs.InstancePropertyReference) or isinstance(expr, refs.StaticFieldReference) or isinstance(expr, refs.StaticPropertyReference)
    
    def detect_type(self, expr):
        if isinstance(expr, refs.InstanceFieldReference):
            actual_type = genRes.GenericsResolver.from_object(expr.object).resolve_type(expr.field.type, True)
            expr.set_actual_type(actual_type, False, astTypes.TypeHelper.is_generic(expr.object.actual_type))
            return True
        elif isinstance(expr, refs.InstancePropertyReference):
            actual_type = genRes.GenericsResolver.from_object(expr.object).resolve_type(expr.property.type, True)
            expr.set_actual_type(actual_type)
            return True
        elif isinstance(expr, refs.StaticPropertyReference):
            expr.set_actual_type(expr.decl.type, False, False)
            return True
        elif isinstance(expr, refs.StaticFieldReference):
            expr.set_actual_type(expr.decl.type, False, False)
            return True
        
        return False