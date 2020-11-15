from OneLangStdLib import *
import OneLang.One.ErrorManager as errorMan
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Transforms.InferTypes as inferTypes
import OneLang.One.Ast.Statements as stats
import OneLang.One.Ast.Types as types

class InferTypesPlugin:
    def __init__(self, name):
        self.main = None
        self.error_man = None
        self.name = name
    
    def can_transform(self, expr):
        return False
    
    def can_detect_type(self, expr):
        return False
    
    def transform(self, expr):
        return expr
    
    def detect_type(self, expr):
        return False
    
    def handle_property(self, prop):
        return False
    
    def handle_lambda(self, lambda_):
        return False
    
    def handle_method(self, method):
        return False
    
    def handle_statement(self, stmt):
        return False