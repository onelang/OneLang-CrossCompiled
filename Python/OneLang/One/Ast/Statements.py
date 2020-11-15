from OneLangStdLib import *
import OneLang.One.Ast.Types as types
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.References as refs
import OneLang.One.Ast.Interfaces as ints

class Statement:
    def __init__(self):
        self.leading_trivia = None
        self.attributes = None

class IfStatement(Statement):
    def __init__(self, condition, then, else_):
        self.condition = condition
        self.then = then
        self.else_ = else_
        super().__init__()

class ReturnStatement(Statement):
    def __init__(self, expression):
        self.expression = expression
        super().__init__()

class ThrowStatement(Statement):
    def __init__(self, expression):
        self.expression = expression
        super().__init__()

class ExpressionStatement(Statement):
    def __init__(self, expression):
        self.expression = expression
        super().__init__()

class BreakStatement(Statement):
    def __init__(self):
        super().__init__()

class ContinueStatement(Statement):
    def __init__(self):
        super().__init__()

class UnsetStatement(Statement):
    def __init__(self, expression):
        self.expression = expression
        super().__init__()

class VariableDeclaration(Statement):
    def __init__(self, name, type, initializer):
        self.name = name
        self.type = type
        self.initializer = initializer
        self.references = []
        self.mutability = None
        super().__init__()
    
    def create_reference(self):
        return refs.VariableDeclarationReference(self)

class WhileStatement(Statement):
    def __init__(self, condition, body):
        self.condition = condition
        self.body = body
        super().__init__()

class DoStatement(Statement):
    def __init__(self, condition, body):
        self.condition = condition
        self.body = body
        super().__init__()

class ForeachVariable:
    def __init__(self, name):
        self.name = name
        self.type = None
        self.references = []
        self.mutability = None
    
    def create_reference(self):
        return refs.ForeachVariableReference(self)

class ForeachStatement(Statement):
    def __init__(self, item_var, items, body):
        self.item_var = item_var
        self.items = items
        self.body = body
        super().__init__()

class ForVariable:
    def __init__(self, name, type, initializer):
        self.name = name
        self.type = type
        self.initializer = initializer
        self.references = []
        self.mutability = None
    
    def create_reference(self):
        return refs.ForVariableReference(self)

class ForStatement(Statement):
    def __init__(self, item_var, condition, incrementor, body):
        self.item_var = item_var
        self.condition = condition
        self.incrementor = incrementor
        self.body = body
        super().__init__()

class CatchVariable:
    def __init__(self, name, type):
        self.name = name
        self.type = type
        self.references = []
        self.mutability = None
    
    def create_reference(self):
        return refs.CatchVariableReference(self)

class TryStatement(Statement):
    def __init__(self, try_body, catch_var, catch_body, finally_body):
        self.try_body = try_body
        self.catch_var = catch_var
        self.catch_body = catch_body
        self.finally_body = finally_body
        super().__init__()
        if self.catch_body == None and self.finally_body == None:
            raise Error("try without catch and finally is not allowed")

class Block:
    def __init__(self, statements):
        self.statements = statements