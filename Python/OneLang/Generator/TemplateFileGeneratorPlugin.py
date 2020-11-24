from onelang_core import *
import OneLang.index as index
import OneLang.Parsers.Common.ExpressionParser as exprPars
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.Interfaces as ints
import OneLang.One.Ast.Statements as stats
import OneLang.Generator.IGeneratorPlugin as iGenPlug
import OneLang.Parsers.Common.Reader as read
import OneLang.VM.Values as vals
import OneLang.One.Ast.References as refs
import OneLang.One.Ast.Types as types
import OneLang.Template.TemplateParser as templPars
import OneLang.Generator.IGenerator as iGen
import OneLang.Template.Nodes as nodes

class CodeTemplate:
    def __init__(self, template, includes):
        self.template = template
        self.includes = includes

class MethodCallTemplate:
    def __init__(self, class_name, method_name, args, template):
        self.class_name = class_name
        self.method_name = method_name
        self.args = args
        self.template = template

class FieldAccessTemplate:
    def __init__(self, class_name, field_name, template):
        self.class_name = class_name
        self.field_name = field_name
        self.template = template

class ExpressionValue:
    def __init__(self, value):
        self.value = value

class LambdaValue:
    def __init__(self, callback):
        self.callback = callback
    
    def call(self, args):
        return self.callback(args)

class TemplateFileGeneratorPlugin:
    def __init__(self, generator, template_yaml):
        self.methods = {}
        self.fields = {}
        self.model_globals = {}
        self.generator = generator
        root = index.OneYaml.load(template_yaml)
        expr_dict = root.dict("expressions")
        
        for expr_str in expr_dict.keys():
            val = expr_dict.get(expr_str)
            tmpl = CodeTemplate(val.as_str(), []) if val.type() == index.VALUE_TYPE.STRING else CodeTemplate(val.str("template"), val.str_arr("includes"))
            
            self.add_expr_template(expr_str, tmpl)
    
    def format_value(self, value):
        if isinstance(value, ExpressionValue):
            result = self.generator.expr(value.value)
            return result
        return None
    
    def add_expr_template(self, expr_str, tmpl):
        expr = exprPars.ExpressionParser(read.Reader(expr_str)).parse()
        if isinstance(expr, exprs.UnresolvedCallExpression) and isinstance(expr.func, exprs.PropertyAccessExpression) and isinstance(expr.func.object, exprs.Identifier):
            call_tmpl = MethodCallTemplate(expr.func.object.text, expr.func.property_name, list(map(lambda x: (x).text, expr.args)), tmpl)
            self.methods[f'''{call_tmpl.class_name}.{call_tmpl.method_name}@{len(call_tmpl.args)}'''] = call_tmpl
        elif isinstance(expr, exprs.PropertyAccessExpression) and isinstance(expr.object, exprs.Identifier):
            field_tmpl = FieldAccessTemplate(expr.object.text, expr.property_name, tmpl)
            self.fields[f'''{field_tmpl.class_name}.{field_tmpl.field_name}'''] = field_tmpl
        else:
            raise Error(f'''This expression template format is not supported: \'{expr_str}\'''')
    
    def expr(self, expr):
        code_tmpl = None
        model = {}
        
        if isinstance(expr, exprs.StaticMethodCallExpression) or isinstance(expr, exprs.InstanceMethodCallExpression):
            call = expr
            method_name = f'''{call.method.parent_interface.name}.{call.method.name}@{len(call.args)}'''
            call_tmpl = self.methods.get(method_name)
            if call_tmpl == None:
                return None
            
            if isinstance(expr, exprs.InstanceMethodCallExpression):
                model["this"] = ExpressionValue(expr.object)
            i = 0
            
            while i < len(call_tmpl.args):
                model[call_tmpl.args[i]] = ExpressionValue(call.args[i])
                i = i + 1
            code_tmpl = call_tmpl.template
        elif isinstance(expr, refs.StaticFieldReference) or isinstance(expr, refs.StaticPropertyReference) or isinstance(expr, refs.InstanceFieldReference) or isinstance(expr, refs.InstancePropertyReference):
            cm = (expr).get_variable()
            field = self.fields.get(f'''{cm.get_parent_interface().name}.{cm.name}''')
            if field == None:
                return None
            
            if isinstance(expr, refs.InstanceFieldReference) or isinstance(expr, refs.InstancePropertyReference):
                model["this"] = ExpressionValue((expr).object)
            code_tmpl = field.template
        else:
            return None
        
        for name in self.model_globals.keys():
            model[name] = self.model_globals.get(name)
        
        for inc in code_tmpl.includes or []:
            self.generator.add_include(inc)
        
        tmpl = templPars.TemplateParser(code_tmpl.template).parse()
        result = tmpl.format(nodes.TemplateContext(vals.ObjectValue(model), self))
        return result
    
    def stmt(self, stmt):
        return None