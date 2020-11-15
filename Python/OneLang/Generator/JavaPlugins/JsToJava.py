from OneLangStdLib import *
import OneLang.Generator.IGeneratorPlugin as iGenPlug
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.Statements as stats
import OneLang.One.Ast.AstTypes as astTypes
import OneLang.One.Ast.Types as types
import OneLang.One.Ast.References as refs
import OneLang.One.Ast.Interfaces as ints
import OneLang.Generator.JavaGenerator as javaGen

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
        if cls_.name == "TsArray":
            if method.name == "includes":
                return f'''{self.array_stream(obj)}.anyMatch({args_r[0]}::equals)'''
            elif method.name == "set":
                if self.is_array(obj):
                    return f'''{obj_r}[{args_r[0]}] = {args_r[1]}'''
                else:
                    return f'''{obj_r}.set({args_r[0]}, {args_r[1]})'''
            elif method.name == "get":
                return f'''{obj_r}[{args_r[0]}]''' if self.is_array(obj) else f'''{obj_r}.get({args_r[0]})'''
            elif method.name == "join":
                self.main.imports["java.util.stream.Collectors"] = None
                return f'''{self.array_stream(obj)}.collect(Collectors.joining({args_r[0]}))'''
            elif method.name == "map":
                #if (returnType.repr() !== "C:TsArray<C:TsString>") debugger;
                return f'''{self.array_stream(obj)}.map({args_r[0]}).{self.to_array(return_type)}'''
            elif method.name == "push":
                return f'''{obj_r}.add({args_r[0]})'''
            elif method.name == "pop":
                return f'''{obj_r}.remove({obj_r}.size() - 1)'''
            elif method.name == "filter":
                return f'''{self.array_stream(obj)}.filter({args_r[0]}).{self.to_array(return_type)}'''
            elif method.name == "every":
                self.main.imports["OneStd.StdArrayHelper"] = None
                return f'''StdArrayHelper.allMatch({obj_r}, {args_r[0]})'''
            elif method.name == "some":
                return f'''{self.array_stream(obj)}.anyMatch({args_r[0]})'''
            elif method.name == "concat":
                self.main.imports["java.util.stream.Stream"] = None
                return f'''Stream.of({obj_r}, {args_r[0]}).flatMap(Stream::of).{self.to_array(obj.get_type())}'''
            elif method.name == "shift":
                return f'''{obj_r}.remove(0)'''
            elif method.name == "find":
                return f'''{self.array_stream(obj)}.filter({args_r[0]}).findFirst().orElse(null)'''
            elif method.name == "sort":
                self.main.imports["java.util.Collections"] = None
                return f'''Collections.sort({obj_r})'''
        elif cls_.name == "TsString":
            if method.name == "replace":
                if isinstance(args[0], exprs.RegexLiteral):
                    self.main.imports["java.util.regex.Pattern"] = None
                    return f'''{obj_r}.replaceAll({JSON.stringify((args[0]).pattern)}, {args_r[1]})'''
                
                return f'''{args_r[0]}.replace({obj_r}, {args_r[1]})'''
            elif method.name == "charCodeAt":
                return f'''(int){obj_r}.charAt({args_r[0]})'''
            elif method.name == "includes":
                return f'''{obj_r}.contains({args_r[0]})'''
            elif method.name == "get":
                return f'''{obj_r}.substring({args_r[0]}, {args_r[0]} + 1)'''
            elif method.name == "substr":
                return f'''{obj_r}.substring({args_r[0]})''' if len(args_r) == 1 else f'''{obj_r}.substring({args_r[0]}, {args_r[0]} + {args_r[1]})'''
            elif method.name == "substring":
                return f'''{obj_r}.substring({args_r[0]}, {args_r[1]})'''
            
            if method.name == "split" and isinstance(args[0], exprs.RegexLiteral):
                pattern = (args[0]).pattern
                return f'''{obj_r}.split({JSON.stringify(pattern)}, -1)'''
        elif cls_.name == "TsMap" or cls_.name == "Map":
            if method.name == "set":
                return f'''{obj_r}.put({args_r[0]}, {args_r[1]})'''
            elif method.name == "get":
                return f'''{obj_r}.get({args_r[0]})'''
            elif method.name == "hasKey" or method.name == "has":
                return f'''{obj_r}.containsKey({args_r[0]})'''
            elif method.name == "delete":
                return f'''{obj_r}.remove({args_r[0]})'''
            elif method.name == "values":
                return f'''{obj_r}.values().{self.to_array(obj.get_type(), 1)}'''
        elif cls_.name == "Object":
            if method.name == "keys":
                return f'''{args_r[0]}.keySet().toArray(String[]::new)'''
            elif method.name == "values":
                return f'''{args_r[0]}.values().{self.to_array(args[0].get_type())}'''
        elif cls_.name == "Set":
            if method.name == "values":
                return f'''{obj_r}.{self.to_array(obj.get_type())}'''
            elif method.name == "has":
                return f'''{obj_r}.contains({args_r[0]})'''
            elif method.name == "add":
                return f'''{obj_r}.add({args_r[0]})'''
        elif cls_.name == "ArrayHelper":
            pass
        elif cls_.name == "Array":
            if method.name == "from":
                return f'''{args_r[0]}'''
        elif cls_.name == "Promise":
            if method.name == "resolve":
                return f'''{args_r[0]}'''
        elif cls_.name == "RegExpExecArray":
            if method.name == "get":
                return f'''{obj_r}[{args_r[0]}]'''
        elif cls_.name in ["JSON", "console", "RegExp"]:
            self.main.imports[f'''OneStd.{cls_.name}'''] = None
            return None
        else:
            return None
        
        method_name = f'''{cls_.name}.{method.name}'''
        if not method_name in self.unhandled_methods:
            console.error(f'''[JsToJava] Method was not handled: {cls_.name}.{method.name}''')
            self.unhandled_methods[method_name] = None
        #debugger;
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