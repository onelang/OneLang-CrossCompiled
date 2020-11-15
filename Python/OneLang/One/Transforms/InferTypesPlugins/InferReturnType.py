from OneLangStdLib import *
import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin as inferTypesPlug
import OneLang.One.Ast.Types as types
import OneLang.One.Ast.AstTypes as astTypes
import OneLang.One.Ast.Statements as stats
import OneLang.One.ErrorManager as errorMan
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.Interfaces as ints

class ReturnTypeInferer:
    def __init__(self, error_man):
        self.returns_null = False
        self.throws = False
        self.return_types = []
        self.error_man = error_man
    
    def add_return(self, return_value):
        if isinstance(return_value, exprs.NullLiteral):
            self.returns_null = True
            return
        
        return_type = return_value.actual_type
        if return_type == None:
            raise Error("Return type cannot be null")
        
        if not ArrayHelper.some(lambda x: astTypes.TypeHelper.equals(x, return_type), self.return_types):
            self.return_types.append(return_type)
    
    def finish(self, declared_type, error_context, async_type):
        inferred_type = None
        
        if len(self.return_types) == 0:
            if self.throws:
                inferred_type = declared_type or astTypes.VoidType.instance
            elif self.returns_null:
                if declared_type != None:
                    inferred_type = declared_type
                else:
                    self.error_man.throw(f'''{error_context} returns only null and it has no declared return type!''')
            else:
                inferred_type = astTypes.VoidType.instance
        elif len(self.return_types) == 1:
            inferred_type = self.return_types[0]
        elif declared_type != None and ArrayHelper.every(lambda x, i: astTypes.TypeHelper.is_assignable_to(x, declared_type), self.return_types):
            inferred_type = declared_type
        else:
            self.error_man.throw(f'''{error_context} returns different types: {", ".join(list(map(lambda x: x.repr(), self.return_types)))}''')
            inferred_type = astTypes.AnyType.instance
        
        check_type = declared_type
        if check_type != None and async_type != None and isinstance(check_type, astTypes.ClassType) and check_type.decl == async_type.decl:
            check_type = check_type.type_arguments[0]
        
        if check_type != None and not astTypes.TypeHelper.is_assignable_to(inferred_type, check_type):
            self.error_man.throw(f'''{error_context} returns different type ({inferred_type.repr()}) than expected {check_type.repr()}''')
        
        self.return_types = None
        return declared_type if declared_type != None else inferred_type

class InferReturnType(inferTypesPlug.InferTypesPlugin):
    def __init__(self):
        self.return_type_infer = []
        super().__init__("InferReturnType")
    
    def get_current(self):
        return self.return_type_infer[len(self.return_type_infer) - 1]
    
    def start(self):
        self.return_type_infer.append(ReturnTypeInferer(self.error_man))
    
    def finish(self, declared_type, error_context, async_type):
        return self.return_type_infer.pop().finish(declared_type, error_context, async_type)
    
    def handle_statement(self, stmt):
        if len(self.return_type_infer) == 0:
            return False
        if isinstance(stmt, stats.ReturnStatement) and stmt.expression != None:
            self.main.process_statement(stmt)
            self.get_current().add_return(stmt.expression)
            return True
        elif isinstance(stmt, stats.ThrowStatement):
            self.get_current().throws = True
            return False
        else:
            return False
    
    def handle_lambda(self, lambda_):
        self.start()
        self.main.process_lambda(lambda_)
        lambda_.returns = self.finish(lambda_.returns, "Lambda", None)
        lambda_.set_actual_type(astTypes.LambdaType(lambda_.parameters, lambda_.returns), False, True)
        return True
    
    def handle_method(self, method):
        if isinstance(method, types.Method) and method.body != None:
            self.start()
            self.main.process_method_base(method)
            method.returns = self.finish(method.returns, f'''Method "{method.name}"''', self.main.current_file.literal_types.promise if method.async_ else None)
            return True
        else:
            return False
    
    def handle_property(self, prop):
        self.main.process_variable(prop)
        
        if prop.getter != None:
            self.start()
            self.main.process_block(prop.getter)
            prop.type = self.finish(prop.type, f'''Property "{prop.name}" getter''', None)
        
        if prop.setter != None:
            self.start()
            self.main.process_block(prop.setter)
            self.finish(astTypes.VoidType.instance, f'''Property "{prop.name}" setter''', None)
        
        return True