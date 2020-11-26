from onelang_core import *
import OneLang.Generator.IGeneratorPlugin as iGenPlug
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.Statements as stats
import OneLang.One.Ast.AstTypes as astTypes
import OneLang.One.Ast.Types as types
import OneLang.One.Ast.Interfaces as ints
import OneLang.Generator.JavaGenerator as javaGen
import json

class JsToJava:
    def __init__(self, main):
        self.unhandled_methods = dict()
        self.main = main
    
    def convert_method(self, cls_, obj, method, args):
        obj_r = None if obj == None else self.main.expr(obj)
        args_r = list(map(lambda x: self.main.expr(x), args))
        if cls_.name == "TsString":
            if method.name == "replace":
                if isinstance(args[0], exprs.RegexLiteral):
                    self.main.imports["java.util.regex.Pattern"] = None
                    return f'''{obj_r}.replaceAll({json.dumps((args[0]).pattern)}, {args_r[1]})'''
                
                return f'''{args_r[0]}.replace({obj_r}, {args_r[1]})'''
        else:
            return None
        
        return None
    
    def expr(self, expr):
        if isinstance(expr, exprs.InstanceMethodCallExpression) and isinstance(expr.object.actual_type, astTypes.ClassType):
            return self.convert_method(expr.object.actual_type.decl, expr.object, expr.method, expr.args)
        elif isinstance(expr, exprs.StaticMethodCallExpression) and isinstance(expr.method.parent_interface, types.Class):
            return self.convert_method(expr.method.parent_interface, None, expr.method, expr.args)
        return None
    
    def stmt(self, stmt):
        return None