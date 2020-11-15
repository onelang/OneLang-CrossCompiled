from OneLangStdLib import *
import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin as inferTypesPlug
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.References as refs
import OneLang.One.Ast.AstTypes as astTypes

class BasicTypeInfer(inferTypesPlug.InferTypesPlugin):
    def __init__(self):
        super().__init__("BasicTypeInfer")
    
    def can_detect_type(self, expr):
        return True
    
    def detect_type(self, expr):
        lit_types = self.main.current_file.literal_types
        
        if isinstance(expr, exprs.CastExpression):
            expr.set_actual_type(expr.new_type)
        elif isinstance(expr, exprs.ParenthesizedExpression):
            expr.set_actual_type(expr.expression.get_type())
        elif isinstance(expr, refs.ThisReference):
            expr.set_actual_type(expr.cls_.type, False, False)
        elif isinstance(expr, refs.SuperReference):
            expr.set_actual_type(expr.cls_.type, False, False)
        elif isinstance(expr, refs.MethodParameterReference):
            expr.set_actual_type(expr.decl.type, False, False)
        elif isinstance(expr, exprs.BooleanLiteral):
            expr.set_actual_type(lit_types.boolean)
        elif isinstance(expr, exprs.NumericLiteral):
            expr.set_actual_type(lit_types.numeric)
        elif isinstance(expr, exprs.StringLiteral) or isinstance(expr, exprs.TemplateString):
            expr.set_actual_type(lit_types.string)
        elif isinstance(expr, exprs.RegexLiteral):
            expr.set_actual_type(lit_types.regex)
        elif isinstance(expr, exprs.InstanceOfExpression):
            expr.set_actual_type(lit_types.boolean)
        elif isinstance(expr, exprs.NullLiteral):
            expr.set_actual_type(expr.expected_type if expr.expected_type != None else astTypes.NullType.instance)
        elif isinstance(expr, refs.VariableDeclarationReference):
            expr.set_actual_type(expr.decl.type)
        elif isinstance(expr, refs.ForeachVariableReference):
            expr.set_actual_type(expr.decl.type)
        elif isinstance(expr, refs.ForVariableReference):
            expr.set_actual_type(expr.decl.type)
        elif isinstance(expr, refs.CatchVariableReference):
            expr.set_actual_type(expr.decl.type or self.main.current_file.literal_types.error)
        elif isinstance(expr, exprs.UnaryExpression):
            operand_type = expr.operand.get_type()
            if isinstance(operand_type, astTypes.ClassType):
                op_id = f'''{expr.operator}{operand_type.decl.name}'''
                
                if op_id == "-TsNumber":
                    expr.set_actual_type(lit_types.numeric)
                elif op_id == "+TsNumber":
                    expr.set_actual_type(lit_types.numeric)
                elif op_id == "!TsBoolean":
                    expr.set_actual_type(lit_types.boolean)
                elif op_id == "++TsNumber":
                    expr.set_actual_type(lit_types.numeric)
                elif op_id == "--TsNumber":
                    expr.set_actual_type(lit_types.numeric)
                else:
                    pass
            elif isinstance(operand_type, astTypes.AnyType):
                expr.set_actual_type(astTypes.AnyType.instance)
            else:
                pass
        elif isinstance(expr, exprs.BinaryExpression):
            left_type = expr.left.get_type()
            right_type = expr.right.get_type()
            is_eq_or_neq = expr.operator == "==" or expr.operator == "!="
            if expr.operator == "=":
                if astTypes.TypeHelper.is_assignable_to(right_type, left_type):
                    expr.set_actual_type(left_type, False, True)
                else:
                    raise Error(f'''Right-side expression ({right_type.repr()}) is not assignable to left-side ({left_type.repr()}).''')
            elif is_eq_or_neq:
                expr.set_actual_type(lit_types.boolean)
            elif isinstance(left_type, astTypes.ClassType) and isinstance(right_type, astTypes.ClassType):
                if left_type.decl == lit_types.numeric.decl and right_type.decl == lit_types.numeric.decl and expr.operator in ["-", "+", "-=", "+=", "%", "/"]:
                    expr.set_actual_type(lit_types.numeric)
                elif left_type.decl == lit_types.numeric.decl and right_type.decl == lit_types.numeric.decl and expr.operator in ["<", "<=", ">", ">="]:
                    expr.set_actual_type(lit_types.boolean)
                elif left_type.decl == lit_types.string.decl and right_type.decl == lit_types.string.decl and expr.operator in ["+", "+="]:
                    expr.set_actual_type(lit_types.string)
                elif left_type.decl == lit_types.string.decl and right_type.decl == lit_types.string.decl and expr.operator in ["<="]:
                    expr.set_actual_type(lit_types.boolean)
                elif left_type.decl == lit_types.boolean.decl and right_type.decl == lit_types.boolean.decl and expr.operator in ["||", "&&"]:
                    expr.set_actual_type(lit_types.boolean)
                elif left_type.decl == lit_types.string.decl and right_type.decl == lit_types.map.decl and expr.operator == "in":
                    expr.set_actual_type(lit_types.boolean)
                else:
                    pass
            elif isinstance(left_type, astTypes.EnumType) and isinstance(right_type, astTypes.EnumType):
                if left_type.decl == right_type.decl and is_eq_or_neq:
                    expr.set_actual_type(lit_types.boolean)
                else:
                    pass
            elif isinstance(left_type, astTypes.AnyType) and isinstance(right_type, astTypes.AnyType):
                expr.set_actual_type(astTypes.AnyType.instance)
            else:
                pass
        elif isinstance(expr, exprs.ConditionalExpression):
            true_type = expr.when_true.get_type()
            false_type = expr.when_false.get_type()
            if expr.expected_type != None:
                if not astTypes.TypeHelper.is_assignable_to(true_type, expr.expected_type):
                    raise Error(f'''Conditional expression expects {expr.expected_type.repr()} but got {true_type.repr()} as true branch''')
                if not astTypes.TypeHelper.is_assignable_to(false_type, expr.expected_type):
                    raise Error(f'''Conditional expression expects {expr.expected_type.repr()} but got {false_type.repr()} as false branch''')
                expr.set_actual_type(expr.expected_type)
            elif astTypes.TypeHelper.is_assignable_to(true_type, false_type):
                expr.set_actual_type(false_type)
            elif astTypes.TypeHelper.is_assignable_to(false_type, true_type):
                expr.set_actual_type(true_type)
            else:
                raise Error(f'''Different types in the whenTrue ({true_type.repr()}) and whenFalse ({false_type.repr()}) expressions of a conditional expression''')
        elif isinstance(expr, exprs.NullCoalesceExpression):
            default_type = expr.default_expr.get_type()
            if_null_type = expr.expr_if_null.get_type()
            if not astTypes.TypeHelper.is_assignable_to(if_null_type, default_type):
                self.error_man.throw(f'''Null-coalescing operator tried to assign incompatible type "{if_null_type.repr()}" to "{default_type.repr()}"''')
            else:
                expr.set_actual_type(default_type)
        elif isinstance(expr, exprs.AwaitExpression):
            expr_type = expr.expr.get_type()
            if isinstance(expr_type, astTypes.ClassType) and expr_type.decl == lit_types.promise.decl:
                expr.set_actual_type((expr_type).type_arguments[0], True)
            else:
                self.error_man.throw(f'''Expected promise type ({lit_types.promise.repr()}) for await expression, but got {expr_type.repr()}''')
        else:
            return False
        
        return True