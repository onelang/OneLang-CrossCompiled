from onelang_core import *
import OneLang.One.AstTransformer as astTrans
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.Statements as stats
import OneLang.One.Ast.References as refs
import OneLang.Utils.ArrayHelper as arrayHelp

class InstanceOfImplicitCast(astTrans.AstTransformer):
    def __init__(self):
        self.casts = []
        self.cast_counts = []
        super().__init__("InstanceOfImplicitCast")
    
    def add_cast(self, cast):
        if len(self.cast_counts) > 0:
            cast.implicit_casts = []
            self.casts.append(cast)
            last = len(self.cast_counts) - 1
            self.cast_counts[last] = self.cast_counts[last] + 1
    
    def push_context(self):
        self.cast_counts.append(0)
    
    def pop_context(self):
        cast_count = self.cast_counts.pop()
        if cast_count != 0:
            del self.casts[-cast_count:]
    
    def equals(self, expr1, expr2):
        # implicit casts don't matter when checking equality...
        while isinstance(expr1, exprs.CastExpression) and expr1.instance_of_cast != None:
            expr1 = expr1.expression
        while isinstance(expr2, exprs.CastExpression) and expr2.instance_of_cast != None:
            expr2 = expr2.expression
        
        # MetP, V, MethP.PA, V.PA, MethP/V [ {FEVR} ], FEVR
        if isinstance(expr1, exprs.PropertyAccessExpression):
            return isinstance(expr2, exprs.PropertyAccessExpression) and expr1.property_name == expr2.property_name and self.equals(expr1.object, expr2.object)
        elif isinstance(expr1, refs.VariableDeclarationReference):
            return isinstance(expr2, refs.VariableDeclarationReference) and expr1.decl == expr2.decl
        elif isinstance(expr1, refs.MethodParameterReference):
            return isinstance(expr2, refs.MethodParameterReference) and expr1.decl == expr2.decl
        elif isinstance(expr1, refs.ForeachVariableReference):
            return isinstance(expr2, refs.ForeachVariableReference) and expr1.decl == expr2.decl
        elif isinstance(expr1, refs.InstanceFieldReference):
            return isinstance(expr2, refs.InstanceFieldReference) and expr1.field == expr2.field
        elif isinstance(expr1, refs.ThisReference):
            return isinstance(expr2, refs.ThisReference)
        elif isinstance(expr1, refs.StaticThisReference):
            return isinstance(expr2, refs.StaticThisReference)
        return False
    
    def visit_expression(self, expr):
        result = expr
        if isinstance(expr, exprs.InstanceOfExpression):
            self.visit_expression(expr.expr)
            self.add_cast(expr)
        elif isinstance(expr, exprs.BinaryExpression) and expr.operator == "&&":
            expr.left = self.visit_expression(expr.left)
            expr.right = self.visit_expression(expr.right)
        elif isinstance(expr, exprs.ConditionalExpression):
            self.push_context()
            expr.condition = self.visit_expression(expr.condition)
            expr.when_true = self.visit_expression(expr.when_true)
            self.pop_context()
            
            expr.when_false = self.visit_expression(expr.when_false)
        elif isinstance(expr, refs.Reference) and isinstance(expr.parent_node, exprs.BinaryExpression) and expr.parent_node.operator == "=" and expr.parent_node.left == expr:
            pass
        else:
            self.push_context()
            result = super().visit_expression(expr)
            self.pop_context()
            # @java final var result2 = result;
            result2 = result
            match = next(filter(lambda cast: self.equals(result2, cast.expr), self.casts), None)
            if match != None:
                cast_expr = exprs.CastExpression(match.check_type, result)
                cast_expr.instance_of_cast = match
                match.implicit_casts.append(cast_expr)
                result = cast_expr
        return result
    
    def visit_statement(self, stmt):
        self.current_statement = stmt
        
        if isinstance(stmt, stats.IfStatement):
            self.push_context()
            stmt.condition = self.visit_expression(stmt.condition)
            self.visit_block(stmt.then)
            self.pop_context()
            
            if stmt.else_ != None:
                self.visit_block(stmt.else_)
        elif isinstance(stmt, stats.WhileStatement):
            self.push_context()
            stmt.condition = self.visit_expression(stmt.condition)
            self.visit_block(stmt.body)
            self.pop_context()
        else:
            self.push_context()
            super().visit_statement(stmt)
            self.pop_context()
        
        return stmt