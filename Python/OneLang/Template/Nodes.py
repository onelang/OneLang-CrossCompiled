from onelang_core import *
import OneLang.One.Ast.Expressions as exprs
import OneLang.Utils.TSOverviewGenerator as tSOvervGen
import OneLang.VM.ExprVM as exprVM
import OneLang.VM.Values as vals

class TemplateBlock:
    def __init__(self, items):
        self.items = items
    
    def format(self, context):
        return "".join(list(map(lambda x: x.format(context), self.items)))

class LiteralNode:
    def __init__(self, value):
        self.value = value
    
    def format(self, context):
        return self.value

class ExpressionNode:
    def __init__(self, expr):
        self.expr = expr
    
    def format(self, context):
        value = exprVM.ExprVM(context).evaluate(self.expr)
        if isinstance(value, vals.StringValue):
            return value.value
        
        if context.hooks != None:
            result = context.hooks.stringify_value(value)
            if result != None:
                return result
        
        raise Error(f'''ExpressionNode ({tSOvervGen.TSOverviewGenerator.preview.expr(self.expr)}) return a non-string result!''')

class ForNode:
    def __init__(self, variable_name, items_expr, body, joiner):
        self.variable_name = variable_name
        self.items_expr = items_expr
        self.body = body
        self.joiner = joiner
    
    def format(self, context):
        items = exprVM.ExprVM(context).evaluate(self.items_expr)
        if not (isinstance(items, vals.ArrayValue)):
            raise Error(f'''ForNode items ({tSOvervGen.TSOverviewGenerator.preview.expr(self.items_expr)}) return a non-array result!''')
        
        result = ""
        for item in (items).items:
            if self.joiner != None and result != "":
                result += self.joiner
            
            context.model.props[self.variable_name] = item
            result += self.body.format(context)
        /* unset context.model.props.get(self.variable_name); */
        return result