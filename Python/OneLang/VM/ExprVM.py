from onelang_core import *
import OneLang.One.Ast.Expressions as exprs
import OneLang.VM.Values as vals

class VMContext:
    def __init__(self, model, hooks = None):
        self.model = model
        self.hooks = hooks

class ExprVM:
    def __init__(self, context):
        self.context = context
    
    @classmethod
    def prop_access(cls, obj, prop_name):
        if not (isinstance(obj, vals.ObjectValue)):
            raise Error("You can only access a property of an object!")
        if not (prop_name in (obj).props):
            raise Error(f'''Property \'{prop_name}\' does not exists on this object!''')
        return (obj).props.get(prop_name)
    
    def evaluate(self, expr):
        if isinstance(expr, exprs.Identifier):
            return ExprVM.prop_access(self.context.model, expr.text)
        elif isinstance(expr, exprs.PropertyAccessExpression):
            obj_value = self.evaluate(expr.object)
            return ExprVM.prop_access(obj_value, expr.property_name)
        elif isinstance(expr, exprs.UnresolvedCallExpression):
            func = self.evaluate(expr.func)
            args = list(map(lambda x: self.evaluate(x), expr.args))
            result = func.call(args)
            return result
        elif isinstance(expr, exprs.StringLiteral):
            return vals.StringValue(expr.string_value)
        elif isinstance(expr, exprs.ConditionalExpression):
            cond_result = self.evaluate(expr.condition)
            result = self.evaluate(expr.when_true if (cond_result).value else expr.when_false)
            return result
        elif isinstance(expr, exprs.TemplateString):
            result = ""
            for part in expr.parts:
                if part.is_literal:
                    result += part.literal_text
                else:
                    value = self.evaluate(part.expression)
                    result += value.value if isinstance(value, vals.StringValue) else self.context.hooks.stringify_value(value)
            return vals.StringValue(result)
        else:
            raise Error("Unsupported expression!")