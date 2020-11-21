from onelang_core import *
import OneLang.One.Ast.AstTypes as astTypes
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.Statements as stats
import OneLang.One.Ast.Types as types
import OneLang.One.Ast.References as refs
import OneLang.One.ErrorManager as errorMan
import OneLang.One.ITransformer as iTrans
import OneLang.One.Ast.Interfaces as ints

class AstTransformer:
    def __init__(self, name):
        self.error_man = errorMan.ErrorManager()
        self.current_file = None
        self.current_interface = None
        self.current_method = None
        self.current_closure = None
        self.current_statement = None
        self.name = name
    
    def visit_attributes_and_trivia(self, node):
        pass
    
    def visit_type(self, type):
        if isinstance(type, astTypes.ClassType) or isinstance(type, astTypes.InterfaceType) or isinstance(type, astTypes.UnresolvedType):
            type2 = type
            type2.type_arguments = list(map(lambda x: self.visit_type(x), type2.type_arguments))
        elif isinstance(type, astTypes.LambdaType):
            for mp in type.parameters:
                self.visit_method_parameter(mp)
            type.return_type = self.visit_type(type.return_type)
        return type
    
    def visit_identifier(self, id):
        return id
    
    def visit_variable(self, variable):
        if variable.type != None:
            variable.type = self.visit_type(variable.type)
        return variable
    
    def visit_variable_with_initializer(self, variable):
        self.visit_variable(variable)
        if variable.initializer != None:
            variable.initializer = self.visit_expression(variable.initializer)
        return variable
    
    def visit_variable_declaration(self, stmt):
        self.visit_variable_with_initializer(stmt)
        return stmt
    
    def visit_unknown_statement(self, stmt):
        self.error_man.throw(f'''Unknown statement type''')
        return stmt
    
    def visit_statement(self, stmt):
        self.current_statement = stmt
        self.visit_attributes_and_trivia(stmt)
        if isinstance(stmt, stats.ReturnStatement):
            if stmt.expression != None:
                stmt.expression = self.visit_expression(stmt.expression)
        elif isinstance(stmt, stats.ExpressionStatement):
            stmt.expression = self.visit_expression(stmt.expression)
        elif isinstance(stmt, stats.IfStatement):
            stmt.condition = self.visit_expression(stmt.condition)
            stmt.then = self.visit_block(stmt.then)
            if stmt.else_ != None:
                stmt.else_ = self.visit_block(stmt.else_)
        elif isinstance(stmt, stats.ThrowStatement):
            stmt.expression = self.visit_expression(stmt.expression)
        elif isinstance(stmt, stats.VariableDeclaration):
            return self.visit_variable_declaration(stmt)
        elif isinstance(stmt, stats.WhileStatement):
            stmt.condition = self.visit_expression(stmt.condition)
            stmt.body = self.visit_block(stmt.body)
        elif isinstance(stmt, stats.DoStatement):
            stmt.condition = self.visit_expression(stmt.condition)
            stmt.body = self.visit_block(stmt.body)
        elif isinstance(stmt, stats.ForStatement):
            if stmt.item_var != None:
                self.visit_variable_with_initializer(stmt.item_var)
            stmt.condition = self.visit_expression(stmt.condition)
            stmt.incrementor = self.visit_expression(stmt.incrementor)
            stmt.body = self.visit_block(stmt.body)
        elif isinstance(stmt, stats.ForeachStatement):
            self.visit_variable(stmt.item_var)
            stmt.items = self.visit_expression(stmt.items)
            stmt.body = self.visit_block(stmt.body)
        elif isinstance(stmt, stats.TryStatement):
            stmt.try_body = self.visit_block(stmt.try_body)
            if stmt.catch_body != None:
                self.visit_variable(stmt.catch_var)
                stmt.catch_body = self.visit_block(stmt.catch_body)
            if stmt.finally_body != None:
                stmt.finally_body = self.visit_block(stmt.finally_body)
        elif isinstance(stmt, stats.BreakStatement):
            pass
        elif isinstance(stmt, stats.UnsetStatement):
            stmt.expression = self.visit_expression(stmt.expression)
        elif isinstance(stmt, stats.ContinueStatement):
            pass
        else:
            return self.visit_unknown_statement(stmt)
        return stmt
    
    def visit_block(self, block):
        block.statements = list(map(lambda x: self.visit_statement(x), block.statements))
        return block
    
    def visit_template_string(self, expr):
        i = 0
        
        while i < len(expr.parts):
            part = expr.parts[i]
            if not part.is_literal:
                part.expression = self.visit_expression(part.expression)
            i = i + 1
        return expr
    
    def visit_unknown_expression(self, expr):
        self.error_man.throw(f'''Unknown expression type''')
        return expr
    
    def visit_lambda(self, lambda_):
        prev_closure = self.current_closure
        self.current_closure = lambda_
        self.visit_method_base(lambda_)
        self.current_closure = prev_closure
        return lambda_
    
    def visit_variable_reference(self, var_ref):
        return var_ref
    
    def visit_expression(self, expr):
        if isinstance(expr, exprs.BinaryExpression):
            expr.left = self.visit_expression(expr.left)
            expr.right = self.visit_expression(expr.right)
        elif isinstance(expr, exprs.NullCoalesceExpression):
            expr.default_expr = self.visit_expression(expr.default_expr)
            expr.expr_if_null = self.visit_expression(expr.expr_if_null)
        elif isinstance(expr, exprs.UnresolvedCallExpression):
            expr.func = self.visit_expression(expr.func)
            expr.type_args = list(map(lambda x: self.visit_type(x), expr.type_args))
            expr.args = list(map(lambda x: self.visit_expression(x), expr.args))
        elif isinstance(expr, exprs.UnresolvedMethodCallExpression):
            expr.object = self.visit_expression(expr.object)
            expr.type_args = list(map(lambda x: self.visit_type(x), expr.type_args))
            expr.args = list(map(lambda x: self.visit_expression(x), expr.args))
        elif isinstance(expr, exprs.ConditionalExpression):
            expr.condition = self.visit_expression(expr.condition)
            expr.when_true = self.visit_expression(expr.when_true)
            expr.when_false = self.visit_expression(expr.when_false)
        elif isinstance(expr, exprs.Identifier):
            return self.visit_identifier(expr)
        elif isinstance(expr, exprs.UnresolvedNewExpression):
            self.visit_type(expr.cls_)
            expr.args = list(map(lambda x: self.visit_expression(x), expr.args))
        elif isinstance(expr, exprs.NewExpression):
            self.visit_type(expr.cls_)
            expr.args = list(map(lambda x: self.visit_expression(x), expr.args))
        elif isinstance(expr, exprs.TemplateString):
            return self.visit_template_string(expr)
        elif isinstance(expr, exprs.ParenthesizedExpression):
            expr.expression = self.visit_expression(expr.expression)
        elif isinstance(expr, exprs.UnaryExpression):
            expr.operand = self.visit_expression(expr.operand)
        elif isinstance(expr, exprs.PropertyAccessExpression):
            expr.object = self.visit_expression(expr.object)
        elif isinstance(expr, exprs.ElementAccessExpression):
            expr.object = self.visit_expression(expr.object)
            expr.element_expr = self.visit_expression(expr.element_expr)
        elif isinstance(expr, exprs.ArrayLiteral):
            expr.items = list(map(lambda x: self.visit_expression(x), expr.items))
        elif isinstance(expr, exprs.MapLiteral):
            for item in expr.items:
                item.value = self.visit_expression(item.value)
        elif isinstance(expr, exprs.StringLiteral):
            pass
        elif isinstance(expr, exprs.BooleanLiteral):
            pass
        elif isinstance(expr, exprs.NumericLiteral):
            pass
        elif isinstance(expr, exprs.NullLiteral):
            pass
        elif isinstance(expr, exprs.RegexLiteral):
            pass
        elif isinstance(expr, exprs.CastExpression):
            expr.new_type = self.visit_type(expr.new_type)
            expr.expression = self.visit_expression(expr.expression)
        elif isinstance(expr, exprs.InstanceOfExpression):
            expr.expr = self.visit_expression(expr.expr)
            expr.check_type = self.visit_type(expr.check_type)
        elif isinstance(expr, exprs.AwaitExpression):
            expr.expr = self.visit_expression(expr.expr)
        elif isinstance(expr, types.Lambda):
            return self.visit_lambda(expr)
        elif isinstance(expr, refs.ClassReference):
            pass
        elif isinstance(expr, refs.EnumReference):
            pass
        elif isinstance(expr, refs.ThisReference):
            pass
        elif isinstance(expr, refs.StaticThisReference):
            pass
        elif isinstance(expr, refs.MethodParameterReference):
            return self.visit_variable_reference(expr)
        elif isinstance(expr, refs.VariableDeclarationReference):
            return self.visit_variable_reference(expr)
        elif isinstance(expr, refs.ForVariableReference):
            return self.visit_variable_reference(expr)
        elif isinstance(expr, refs.ForeachVariableReference):
            return self.visit_variable_reference(expr)
        elif isinstance(expr, refs.CatchVariableReference):
            return self.visit_variable_reference(expr)
        elif isinstance(expr, refs.GlobalFunctionReference):
            pass
        elif isinstance(expr, refs.SuperReference):
            pass
        elif isinstance(expr, refs.InstanceFieldReference):
            expr.object = self.visit_expression(expr.object)
            return self.visit_variable_reference(expr)
        elif isinstance(expr, refs.InstancePropertyReference):
            expr.object = self.visit_expression(expr.object)
            return self.visit_variable_reference(expr)
        elif isinstance(expr, refs.StaticFieldReference):
            return self.visit_variable_reference(expr)
        elif isinstance(expr, refs.StaticPropertyReference):
            return self.visit_variable_reference(expr)
        elif isinstance(expr, refs.EnumMemberReference):
            pass
        elif isinstance(expr, exprs.StaticMethodCallExpression):
            expr.type_args = list(map(lambda x: self.visit_type(x), expr.type_args))
            expr.args = list(map(lambda x: self.visit_expression(x), expr.args))
        elif isinstance(expr, exprs.GlobalFunctionCallExpression):
            expr.args = list(map(lambda x: self.visit_expression(x), expr.args))
        elif isinstance(expr, exprs.InstanceMethodCallExpression):
            expr.object = self.visit_expression(expr.object)
            expr.type_args = list(map(lambda x: self.visit_type(x), expr.type_args))
            expr.args = list(map(lambda x: self.visit_expression(x), expr.args))
        elif isinstance(expr, exprs.LambdaCallExpression):
            expr.args = list(map(lambda x: self.visit_expression(x), expr.args))
        else:
            return self.visit_unknown_expression(expr)
        return expr
    
    def visit_method_parameter(self, method_parameter):
        self.visit_attributes_and_trivia(method_parameter)
        self.visit_variable_with_initializer(method_parameter)
    
    def visit_method_base(self, method):
        for item in method.parameters:
            self.visit_method_parameter(item)
        
        if method.body != None:
            method.body = self.visit_block(method.body)
    
    def visit_method(self, method):
        self.current_method = method
        self.current_closure = method
        self.visit_attributes_and_trivia(method)
        self.visit_method_base(method)
        method.returns = self.visit_type(method.returns)
        self.current_closure = None
        self.current_method = None
    
    def visit_global_function(self, func):
        self.visit_method_base(func)
        func.returns = self.visit_type(func.returns)
    
    def visit_constructor(self, constructor):
        self.current_method = constructor
        self.current_closure = constructor
        self.visit_attributes_and_trivia(constructor)
        self.visit_method_base(constructor)
        self.current_closure = None
        self.current_method = None
    
    def visit_field(self, field):
        self.visit_attributes_and_trivia(field)
        self.visit_variable_with_initializer(field)
    
    def visit_property(self, prop):
        self.visit_attributes_and_trivia(prop)
        self.visit_variable(prop)
        if prop.getter != None:
            prop.getter = self.visit_block(prop.getter)
        if prop.setter != None:
            prop.setter = self.visit_block(prop.setter)
    
    def visit_interface(self, intf):
        self.current_interface = intf
        self.visit_attributes_and_trivia(intf)
        intf.base_interfaces = list(map(lambda x: self.visit_type(x), intf.base_interfaces))
        for field in intf.fields:
            self.visit_field(field)
        for method in intf.methods:
            self.visit_method(method)
        self.current_interface = None
    
    def visit_class(self, cls_):
        self.current_interface = cls_
        self.visit_attributes_and_trivia(cls_)
        if cls_.constructor_ != None:
            self.visit_constructor(cls_.constructor_)
        
        cls_.base_class = self.visit_type(cls_.base_class)
        cls_.base_interfaces = list(map(lambda x: self.visit_type(x), cls_.base_interfaces))
        for field in cls_.fields:
            self.visit_field(field)
        for prop in cls_.properties:
            self.visit_property(prop)
        for method in cls_.methods:
            self.visit_method(method)
        self.current_interface = None
    
    def visit_enum(self, enum_):
        self.visit_attributes_and_trivia(enum_)
        for value in enum_.values:
            self.visit_enum_member(value)
    
    def visit_enum_member(self, enum_member):
        pass
    
    def visit_import(self, imp):
        self.visit_attributes_and_trivia(imp)
    
    def visit_file(self, source_file):
        self.error_man.reset_context(self)
        self.current_file = source_file
        for imp in source_file.imports:
            self.visit_import(imp)
        for enum_ in source_file.enums:
            self.visit_enum(enum_)
        for intf in source_file.interfaces:
            self.visit_interface(intf)
        for cls_ in source_file.classes:
            self.visit_class(cls_)
        for func in source_file.funcs:
            self.visit_global_function(func)
        source_file.main_block = self.visit_block(source_file.main_block)
        self.current_file = None
    
    def visit_files(self, files):
        for file in files:
            self.visit_file(file)
    
    def visit_package(self, pkg):
        self.visit_files(pkg.files.values())