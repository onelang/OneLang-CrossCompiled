from onelang_core import *
import OneLang.Utils.TSOverviewGenerator as tSOvervGen
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.References as refs
import OneLang.One.Ast.Statements as stats
import OneLang.One.Ast.Types as types
import OneLang.One.AstTransformer as astTrans

class DefaultExpressionNamingStrategy:
    def __init__(self):
        pass
    
    def get_name_for(self, expr):
        if isinstance(expr, exprs.InstanceMethodCallExpression) or isinstance(expr, exprs.StaticMethodCallExpression):
            return f'''{(expr).method.name}Result'''
        return "result"

class VariableNameHandler:
    def __init__(self):
        self.usage_count = Map()
    
    def use_name(self, name):
        if self.usage_count.has(name):
            new_idx = self.usage_count.get(name) + 1
            self.usage_count.set(name, new_idx)
            return f'''{name}{new_idx}'''
        else:
            self.usage_count.set(name, 1)
            return name
    
    def reset_scope(self):
        self.usage_count = Map()

class ConvertNullCoalesce(astTrans.AstTransformer):
    def __init__(self):
        self.expr_naming = DefaultExpressionNamingStrategy()
        self.var_names = VariableNameHandler()
        self.statements = []
        super().__init__("RemoveNullCoalesce")
    
    def visit_variable(self, variable):
        self.var_names.use_name(variable.name)
        return super().visit_variable(variable)
    
    def visit_method_base(self, method_base):
        if not (isinstance(method_base, types.Lambda)):
            self.var_names.reset_scope()
        super().visit_method_base(method_base)
    
    def visit_block(self, block):
        # @csharp var prevStatements = this.statements;
        # @java var prevStatements = this.statements;
        prev_statements = self.statements
        self.statements = []
        for stmt in block.statements:
            self.statements.append(self.visit_statement(stmt))
        block.statements = self.statements
        # @csharp this.statements = prevStatements;
        # @java this.statements = prevStatements;
        self.statements = prev_statements
        return block
    
    def visit_expression(self, expr):
        expr = super().visit_expression(expr)
        if isinstance(expr, exprs.NullCoalesceExpression):
            if isinstance(expr.default_expr, refs.InstanceFieldReference) or isinstance(expr.default_expr, refs.StaticFieldReference):
                return expr
            
            var_name = self.var_names.use_name(self.expr_naming.get_name_for(expr.default_expr))
            
            var_decl = stats.VariableDeclaration(var_name, expr.default_expr.get_type(), expr.default_expr)
            var_decl.mutability = types.MutabilityInfo(False, False, False)
            self.statements.append(var_decl)
            
            expr.default_expr = refs.VariableDeclarationReference(var_decl)
        return expr