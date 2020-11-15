from OneLangStdLib import *
import OneLang.Generator.IGeneratorPlugin as iGenPlug
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.Statements as stats
import OneLang.One.Ast.AstTypes as astTypes
import OneLang.One.Ast.Types as types
import OneLang.One.Ast.References as refs
import OneLang.One.Ast.Interfaces as ints
import OneLang.Generator.PhpGenerator as phpGen
import re

class JsToPhp:
    def __init__(self, main):
        self.unhandled_methods = dict()
        self.main = main
    
    def convert_method(self, cls_, obj, method, args):
        if cls_.name == "TsArray":
            obj_r = self.main.expr(obj)
            args_r = list(map(lambda x: self.main.expr(x), args))
            if method.name == "includes":
                return f'''in_array({args_r[0]}, {obj_r})'''
            elif method.name == "set":
                return f'''{obj_r}[{args_r[0]}] = {args_r[1]}'''
            elif method.name == "get":
                return f'''{obj_r}[{args_r[0]}]'''
            elif method.name == "join":
                return f'''implode({args_r[0]}, {obj_r})'''
            elif method.name == "map":
                return f'''array_map({args_r[0]}, {obj_r})'''
            elif method.name == "push":
                return f'''{obj_r}[] = {args_r[0]}'''
            elif method.name == "pop":
                return f'''array_pop({obj_r})'''
            elif method.name == "filter":
                return f'''array_values(array_filter({obj_r}, {args_r[0]}))'''
            elif method.name == "every":
                return f'''\\OneLang\\ArrayHelper::every({obj_r}, {args_r[0]})'''
            elif method.name == "some":
                return f'''\\OneLang\\ArrayHelper::some({obj_r}, {args_r[0]})'''
            elif method.name == "concat":
                return f'''array_merge({obj_r}, {args_r[0]})'''
            elif method.name == "shift":
                return f'''array_shift({obj_r})'''
            elif method.name == "find":
                return f'''\\OneLang\\ArrayHelper::find({obj_r}, {args_r[0]})'''
            elif method.name == "sort":
                return f'''sort({obj_r})'''
        elif cls_.name == "TsString":
            obj_r = self.main.expr(obj)
            args_r = list(map(lambda x: self.main.expr(x), args))
            if method.name == "split":
                if isinstance(args[0], exprs.RegexLiteral):
                    pattern = (args[0]).pattern
                    mod_pattern = "/" + re.sub("/", "\\/", pattern) + "/"
                    return f'''preg_split({JSON.stringify(mod_pattern)}, {obj_r})'''
                
                return f'''explode({args_r[0]}, {obj_r})'''
            elif method.name == "replace":
                if isinstance(args[0], exprs.RegexLiteral):
                    return f'''preg_replace({JSON.stringify(f'/{(args[0]).pattern}/')}, {args_r[1]}, {obj_r})'''
                
                return f'''{args_r[0]}.replace({obj_r}, {args_r[1]})'''
            elif method.name == "includes":
                return f'''strpos({obj_r}, {args_r[0]}) !== false'''
            elif method.name == "startsWith":
                if len(args_r) > 1:
                    return f'''substr_compare({obj_r}, {args_r[0]}, {args_r[1]}, strlen({args_r[0]})) === 0'''
                else:
                    return f'''substr_compare({obj_r}, {args_r[0]}, 0, strlen({args_r[0]})) === 0'''
            elif method.name == "endsWith":
                if len(args_r) > 1:
                    return f'''substr_compare({obj_r}, {args_r[0]}, {args_r[1]} - strlen({args_r[0]}), strlen({args_r[0]})) === 0'''
                else:
                    return f'''substr_compare({obj_r}, {args_r[0]}, strlen({obj_r}) - strlen({args_r[0]}), strlen({args_r[0]})) === 0'''
            elif method.name == "indexOf":
                return f'''strpos({obj_r}, {args_r[0]}, {args_r[1]})'''
            elif method.name == "lastIndexOf":
                return f'''strrpos({obj_r}, {args_r[0]}, {args_r[1]} - strlen({obj_r}))'''
            elif method.name == "substr":
                if len(args_r) > 1:
                    return f'''substr({obj_r}, {args_r[0]}, {args_r[1]})'''
                else:
                    return f'''substr({obj_r}, {args_r[0]})'''
            elif method.name == "substring":
                return f'''substr({obj_r}, {args_r[0]}, {args_r[1]} - ({args_r[0]}))'''
            elif method.name == "repeat":
                return f'''str_repeat({obj_r}, {args_r[0]})'''
            elif method.name == "toUpperCase":
                return f'''strtoupper({obj_r})'''
            elif method.name == "toLowerCase":
                return f'''strtolower({obj_r})'''
            elif method.name == "get":
                return f'''{obj_r}[{args_r[0]}]'''
            elif method.name == "charCodeAt":
                return f'''ord({obj_r}[{args_r[0]}])'''
        elif cls_.name == "TsMap":
            obj_r = self.main.expr(obj)
            args_r = list(map(lambda x: self.main.expr(x), args))
            if method.name == "set":
                return f'''{obj_r}[{args_r[0]}] = {args_r[1]}'''
            elif method.name == "get":
                return f'''@{obj_r}[{args_r[0]}] ?? null'''
            elif method.name == "hasKey":
                return f'''array_key_exists({args_r[0]}, {obj_r})'''
        elif cls_.name == "Object":
            args_r = list(map(lambda x: self.main.expr(x), args))
            if method.name == "keys":
                return f'''array_keys({args_r[0]})'''
            elif method.name == "values":
                return f'''array_values({args_r[0]})'''
        elif cls_.name == "ArrayHelper":
            args_r = list(map(lambda x: self.main.expr(x), args))
            if method.name == "sortBy":
                return f'''\\OneLang\\ArrayHelper::sortBy({args_r[0]}, {args_r[1]})'''
            elif method.name == "removeLastN":
                return f'''array_splice({args_r[0]}, -{args_r[1]})'''
        elif cls_.name == "Math":
            args_r = list(map(lambda x: self.main.expr(x), args))
            if method.name == "floor":
                return f'''floor({args_r[0]})'''
        elif cls_.name == "JSON":
            args_r = list(map(lambda x: self.main.expr(x), args))
            if method.name == "stringify":
                return f'''json_encode({args_r[0]}, JSON_UNESCAPED_SLASHES)'''
        elif cls_.name == "RegExpExecArray":
            obj_r = self.main.expr(obj)
            args_r = list(map(lambda x: self.main.expr(x), args))
            return f'''{obj_r}[{args_r[0]}]'''
        else:
            return None
        
        method_name = f'''{cls_.name}.{method.name}'''
        if not method_name in self.unhandled_methods:
            console.error(f'''[JsToPython] Method was not handled: {cls_.name}.{method.name}''')
            self.unhandled_methods[method_name] = None
        #debugger;
        return None
    
    def expr(self, expr):
        if isinstance(expr, exprs.InstanceMethodCallExpression) and isinstance(expr.object.actual_type, astTypes.ClassType):
            return self.convert_method(expr.object.actual_type.decl, expr.object, expr.method, expr.args)
        elif isinstance(expr, refs.InstancePropertyReference) and isinstance(expr.object.actual_type, astTypes.ClassType):
            if expr.property.parent_class.name == "TsString" and expr.property.name == "length":
                return f'''strlen({self.main.expr(expr.object)})'''
            if expr.property.parent_class.name == "TsArray" and expr.property.name == "length":
                return f'''count({self.main.expr(expr.object)})'''
        elif isinstance(expr, refs.InstanceFieldReference) and isinstance(expr.object.actual_type, astTypes.ClassType):
            if expr.field.parent_interface.name == "RegExpExecArray" and expr.field.name == "length":
                return f'''count({self.main.expr(expr.object)})'''
        elif isinstance(expr, exprs.StaticMethodCallExpression) and isinstance(expr.method.parent_interface, types.Class):
            return self.convert_method(expr.method.parent_interface, None, expr.method, expr.args)
        return None
    
    def stmt(self, stmt):
        return None