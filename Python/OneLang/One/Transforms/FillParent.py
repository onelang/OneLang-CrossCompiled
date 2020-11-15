from OneLangStdLib import *
import OneLang.One.Ast.Types as types
import OneLang.One.Ast.Statements as stats
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.AstTransformer as astTrans

class FillParent(astTrans.AstTransformer):
    def __init__(self):
        self.parent_node_stack = []
        super().__init__("FillParent")
    
    def visit_expression(self, expr):
        if len(self.parent_node_stack) == 0:
            pass
        expr.parent_node = self.parent_node_stack[len(self.parent_node_stack) - 1]
        self.parent_node_stack.append(expr)
        super().visit_expression(expr)
        self.parent_node_stack.pop()
        return expr
    
    def visit_statement(self, stmt):
        self.parent_node_stack.append(stmt)
        super().visit_statement(stmt)
        self.parent_node_stack.pop()
        return stmt
    
    def visit_enum(self, enum_):
        enum_.parent_file = self.current_file
        super().visit_enum(enum_)
        for value in enum_.values:
            value.parent_enum = enum_
    
    def visit_interface(self, intf):
        intf.parent_file = self.current_file
        super().visit_interface(intf)
    
    def visit_class(self, cls_):
        cls_.parent_file = self.current_file
        super().visit_class(cls_)
    
    def visit_global_function(self, func):
        func.parent_file = self.current_file
        super().visit_global_function(func)
    
    def visit_field(self, field):
        field.parent_interface = self.current_interface
        
        self.parent_node_stack.append(field)
        super().visit_field(field)
        self.parent_node_stack.pop()
    
    def visit_property(self, prop):
        prop.parent_class = self.current_interface
        
        self.parent_node_stack.append(prop)
        super().visit_property(prop)
        self.parent_node_stack.pop()
    
    def visit_method_base(self, method):
        if isinstance(method, types.Constructor):
            method.parent_class = self.current_interface
        elif isinstance(method, types.Method):
            method.parent_interface = self.current_interface
        elif isinstance(method, types.GlobalFunction):
            pass
        elif isinstance(method, types.Lambda):
            pass
        else:
            pass
        
        for param in method.parameters:
            param.parent_method = method
        
        self.parent_node_stack.append(method)
        super().visit_method_base(method)
        self.parent_node_stack.pop()
    
    def visit_file(self, file):
        for imp in file.imports:
            imp.parent_file = file
        
        super().visit_file(file)