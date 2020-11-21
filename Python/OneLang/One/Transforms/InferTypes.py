from onelang_core import *
from enum import Enum
import OneLang.One.AstTransformer as astTrans
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.Types as types
import OneLang.One.Transforms.InferTypesPlugins.BasicTypeInfer as basicTypeInfer
import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin as inferTypesPlug
import OneLang.One.Transforms.InferTypesPlugins.ArrayAndMapLiteralTypeInfer as arrayAndMapLitTypeInfer
import OneLang.One.Transforms.InferTypesPlugins.ResolveFieldAndPropertyAccess as resFieldAndPropAccs
import OneLang.One.Transforms.InferTypesPlugins.ResolveMethodCalls as resMethCalls
import OneLang.One.Transforms.InferTypesPlugins.LambdaResolver as lambdRes
import OneLang.One.Ast.Statements as stats
import OneLang.One.Transforms.InferTypesPlugins.ResolveEnumMemberAccess as resEnumMembAccs
import OneLang.One.Transforms.InferTypesPlugins.InferReturnType as inferRetType
import OneLang.One.Transforms.InferTypesPlugins.TypeScriptNullCoalesce as typeScrNullCoal
import OneLang.One.Transforms.InferTypesPlugins.InferForeachVarType as inferForVarType
import OneLang.One.Transforms.InferTypesPlugins.ResolveFuncCalls as resFuncCalls
import OneLang.One.Transforms.InferTypesPlugins.NullabilityCheckWithNot as nullCheckWithNot
import OneLang.One.Transforms.InferTypesPlugins.ResolveNewCall as resNewCall
import OneLang.One.Transforms.InferTypesPlugins.ResolveElementAccess as resElemAccs
import OneLang.One.Ast.AstTypes as astTypes

class INFER_TYPES_STAGE(Enum):
    INVALID = 1
    FIELDS = 2
    PROPERTIES = 3
    METHODS = 4

