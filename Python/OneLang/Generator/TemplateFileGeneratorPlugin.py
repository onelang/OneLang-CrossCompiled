from onelang_core import *
from onelang_yaml import *
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
import OneLang.VM.ExprVM as exprVM
import OneLang.Parsers.TypeScriptParser as typeScrPars
import OneLang.One.Ast.AstTypes as astTypes

class CodeTemplate:
    def __init__(self, template, includes, if_expr):
        self.template = template
        self.includes = includes
        self.if_expr = if_expr

class CallTemplate:
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
    
    def equals(self, other):
        return isinstance(other, ExpressionValue) and other.value == self.value

class TypeValue:
    def __init__(self, type):
        self.type = type
    
    def equals(self, other):
        return isinstance(other, TypeValue) and astTypes.TypeHelper.equals(other.type, self.type)

class LambdaValue:
    def __init__(self, callback):
        self.callback = callback
    
    def equals(self, other):
        return False
    
    def call(self, args):
        return self.callback(args)

class TemplateFileGeneratorPlugin:
    def __init__(self, generator, template_yaml):
        self.methods = {}
        self.fields = {}
        self.model_globals = {}
        self.generator = generator
        root = OneYaml.load(template_yaml)
        expr_dict = root.dict("expressions")
        if expr_dict == None:
            return
        
        for expr_str in expr_dict.keys():
            val = expr_dict.get(expr_str)
            tmpl_only = val.type() == VALUE_TYPE.STRING
            if_str = None if tmpl_only else val.str("if")
            if_expr = None if if_str == None else typeScrPars.TypeScriptParser2(if_str, None).parse_expression()
            tmpl = CodeTemplate(val.as_str(), [], None) if tmpl_only else CodeTemplate(val.str("template"), val.str_arr("includes"), if_expr)
            
            self.add_expr_template(expr_str, tmpl)
    
    def prop_access(self, obj, prop_name):
        if isinstance(obj, ExpressionValue) and prop_name == "type":
            return TypeValue(obj.value.get_type())
        if isinstance(obj, TypeValue) and prop_name == "name" and isinstance(obj.type, astTypes.ClassType):
            return vals.StringValue(obj.type.decl.name)
        return None
    
    def stringify_value(self, value):
        if isinstance(value, ExpressionValue):
            result = self.generator.expr(value.value)
            return result
        return None
    
    def add_method(self, name, call_tmpl):
        if not (name in self.methods):
            self.methods[name] = []
        # @php $this->methods[$name][] = $callTmpl;
        self.methods.get(name).append(call_tmpl)
    
    def add_expr_template(self, expr_str, tmpl):
        expr = typeScrPars.TypeScriptParser2(expr_str, None).parse_expression()
        if isinstance(expr, exprs.UnresolvedCallExpression) and isinstance(expr.func, exprs.PropertyAccessExpression) and isinstance(expr.func.object, exprs.Identifier):
            call_tmpl = CallTemplate(expr.func.object.text, expr.func.property_name, list(map(lambda x: (x).text, expr.args)), tmpl)
            self.add_method(f'''{call_tmpl.class_name}.{call_tmpl.method_name}@{len(call_tmpl.args)}''', call_tmpl)
        elif isinstance(expr, exprs.UnresolvedCallExpression) and isinstance(expr.func, exprs.Identifier):
            call_tmpl = CallTemplate(None, expr.func.text, list(map(lambda x: (x).text, expr.args)), tmpl)
            self.add_method(f'''{call_tmpl.method_name}@{len(call_tmpl.args)}''', call_tmpl)
        elif isinstance(expr, exprs.PropertyAccessExpression) and isinstance(expr.object, exprs.Identifier):
            field_tmpl = FieldAccessTemplate(expr.object.text, expr.property_name, tmpl)
            self.fields[f'''{field_tmpl.class_name}.{field_tmpl.field_name}'''] = field_tmpl
        elif isinstance(expr, exprs.UnresolvedNewExpression) and isinstance(expr.cls_, astTypes.UnresolvedType):
            call_tmpl = CallTemplate(expr.cls_.type_name, "constructor", list(map(lambda x: (x).text, expr.args)), tmpl)
            self.add_method(f'''{call_tmpl.class_name}.{call_tmpl.method_name}@{len(call_tmpl.args)}''', call_tmpl)
        else:
            raise Error(f'''This expression template format is not supported: \'{expr_str}\'''')
    
    def expr(self, expr):
        is_call_expr = isinstance(expr, exprs.StaticMethodCallExpression) or isinstance(expr, exprs.InstanceMethodCallExpression) or isinstance(expr, exprs.GlobalFunctionCallExpression) or isinstance(expr, exprs.NewExpression)
        is_field_ref = isinstance(expr, refs.StaticFieldReference) or isinstance(expr, refs.StaticPropertyReference) or isinstance(expr, refs.InstanceFieldReference) or isinstance(expr, refs.InstancePropertyReference)
        
        if not is_call_expr and not is_field_ref:
            return None
        # quick return
        
        code_tmpl = None
        model = vals.ObjectValue({})
        context = exprVM.VMContext(model, self)
        
        model.props["type"] = TypeValue(expr.get_type())
        for name in self.model_globals.keys():
            model.props[name] = self.model_globals.get(name)
        
        if is_call_expr:
            call = expr
            parent_intf = call.get_parent_interface()
            method_name = f'''{("" if parent_intf == None else f'{parent_intf.name}.')}{call.get_method_name()}@{len(call.args)}'''
            call_tmpls = self.methods.get(method_name)
            if call_tmpls == None:
                return None
            
            for call_tmpl in call_tmpls:
                if isinstance(expr, exprs.InstanceMethodCallExpression):
                    model.props["this"] = ExpressionValue(expr.object)
                i = 0
                
                while i < len(call_tmpl.args):
                    model.props[call_tmpl.args[i]] = ExpressionValue(call.args[i])
                    i = i + 1
                
                if call_tmpl.template.if_expr == None or (exprVM.ExprVM(context).evaluate(call_tmpl.template.if_expr)).value:
                    code_tmpl = call_tmpl.template
                    break
        elif is_field_ref:
            cm = (expr).get_variable()
            field = self.fields.get(f'''{cm.get_parent_interface().name}.{cm.name}''')
            if field == None:
                return None
            
            if isinstance(expr, refs.InstanceFieldReference) or isinstance(expr, refs.InstancePropertyReference):
                model.props["this"] = ExpressionValue((expr).object)
            code_tmpl = field.template
        else:
            return None
        
        if code_tmpl == None:
            return None
        
        for inc in code_tmpl.includes or []:
            self.generator.add_include(inc)
        
        tmpl = templPars.TemplateParser(code_tmpl.template).parse()
        result = tmpl.format(context)
        return result
    
    def stmt(self, stmt):
        return None