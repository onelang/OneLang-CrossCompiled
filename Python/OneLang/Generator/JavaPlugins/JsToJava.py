from onelang_core import *
import OneLang.Generator.IGeneratorPlugin as iGenPlug
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.Statements as stats
import OneLang.One.Ast.AstTypes as astTypes
import OneLang.One.Ast.Types as types
import OneLang.One.Ast.References as refs
import OneLang.One.Ast.Interfaces as ints
import OneLang.Generator.JavaGenerator as javaGen
import json

class JsToJava:
    def __init__(self, main):
        self.unhandled_methods = dict()
        self.main = main
    
    def is_array(self, array_expr):
        # TODO: InstanceMethodCallExpression is a hack, we should introduce real stream handling
        return isinstance(array_expr, refs.VariableReference) and not array_expr.get_variable().mutability.mutated or isinstance(array_expr, exprs.StaticMethodCallExpression) or isinstance(array_expr, exprs.InstanceMethodCallExpression)
    
    def array_stream(self, array_expr):
        is_array = self.is_array(array_expr)
        obj_r = self.main.expr(array_expr)
        if is_array:
            self.main.imports["java.util.Arrays"] = None
        return f'''Arrays.stream({obj_r})''' if is_array else f'''{obj_r}.stream()'''
    
    def to_array(self, array_type, type_arg_idx = 0):
        type = (array_type).type_arguments[type_arg_idx]
        return f'''toArray({self.main.type(type)}[]::new)'''
    
    def convert_method(self, cls_, obj, method, args, return_type):
        obj_r = None if obj == None else self.main.expr(obj)
        args_r = list(map(lambda x: self.main.expr(x), args))
        if cls_.name == "TsString":
            if method.name == "replace":
                if isinstance(args[0], exprs.RegexLiteral):
                    self.main.imports["java.util.regex.Pattern"] = None
                    return f'''{obj_r}.replaceAll({json.dumps((args[0]).pattern)}, {args_r[1]})'''
                
                return f'''{args_r[0]}.replace({obj_r}, {args_r[1]})'''
        elif cls_.name in ["console", "RegExp"]:
            self.main.imports[f'''io.onelang.std.core.{cls_.name}'''] = None
            return None
        elif cls_.name in ["JSON"]:
            self.main.imports[f'''io.onelang.std.json.{cls_.name}'''] = None
            return None
        else:
            return None
        
        return None
    
    def expr(self, expr):
        if isinstance(expr, exprs.InstanceMethodCallExpression) and isinstance(expr.object.actual_type, astTypes.ClassType):
            return self.convert_method(expr.object.actual_type.decl, expr.object, expr.method, expr.args, expr.actual_type)
        elif isinstance(expr, refs.InstancePropertyReference) and isinstance(expr.object.actual_type, astTypes.ClassType):
            if expr.property.parent_class.name == "TsString" and expr.property.name == "length":
                return f'''{self.main.expr(expr.object)}.length()'''
            if expr.property.parent_class.name == "TsArray" and expr.property.name == "length":
                return f'''{self.main.expr(expr.object)}.{("length" if self.is_array(expr.object) else "size()")}'''
        elif isinstance(expr, refs.InstanceFieldReference) and isinstance(expr.object.actual_type, astTypes.ClassType):
            if expr.field.parent_interface.name == "RegExpExecArray" and expr.field.name == "length":
                return f'''{self.main.expr(expr.object)}.length'''
            if expr.field.parent_interface.name == "Map" and expr.field.name == "size":
                return f'''{self.main.expr(expr.object)}.size()'''
        elif isinstance(expr, exprs.StaticMethodCallExpression) and isinstance(expr.method.parent_interface, types.Class):
            return self.convert_method(expr.method.parent_interface, None, expr.method, expr.args, expr.actual_type)
        return None
    
    def stmt(self, stmt):
        return None