from OneLangStdLib import *
from enum import Enum
import OneLang.One.Ast.AstTypes as astTypes
import OneLang.One.Ast.Interfaces as ints

class TYPE_RESTRICTION(Enum):
    NO_RESTRICTION = 1
    SHOULD_NOT_HAVE_TYPE = 2
    MUST_BE_GENERIC = 3
    SHOULD_NOT_BE_GENERIC = 4

class UNARY_TYPE(Enum):
    POSTFIX = 1
    PREFIX = 2

class Expression:
    def __init__(self):
        self.parent_node = None
        self.expected_type = None
        self.actual_type = None
    
    def type_check(self, type, allow_void):
        if type == None:
            raise Error("New type cannot be null!")
        
        if isinstance(type, astTypes.VoidType) and not allow_void:
            raise Error("Expression's type cannot be VoidType!")
        
        if isinstance(type, astTypes.UnresolvedType):
            raise Error("Expression's type cannot be UnresolvedType!")
    
    def set_actual_type(self, actual_type, allow_void = False, allow_generic = False):
        if self.actual_type != None:
            raise Error(f'''Expression already has actual type (current type = {self.actual_type.repr()}, new type = {actual_type.repr()})''')
        
        self.type_check(actual_type, allow_void)
        
        if self.expected_type != None and not astTypes.TypeHelper.is_assignable_to(actual_type, self.expected_type):
            raise Error(f'''Actual type ({actual_type.repr()}) is not assignable to the declared type ({self.expected_type.repr()})!''')
        
        # TODO: decide if this check needed or not
        #if (!allowGeneric && TypeHelper.isGeneric(actualType))
        #    throw new Error(`Actual type cannot be generic (${actualType.repr()})!`);
        
        self.actual_type = actual_type
    
    def set_expected_type(self, type, allow_void = False):
        if self.actual_type != None:
            raise Error("Cannot set expected type after actual type was already set!")
        
        if self.expected_type != None:
            raise Error("Expression already has a expected type!")
        
        self.type_check(type, allow_void)
        
        self.expected_type = type
    
    def get_type(self):
        return self.actual_type or self.expected_type

class Identifier(Expression):
    def __init__(self, text):
        self.text = text
        super().__init__()

class NumericLiteral(Expression):
    def __init__(self, value_as_text):
        self.value_as_text = value_as_text
        super().__init__()

class BooleanLiteral(Expression):
    def __init__(self, bool_value):
        self.bool_value = bool_value
        super().__init__()

class CharacterLiteral(Expression):
    def __init__(self, char_value):
        self.char_value = char_value
        super().__init__()

class StringLiteral(Expression):
    def __init__(self, string_value):
        self.string_value = string_value
        super().__init__()

class NullLiteral(Expression):
    def __init__(self):
        super().__init__()

class RegexLiteral(Expression):
    def __init__(self, pattern, case_insensitive, global_):
        self.pattern = pattern
        self.case_insensitive = case_insensitive
        self.global_ = global_
        super().__init__()

class TemplateStringPart:
    def __init__(self, is_literal, literal_text, expression):
        self.is_literal = is_literal
        self.literal_text = literal_text
        self.expression = expression
    
    @classmethod
    def literal(cls, literal_text):
        return TemplateStringPart(True, literal_text, None)
    
    @classmethod
    def expression(cls, expr):
        return TemplateStringPart(False, None, expr)

class TemplateString(Expression):
    def __init__(self, parts):
        self.parts = parts
        super().__init__()

class ArrayLiteral(Expression):
    def __init__(self, items):
        self.items = items
        super().__init__()

class MapLiteralItem:
    def __init__(self, key, value):
        self.key = key
        self.value = value

class MapLiteral(Expression):
    def __init__(self, items):
        self.items = items
        super().__init__()

class UnresolvedNewExpression(Expression):
    def __init__(self, cls_, args):
        self.cls_ = cls_
        self.args = args
        super().__init__()

class NewExpression(Expression):
    def __init__(self, cls_, args):
        self.cls_ = cls_
        self.args = args
        super().__init__()

class BinaryExpression(Expression):
    def __init__(self, left, operator, right):
        self.left = left
        self.operator = operator
        self.right = right
        super().__init__()

class NullCoalesceExpression(Expression):
    def __init__(self, default_expr, expr_if_null):
        self.default_expr = default_expr
        self.expr_if_null = expr_if_null
        super().__init__()

class UnaryExpression(Expression):
    def __init__(self, unary_type, operator, operand):
        self.unary_type = unary_type
        self.operator = operator
        self.operand = operand
        super().__init__()

class CastExpression(Expression):
    def __init__(self, new_type, expression):
        self.new_type = new_type
        self.expression = expression
        self.instance_of_cast = None
        super().__init__()

class ParenthesizedExpression(Expression):
    def __init__(self, expression):
        self.expression = expression
        super().__init__()

class ConditionalExpression(Expression):
    def __init__(self, condition, when_true, when_false):
        self.condition = condition
        self.when_true = when_true
        self.when_false = when_false
        super().__init__()

class PropertyAccessExpression(Expression):
    def __init__(self, object, property_name):
        self.object = object
        self.property_name = property_name
        super().__init__()

class ElementAccessExpression(Expression):
    def __init__(self, object, element_expr):
        self.object = object
        self.element_expr = element_expr
        super().__init__()

class UnresolvedCallExpression(Expression):
    def __init__(self, func, type_args, args):
        self.func = func
        self.type_args = type_args
        self.args = args
        super().__init__()

class UnresolvedMethodCallExpression(Expression):
    def __init__(self, object, method_name, type_args, args):
        self.object = object
        self.method_name = method_name
        self.type_args = type_args
        self.args = args
        super().__init__()

class StaticMethodCallExpression(Expression):
    def __init__(self, method, type_args, args, is_this_call):
        self.method = method
        self.type_args = type_args
        self.args = args
        self.is_this_call = is_this_call
        super().__init__()

class InstanceMethodCallExpression(Expression):
    def __init__(self, object, method, type_args, args):
        self.object = object
        self.method = method
        self.type_args = type_args
        self.args = args
        super().__init__()

class GlobalFunctionCallExpression(Expression):
    def __init__(self, func, args):
        self.func = func
        self.args = args
        super().__init__()

class LambdaCallExpression(Expression):
    def __init__(self, method, args):
        self.method = method
        self.args = args
        super().__init__()

class TodoExpression(Expression):
    def __init__(self, expr):
        self.expr = expr
        super().__init__()

class InstanceOfExpression(Expression):
    def __init__(self, expr, check_type):
        self.expr = expr
        self.check_type = check_type
        self.implicit_casts = None
        self.alias = None
        super().__init__()

class AwaitExpression(Expression):
    def __init__(self, expr):
        self.expr = expr
        super().__init__()