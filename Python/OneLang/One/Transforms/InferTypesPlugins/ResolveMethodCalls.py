from onelang_core import *
import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin as inferTypesPlug
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.AstTypes as astTypes
import OneLang.One.Transforms.InferTypesPlugins.Helpers.GenericsResolver as genRes
import OneLang.One.Ast.References as refs
import OneLang.One.Ast.Types as types

class ResolveMethodCalls(inferTypesPlug.InferTypesPlugin):
    def __init__(self):
        super().__init__("ResolveMethodCalls")
    
    def find_method(self, cls_, method_name, is_static, args):
        all_bases = list(filter(lambda x: isinstance(x, types.Class), cls_.get_all_base_interfaces())) if isinstance(cls_, types.Class) else cls_.get_all_base_interfaces()
        
        methods = []
        for base in all_bases:
            for m in base.methods:
                min_len = len(list(filter(lambda p: p.initializer == None, m.parameters)))
                max_len = len(m.parameters)
                match = m.name == method_name and m.is_static == is_static and min_len <= len(args) and len(args) <= max_len
                if match:
                    methods.append(m)
        
        if len(methods) == 0:
            raise Error(f'''Method \'{method_name}\' was not found on type \'{cls_.name}\' with {len(args)} arguments''')
        elif len(methods) > 1:
            # TODO: actually we should implement proper method shadowing here...
            this_methods = list(filter(lambda x: x.parent_interface == cls_, methods))
            if len(this_methods) == 1:
                return this_methods[0]
            raise Error(f'''Multiple methods found with name \'{method_name}\' and {len(args)} arguments on type \'{cls_.name}\'''')
        return methods[0]
    
    def resolve_return_type(self, expr, generics_resolver):
        generics_resolver.collect_from_method_call(expr)
        
        i = 0
        
        while i < len(expr.args):
            # actually doesn't have to resolve, but must check if generic type confirm the previous argument with the same generic type
            param_type = generics_resolver.resolve_type(expr.method.parameters[i].type, False)
            if param_type != None:
                expr.args[i].set_expected_type(param_type)
            expr.args[i] = self.main.run_plugins_on(expr.args[i])
            generics_resolver.collect_resolutions_from_actual_type(param_type, expr.args[i].actual_type)
            i = i + 1
        
        if expr.method.returns == None:
            self.error_man.throw(f'''Method ({expr.method.parent_interface.name}::{expr.method.name}) return type was not specified or infered before the call.''')
            return
        
        expr.set_actual_type(generics_resolver.resolve_type(expr.method.returns, True), True, isinstance(expr, exprs.InstanceMethodCallExpression) and astTypes.TypeHelper.is_generic(expr.object.get_type()))
    
    def transform_method_call(self, expr):
        if isinstance(expr.object, refs.ClassReference) or isinstance(expr.object, refs.StaticThisReference):
            cls_ = expr.object.decl if isinstance(expr.object, refs.ClassReference) else expr.object.cls_ if isinstance(expr.object, refs.StaticThisReference) else None
            method = self.find_method(cls_, expr.method_name, True, expr.args)
            result = exprs.StaticMethodCallExpression(method, expr.type_args, expr.args, isinstance(expr.object, refs.StaticThisReference))
            self.resolve_return_type(result, genRes.GenericsResolver())
            return result
        else:
            resolved_object = expr.object if expr.object.actual_type != None else self.main.run_plugins_on(expr.object)
            object_type = resolved_object.get_type()
            intf_type = object_type.decl if isinstance(object_type, astTypes.ClassType) else object_type.decl if isinstance(object_type, astTypes.InterfaceType) else None
            
            if intf_type != None:
                method = self.find_method(intf_type, expr.method_name, False, expr.args)
                result = exprs.InstanceMethodCallExpression(resolved_object, method, expr.type_args, expr.args)
                self.resolve_return_type(result, genRes.GenericsResolver.from_object(resolved_object))
                return result
            elif isinstance(object_type, astTypes.AnyType):
                expr.set_actual_type(astTypes.AnyType.instance)
                return expr
            else:
                pass
            return resolved_object
    
    def can_transform(self, expr):
        return isinstance(expr, exprs.UnresolvedMethodCallExpression) and not (isinstance(expr.actual_type, astTypes.AnyType))
    
    def transform(self, expr):
        return self.transform_method_call(expr)