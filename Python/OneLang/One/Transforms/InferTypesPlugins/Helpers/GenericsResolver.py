from OneLangStdLib import *
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.AstTypes as astTypes
import OneLang.One.Ast.Types as types
import OneLang.One.Ast.Interfaces as ints

class GenericsResolver:
    def __init__(self):
        self.resolution_map = Map()
    
    @classmethod
    def from_object(cls, object):
        resolver = GenericsResolver()
        resolver.collect_class_generics_from_object(object)
        return resolver
    
    def add_resolution(self, type_var_name, actual_type):
        prev_res = self.resolution_map.get(type_var_name)
        if prev_res != None and not astTypes.TypeHelper.equals(prev_res, actual_type):
            raise Error(f'''Resolving \'{type_var_name}\' is ambiguous, {prev_res.repr()} <> {actual_type.repr()}''')
        self.resolution_map.set(type_var_name, actual_type)
    
    def collect_from_method_call(self, method_call):
        if len(method_call.type_args) == 0:
            return
        if len(method_call.type_args) != len(method_call.method.type_arguments):
            raise Error(f'''Expected {len(method_call.method.type_arguments)} type argument(s) for method call, but got {len(method_call.type_args)}''')
        i = 0
        
        while i < len(method_call.type_args):
            self.add_resolution(method_call.method.type_arguments[i], method_call.type_args[i])
            i = i + 1
    
    def collect_class_generics_from_object(self, actual_object):
        actual_type = actual_object.get_type()
        if isinstance(actual_type, astTypes.ClassType):
            if not self.collect_resolutions_from_actual_type(actual_type.decl.type, actual_type):
                pass
        elif isinstance(actual_type, astTypes.InterfaceType):
            if not self.collect_resolutions_from_actual_type(actual_type.decl.type, actual_type):
                pass
        else:
            raise Error(f'''Expected ClassType or InterfaceType, got {(actual_type.repr() if actual_type != None else "<null>")}''')
    
    def collect_resolutions_from_actual_type(self, generic_type, actual_type):
        if not astTypes.TypeHelper.is_generic(generic_type):
            return True
        if isinstance(generic_type, astTypes.GenericsType):
            self.add_resolution(generic_type.type_var_name, actual_type)
            return True
        elif isinstance(generic_type, astTypes.ClassType) and isinstance(actual_type, astTypes.ClassType) and generic_type.decl == actual_type.decl:
            if len(generic_type.type_arguments) != len(actual_type.type_arguments):
                raise Error(f'''Same class ({generic_type.repr()}) used with different number of type arguments ({len(generic_type.type_arguments)} <> {len(actual_type.type_arguments)})''')
            return ArrayHelper.every(lambda x, i: self.collect_resolutions_from_actual_type(x, actual_type.type_arguments[i]), generic_type.type_arguments)
        elif isinstance(generic_type, astTypes.InterfaceType) and isinstance(actual_type, astTypes.InterfaceType) and generic_type.decl == actual_type.decl:
            if len(generic_type.type_arguments) != len(actual_type.type_arguments):
                raise Error(f'''Same class ({generic_type.repr()}) used with different number of type arguments ({len(generic_type.type_arguments)} <> {len(actual_type.type_arguments)})''')
            return ArrayHelper.every(lambda x, i: self.collect_resolutions_from_actual_type(x, actual_type.type_arguments[i]), generic_type.type_arguments)
        elif isinstance(generic_type, astTypes.LambdaType) and isinstance(actual_type, astTypes.LambdaType):
            if len(generic_type.parameters) != len(actual_type.parameters):
                raise Error(f'''Generic lambda type has {len(generic_type.parameters)} parameters while the actual type has {len(actual_type.parameters)}''')
            params_ok = ArrayHelper.every(lambda x, i: self.collect_resolutions_from_actual_type(x.type, actual_type.parameters[i].type), generic_type.parameters)
            result_ok = self.collect_resolutions_from_actual_type(generic_type.return_type, actual_type.return_type)
            return params_ok and result_ok
        elif isinstance(generic_type, astTypes.EnumType) and isinstance(actual_type, astTypes.EnumType) and generic_type.decl == actual_type.decl:
            pass
        elif isinstance(generic_type, astTypes.AnyType) or isinstance(actual_type, astTypes.AnyType):
            pass
        else:
            raise Error(f'''Generic type {generic_type.repr()} is not compatible with actual type {actual_type.repr()}''')
        return False
    
    def resolve_type(self, type, must_resolve_all_generics):
        if isinstance(type, astTypes.GenericsType):
            resolved_type = self.resolution_map.get(type.type_var_name)
            if resolved_type == None and must_resolve_all_generics:
                raise Error(f'''Could not resolve generics type: {type.repr()}''')
            return resolved_type if resolved_type != None else type
        elif isinstance(type, astTypes.ClassType):
            return astTypes.ClassType(type.decl, list(map(lambda x: self.resolve_type(x, must_resolve_all_generics), type.type_arguments)))
        elif isinstance(type, astTypes.InterfaceType):
            return astTypes.InterfaceType(type.decl, list(map(lambda x: self.resolve_type(x, must_resolve_all_generics), type.type_arguments)))
        elif isinstance(type, astTypes.LambdaType):
            return astTypes.LambdaType(list(map(lambda x: types.MethodParameter(x.name, self.resolve_type(x.type, must_resolve_all_generics), x.initializer, None), type.parameters)), self.resolve_type(type.return_type, must_resolve_all_generics))
        else:
            return type