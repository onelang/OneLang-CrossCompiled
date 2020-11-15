from OneLangStdLib import *
import OneLang.Generator.IGeneratorPlugin as iGenPlug
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.Statements as stats
import OneLang.One.Ast.AstTypes as astTypes
import OneLang.Generator.PythonGenerator as pythGen
import OneLang.One.Ast.Types as types
import OneLang.One.Ast.References as refs
import OneLang.One.Ast.Interfaces as ints

class JsToPython:
    def __init__(self, main):
        self.unhandled_methods = dict()
        self.main = main
    
    def convert_method(self, cls_, obj, method, args):
        if cls_.name == "TsArray":
            obj_r = self.main.expr(obj)
            args_r = list(map(lambda x: self.main.expr(x), args))
            if method.name == "includes":
                return f'''{args_r[0]} in {obj_r}'''
            elif method.name == "set":
                return f'''{obj_r}[{args_r[0]}] = {args_r[1]}'''
            elif method.name == "get":
                return f'''{obj_r}[{args_r[0]}]'''
            elif method.name == "join":
                return f'''{args_r[0]}.join({obj_r})'''
            elif method.name == "map":
                return f'''list(map({args_r[0]}, {obj_r}))'''
            elif method.name == "push":
                return f'''{obj_r}.append({args_r[0]})'''
            elif method.name == "pop":
                return f'''{obj_r}.pop()'''
            elif method.name == "filter":
                return f'''list(filter({args_r[0]}, {obj_r}))'''
            elif method.name == "every":
                return f'''ArrayHelper.every({args_r[0]}, {obj_r})'''
            elif method.name == "some":
                return f'''ArrayHelper.some({args_r[0]}, {obj_r})'''
            elif method.name == "concat":
                return f'''{obj_r} + {args_r[0]}'''
            elif method.name == "shift":
                return f'''{obj_r}.pop(0)'''
            elif method.name == "find":
                return f'''next(filter({args_r[0]}, {obj_r}), None)'''
        elif cls_.name == "TsString":
            obj_r = self.main.expr(obj)
            args_r = list(map(lambda x: self.main.expr(x), args))
            if method.name == "split":
                if isinstance(args[0], exprs.RegexLiteral):
                    pattern = (args[0]).pattern
                    if not pattern.startswith("^"):
                        #return `${objR}.split(${JSON.stringify(pattern)})`;
                        self.main.imports["import re"] = None
                        return f'''re.split({JSON.stringify(pattern)}, {obj_r})'''
                
                return f'''{args_r[0]}.split({obj_r})'''
            elif method.name == "replace":
                if isinstance(args[0], exprs.RegexLiteral):
                    self.main.imports["import re"] = None
                    return f'''re.sub({JSON.stringify((args[0]).pattern)}, {args_r[1]}, {obj_r})'''
                
                return f'''{args_r[0]}.replace({obj_r}, {args_r[1]})'''
            elif method.name == "includes":
                return f'''{args_r[0]} in {obj_r}'''
            elif method.name == "startsWith":
                return f'''{obj_r}.startswith({", ".join(args_r)})'''
            elif method.name == "indexOf":
                return f'''{obj_r}.find({args_r[0]}, {args_r[1]})'''
            elif method.name == "lastIndexOf":
                return f'''{obj_r}.rfind({args_r[0]}, 0, {args_r[1]})'''
            elif method.name == "substr":
                return f'''{obj_r}[{args_r[0]}:]''' if len(args_r) == 1 else f'''{obj_r}[{args_r[0]}:{args_r[0]} + {args_r[1]}]'''
            elif method.name == "substring":
                return f'''{obj_r}[{args_r[0]}:{args_r[1]}]'''
            elif method.name == "repeat":
                return f'''{obj_r} * ({args_r[0]})'''
            elif method.name == "toUpperCase":
                return f'''{obj_r}.upper()'''
            elif method.name == "toLowerCase":
                return f'''{obj_r}.lower()'''
            elif method.name == "endsWith":
                return f'''{obj_r}.endswith({args_r[0]})'''
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
                return f'''{obj_r}.get({args_r[0]})'''
            elif method.name == "hasKey":
                return f'''{args_r[0]} in {obj_r}'''
        elif cls_.name == "Object":
            args_r = list(map(lambda x: self.main.expr(x), args))
            if method.name == "keys":
                return f'''{args_r[0]}.keys()'''
            elif method.name == "values":
                return f'''{args_r[0]}.values()'''
        elif cls_.name == "Set":
            obj_r = self.main.expr(obj)
            args_r = list(map(lambda x: self.main.expr(x), args))
            if method.name == "values":
                return f'''{obj_r}.keys()'''
            elif method.name == "has":
                return f'''{args_r[0]} in {obj_r}'''
            elif method.name == "add":
                return f'''{obj_r}[{args_r[0]}] = None'''
        elif cls_.name == "ArrayHelper":
            args_r = list(map(lambda x: self.main.expr(x), args))
            if method.name == "sortBy":
                return f'''sorted({args_r[0]}, key={args_r[1]})'''
            elif method.name == "removeLastN":
                return f'''del {args_r[0]}[-{args_r[1]}:]'''
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
                return f'''len({self.main.expr(expr.object)})'''
            if expr.property.parent_class.name == "TsArray" and expr.property.name == "length":
                return f'''len({self.main.expr(expr.object)})'''
        elif isinstance(expr, refs.InstanceFieldReference) and isinstance(expr.object.actual_type, astTypes.ClassType):
            if expr.field.parent_interface.name == "RegExpExecArray" and expr.field.name == "length":
                return f'''len({self.main.expr(expr.object)})'''
        elif isinstance(expr, exprs.StaticMethodCallExpression) and isinstance(expr.method.parent_interface, types.Class):
            return self.convert_method(expr.method.parent_interface, None, expr.method, expr.args)
        return None
    
    def stmt(self, stmt):
        return None