class InferTypes(astTrans.AstTransformer):
    def __init__(self):
        self.stage = None
        self.plugins = []
        self.context_info_idx = 0
        super().__init__("InferTypes")
        self.add_plugin(basicTypeInfer.BasicTypeInfer())
        self.add_plugin(arrayAndMapLitTypeInfer.ArrayAndMapLiteralTypeInfer())
        self.add_plugin(resFieldAndPropAccs.ResolveFieldAndPropertyAccess())
        self.add_plugin(resMethCalls.ResolveMethodCalls())
        self.add_plugin(lambdRes.LambdaResolver())
        self.add_plugin(inferRetType.InferReturnType())
        self.add_plugin(resEnumMembAccs.ResolveEnumMemberAccess())
        self.add_plugin(typeScrNullCoal.TypeScriptNullCoalesce())
        self.add_plugin(inferForVarType.InferForeachVarType())
        self.add_plugin(resFuncCalls.ResolveFuncCalls())
        self.add_plugin(nullCheckWithNot.NullabilityCheckWithNot())
        self.add_plugin(resNewCall.ResolveNewCalls())
        self.add_plugin(resElemAccs.ResolveElementAccess())
    
    def process_lambda(self, lambda_):
        super().visit_method_base(lambda_)
    
    def process_method_base(self, method):
        super().visit_method_base(method)
    
    def process_block(self, block):
        super().visit_block(block)
    
    def process_variable(self, variable):
        super().visit_variable(variable)
    
    def process_statement(self, stmt):
        super().visit_statement(stmt)
    
    def process_expression(self, expr):
        super().visit_expression(expr)
    
    def add_plugin(self, plugin):
        plugin.main = self
        plugin.error_man = self.error_man
        self.plugins.append(plugin)
    
    def visit_variable_with_initializer(self, variable):
        if variable.type != None and variable.initializer != None:
            variable.initializer.set_expected_type(variable.type)
        
        variable = super().visit_variable_with_initializer(variable)
        
        if variable.type == None and variable.initializer != None:
            variable.type = variable.initializer.get_type()
        
        return variable
    
    def run_transform_round(self, expr):
        if expr.actual_type != None:
            return expr
        
        self.error_man.current_node = expr
        
        transformers = list(filter(lambda x: x.can_transform(expr), self.plugins))
        if len(transformers) > 1:
            self.error_man.throw(f'''Multiple transformers found: {", ".join(list(map(lambda x: x.name, transformers)))}''')
        if len(transformers) != 1:
            return expr
        
        plugin = transformers[0]
        self.context_info_idx = self.context_info_idx + 1
        self.error_man.last_context_info = f'''[{self.context_info_idx}] running transform plugin "{plugin.name}"'''
        try:
            new_expr = plugin.transform(expr)
            # expression changed, restart the type infering process on the new expression
            if new_expr != None:
                new_expr.parent_node = expr.parent_node
            return new_expr
        except Exception as e:
            self.error_man.current_node = expr
            self.error_man.throw(f'''Error while running type transformation phase: {e}''')
            return expr
    
    def detect_type(self, expr):
        for plugin in self.plugins:
            if not plugin.can_detect_type(expr):
                continue
            self.context_info_idx = self.context_info_idx + 1
            self.error_man.last_context_info = f'''[{self.context_info_idx}] running type detection plugin "{plugin.name}"'''
            self.error_man.current_node = expr
            try:
                if plugin.detect_type(expr):
                    return True
            except Exception as e:
                self.error_man.throw(f'''Error while running type detection phase: {e}''')
        return False
    
    def visit_expression(self, expr):
        trans_expr = expr
        while True:
            new_expr = self.run_transform_round(trans_expr)
            if new_expr == trans_expr:
                break
            trans_expr = new_expr
        
        # if the plugin did not handle the expression, we use the default visit method
        if trans_expr == expr:
            trans_expr = super().visit_expression(expr)
        
        if trans_expr.actual_type != None:
            return trans_expr
        
        detect_success = self.detect_type(trans_expr)
        
        if trans_expr.actual_type == None:
            if detect_success:
                self.error_man.throw("Type detection failed, although plugin tried to handle it")
            else:
                self.error_man.throw("Type detection failed: none of the plugins could resolve the type")
        
        return trans_expr
    
    def visit_statement(self, stmt):
        self.current_statement = stmt
        
        if isinstance(stmt, stats.ReturnStatement) and stmt.expression != None and isinstance(self.current_closure, types.Method) and self.current_closure.returns != None:
            return_type = self.current_closure.returns
            if isinstance(return_type, astTypes.ClassType) and return_type.decl == self.current_file.literal_types.promise.decl and self.current_closure.async_:
                return_type = return_type.type_arguments[0]
            stmt.expression.set_expected_type(return_type)
        
        for plugin in self.plugins:
            if plugin.handle_statement(stmt):
                return stmt
        
        return super().visit_statement(stmt)
    
    def visit_field(self, field):
        if self.stage != INFER_TYPES_STAGE.FIELDS:
            return
        super().visit_field(field)
    
    def visit_property(self, prop):
        if self.stage != INFER_TYPES_STAGE.PROPERTIES:
            return
        
        for plugin in self.plugins:
            if plugin.handle_property(prop):
                return
        
        super().visit_property(prop)
    
    def visit_method_base(self, method):
        if self.stage != INFER_TYPES_STAGE.METHODS:
            return
        
        for plugin in self.plugins:
            if plugin.handle_method(method):
                return
        
        super().visit_method_base(method)
    
    def visit_lambda(self, lambda_):
        if lambda_.actual_type != None:
            return lambda_
        
        prev_closure = self.current_closure
        self.current_closure = lambda_
        
        for plugin in self.plugins:
            if plugin.handle_lambda(lambda_):
                return lambda_
        
        self.current_closure = prev_closure
        super().visit_method_base(lambda_)
        return lambda_
    
    def run_plugins_on(self, expr):
        return self.visit_expression(expr)
    
    def visit_class(self, cls_):
        if cls_.attributes.get("external") == "true":
            return
        super().visit_class(cls_)
    
    def visit_files(self, files):
        for stage in [INFER_TYPES_STAGE.FIELDS, INFER_TYPES_STAGE.PROPERTIES, INFER_TYPES_STAGE.METHODS]:
            self.stage = stage
            for file in files:
                self.visit_file(file)