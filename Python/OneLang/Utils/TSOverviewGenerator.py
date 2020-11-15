from OneLangStdLib import *
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.Statements as stats
import OneLang.One.Ast.Types as types
import OneLang.One.Ast.AstTypes as astTypes
import OneLang.One.Ast.References as refs
import OneLang.One.Ast.Interfaces as ints
import OneLangStdLib as one
import re

@one.static_init
class TSOverviewGenerator:
    @classmethod
    def static_init(cls):
        cls.preview = cls(True)
    
    def __init__(self, preview_only = False, show_types = False):
        self.preview_only = preview_only
        self.show_types = show_types
    
    def leading(self, item):
        result = ""
        if item.leading_trivia != None and len(item.leading_trivia) > 0:
            result += item.leading_trivia
        if item.attributes != None:
            result += "".join(list(map(lambda x: f'''/// {{ATTR}} name="{x}", value={JSON.stringify(item.attributes.get(x))}\n''', item.attributes.keys())))
        return result
    
    def pre_arr(self, prefix, value):
        return f'''{prefix}{", ".join(value)}''' if len(value) > 0 else ""
    
    def pre_if(self, prefix, condition):
        return prefix if condition else ""
    
    def pre(self, prefix, value):
        return f'''{prefix}{value}''' if value != None else ""
    
    def type_args(self, args):
        return f'''<{", ".join(args)}>''' if args != None and len(args) > 0 else ""
    
    def type(self, t, raw = False):
        repr = "???" if t == None else t.repr()
        if repr == "U:UNKNOWN":
            pass
        return ("" if raw else "{T}") + repr
    
    def var(self, v):
        result = ""
        is_prop = isinstance(v, types.Property)
        if isinstance(v, types.Field) or isinstance(v, types.Property):
            m = v
            result += self.pre_if("", m.is_static)
            result += "private " if m.visibility == types.VISIBILITY.PRIVATE else "protected " if m.visibility == types.VISIBILITY.PROTECTED else "public " if m.visibility == types.VISIBILITY.PUBLIC else "VISIBILITY-NOT-SET"
        result += f'''{("@prop " if is_prop else "")}'''
        if v.mutability != None:
            result += f'''{("@unused " if v.mutability.unused else "")}'''
            result += f'''{("@mutated " if v.mutability.mutated else "")}'''
            result += f'''{("@reass " if v.mutability.reassigned else "")}'''
        result += f'''{v.name}{("()" if is_prop else "")}: {self.type(v.type)}'''
        if isinstance(v, stats.VariableDeclaration) or isinstance(v, stats.ForVariable) or isinstance(v, types.Field) or isinstance(v, types.MethodParameter):
            init = (v).initializer
            if init != None:
                result += self.pre(" = ", self.expr(init))
        return result
    
    def expr(self, expr):
        res = "UNKNOWN-EXPR"
        if isinstance(expr, exprs.NewExpression):
            res = f'''new {self.type(expr.cls_)}({("..." if self.preview_only else ", ".join(list(map(lambda x: self.expr(x), expr.args))))})'''
        elif isinstance(expr, exprs.UnresolvedNewExpression):
            res = f'''new {self.type(expr.cls_)}({("..." if self.preview_only else ", ".join(list(map(lambda x: self.expr(x), expr.args))))})'''
        elif isinstance(expr, exprs.Identifier):
            res = f'''{{ID}}{expr.text}'''
        elif isinstance(expr, exprs.PropertyAccessExpression):
            res = f'''{self.expr(expr.object)}.{{PA}}{expr.property_name}'''
        elif isinstance(expr, exprs.UnresolvedCallExpression):
            type_args = f'''<{", ".join(list(map(lambda x: self.type(x), expr.type_args)))}>''' if len(expr.type_args) > 0 else ""
            res = f'''{self.expr(expr.func)}{type_args}({("..." if self.preview_only else ", ".join(list(map(lambda x: self.expr(x), expr.args))))})'''
        elif isinstance(expr, exprs.UnresolvedMethodCallExpression):
            type_args = f'''<{", ".join(list(map(lambda x: self.type(x), expr.type_args)))}>''' if len(expr.type_args) > 0 else ""
            res = f'''{self.expr(expr.object)}.{{UM}}{expr.method_name}{type_args}({("..." if self.preview_only else ", ".join(list(map(lambda x: self.expr(x), expr.args))))})'''
        elif isinstance(expr, exprs.InstanceMethodCallExpression):
            type_args = f'''<{", ".join(list(map(lambda x: self.type(x), expr.type_args)))}>''' if len(expr.type_args) > 0 else ""
            res = f'''{self.expr(expr.object)}.{{M}}{expr.method.name}{type_args}({("..." if self.preview_only else ", ".join(list(map(lambda x: self.expr(x), expr.args))))})'''
        elif isinstance(expr, exprs.StaticMethodCallExpression):
            type_args = f'''<{", ".join(list(map(lambda x: self.type(x), expr.type_args)))}>''' if len(expr.type_args) > 0 else ""
            res = f'''{expr.method.parent_interface.name}.{{M}}{expr.method.name}{type_args}({("..." if self.preview_only else ", ".join(list(map(lambda x: self.expr(x), expr.args))))})'''
        elif isinstance(expr, exprs.GlobalFunctionCallExpression):
            res = f'''{expr.func.name}({("..." if self.preview_only else ", ".join(list(map(lambda x: self.expr(x), expr.args))))})'''
        elif isinstance(expr, exprs.LambdaCallExpression):
            res = f'''{self.expr(expr.method)}({("..." if self.preview_only else ", ".join(list(map(lambda x: self.expr(x), expr.args))))})'''
        elif isinstance(expr, exprs.BooleanLiteral):
            res = f'''{("true" if expr.bool_value else "false")}'''
        elif isinstance(expr, exprs.StringLiteral):
            res = f'''{JSON.stringify(expr.string_value)}'''
        elif isinstance(expr, exprs.NumericLiteral):
            res = f'''{expr.value_as_text}'''
        elif isinstance(expr, exprs.CharacterLiteral):
            res = f'''\'{expr.char_value}\''''
        elif isinstance(expr, exprs.ElementAccessExpression):
            res = f'''({self.expr(expr.object)})[{self.expr(expr.element_expr)}]'''
        elif isinstance(expr, exprs.TemplateString):
            res = "`" + "".join(list(map(lambda x: x.literal_text if x.is_literal else "${" + self.expr(x.expression) + "}", expr.parts))) + "`"
        elif isinstance(expr, exprs.BinaryExpression):
            res = f'''{self.expr(expr.left)} {expr.operator} {self.expr(expr.right)}'''
        elif isinstance(expr, exprs.ArrayLiteral):
            res = f'''[{", ".join(list(map(lambda x: self.expr(x), expr.items)))}]'''
        elif isinstance(expr, exprs.CastExpression):
            res = f'''<{self.type(expr.new_type)}>({self.expr(expr.expression)})'''
        elif isinstance(expr, exprs.ConditionalExpression):
            res = f'''{self.expr(expr.condition)} ? {self.expr(expr.when_true)} : {self.expr(expr.when_false)}'''
        elif isinstance(expr, exprs.InstanceOfExpression):
            res = f'''{self.expr(expr.expr)} instanceof {self.type(expr.check_type)}'''
        elif isinstance(expr, exprs.ParenthesizedExpression):
            res = f'''({self.expr(expr.expression)})'''
        elif isinstance(expr, exprs.RegexLiteral):
            res = f'''/{expr.pattern}/{("g" if expr.global_ else "")}{("g" if expr.case_insensitive else "")}'''
        elif isinstance(expr, types.Lambda):
            res = f'''({", ".join(list(map(lambda x: x.name + (": " + self.type(x.type) if x.type != None else ""), expr.parameters)))})''' + (f''' @captures({", ".join(list(map(lambda x: x.name, expr.captures)))})''' if expr.captures != None and len(expr.captures) > 0 else "") + f''' => {{ {self.raw_block(expr.body)} }}'''
        elif isinstance(expr, exprs.UnaryExpression) and expr.unary_type == exprs.UNARY_TYPE.PREFIX:
            res = f'''{expr.operator}{self.expr(expr.operand)}'''
        elif isinstance(expr, exprs.UnaryExpression) and expr.unary_type == exprs.UNARY_TYPE.POSTFIX:
            res = f'''{self.expr(expr.operand)}{expr.operator}'''
        elif isinstance(expr, exprs.MapLiteral):
            repr = ",\n".join(list(map(lambda item: f'''{item.key}: {self.expr(item.value)}''', expr.items)))
            res = "{L:M}" + ("{}" if repr == "" else f'''{{\n{self.pad(repr)}\n}}''' if "\n" in repr else f'''{{ {repr} }}''')
        elif isinstance(expr, exprs.NullLiteral):
            res = f'''null'''
        elif isinstance(expr, exprs.AwaitExpression):
            res = f'''await {self.expr(expr.expr)}'''
        elif isinstance(expr, refs.ThisReference):
            res = f'''{{R}}this'''
        elif isinstance(expr, refs.StaticThisReference):
            res = f'''{{R:Static}}this'''
        elif isinstance(expr, refs.EnumReference):
            res = f'''{{R:Enum}}{expr.decl.name}'''
        elif isinstance(expr, refs.ClassReference):
            res = f'''{{R:Cls}}{expr.decl.name}'''
        elif isinstance(expr, refs.MethodParameterReference):
            res = f'''{{R:MetP}}{expr.decl.name}'''
        elif isinstance(expr, refs.VariableDeclarationReference):
            res = f'''{{V}}{expr.decl.name}'''
        elif isinstance(expr, refs.ForVariableReference):
            res = f'''{{R:ForV}}{expr.decl.name}'''
        elif isinstance(expr, refs.ForeachVariableReference):
            res = f'''{{R:ForEV}}{expr.decl.name}'''
        elif isinstance(expr, refs.CatchVariableReference):
            res = f'''{{R:CatchV}}{expr.decl.name}'''
        elif isinstance(expr, refs.GlobalFunctionReference):
            res = f'''{{R:GFunc}}{expr.decl.name}'''
        elif isinstance(expr, refs.SuperReference):
            res = f'''{{R}}super'''
        elif isinstance(expr, refs.StaticFieldReference):
            res = f'''{{R:StFi}}{expr.decl.parent_interface.name}::{expr.decl.name}'''
        elif isinstance(expr, refs.StaticPropertyReference):
            res = f'''{{R:StPr}}{expr.decl.parent_class.name}::{expr.decl.name}'''
        elif isinstance(expr, refs.InstanceFieldReference):
            res = f'''{self.expr(expr.object)}.{{F}}{expr.field.name}'''
        elif isinstance(expr, refs.InstancePropertyReference):
            res = f'''{self.expr(expr.object)}.{{P}}{expr.property.name}'''
        elif isinstance(expr, refs.EnumMemberReference):
            res = f'''{{E}}{expr.decl.parent_enum.name}::{expr.decl.name}'''
        elif isinstance(expr, exprs.NullCoalesceExpression):
            res = f'''{self.expr(expr.default_expr)} ?? {self.expr(expr.expr_if_null)}'''
        else:
            pass
        
        if self.show_types:
            res = f'''<{self.type(expr.get_type(), True)}>({res})'''
        
        return res
    
    def block(self, block, allow_one_liner = True):
        if self.preview_only:
            return " { ... }"
        stmt_len = len(block.statements)
        return " { }" if stmt_len == 0 else f'''\n{self.pad(self.raw_block(block))}''' if allow_one_liner and stmt_len == 1 else f''' {{\n{self.pad(self.raw_block(block))}\n}}'''
    
    def stmt(self, stmt):
        res = "UNKNOWN-STATEMENT"
        if isinstance(stmt, stats.BreakStatement):
            res = "break;"
        elif isinstance(stmt, stats.ReturnStatement):
            res = "return;" if stmt.expression == None else f'''return {self.expr(stmt.expression)};'''
        elif isinstance(stmt, stats.UnsetStatement):
            res = f'''unset {self.expr(stmt.expression)};'''
        elif isinstance(stmt, stats.ThrowStatement):
            res = f'''throw {self.expr(stmt.expression)};'''
        elif isinstance(stmt, stats.ExpressionStatement):
            res = f'''{self.expr(stmt.expression)};'''
        elif isinstance(stmt, stats.VariableDeclaration):
            res = f'''var {self.var(stmt)};'''
        elif isinstance(stmt, stats.ForeachStatement):
            res = f'''for (const {stmt.item_var.name} of {self.expr(stmt.items)})''' + self.block(stmt.body)
        elif isinstance(stmt, stats.IfStatement):
            else_if = stmt.else_ != None and len(stmt.else_.statements) == 1 and isinstance(stmt.else_.statements[0], stats.IfStatement)
            res = f'''if ({self.expr(stmt.condition)}){self.block(stmt.then)}'''
            if not self.preview_only:
                res += (f'''\nelse {self.stmt(stmt.else_.statements[0])}''' if else_if else "") + (f'''\nelse''' + self.block(stmt.else_) if not else_if and stmt.else_ != None else "")
        elif isinstance(stmt, stats.WhileStatement):
            res = f'''while ({self.expr(stmt.condition)})''' + self.block(stmt.body)
        elif isinstance(stmt, stats.ForStatement):
            res = f'''for ({(self.var(stmt.item_var) if stmt.item_var != None else "")}; {self.expr(stmt.condition)}; {self.expr(stmt.incrementor)})''' + self.block(stmt.body)
        elif isinstance(stmt, stats.DoStatement):
            res = f'''do{self.block(stmt.body)} while ({self.expr(stmt.condition)})'''
        elif isinstance(stmt, stats.TryStatement):
            res = "try" + self.block(stmt.try_body, False) + (f''' catch ({stmt.catch_var.name}){self.block(stmt.catch_body)}''' if stmt.catch_body != None else "") + ("finally" + self.block(stmt.finally_body) if stmt.finally_body != None else "")
        elif isinstance(stmt, stats.ContinueStatement):
            res = f'''continue;'''
        else:
            pass
        return res if self.preview_only else self.leading(stmt) + res
    
    def raw_block(self, block):
        return "\n".join(list(map(lambda stmt: self.stmt(stmt), block.statements)))
    
    def method_base(self, method, returns):
        if method == None:
            return ""
        name = method.name if isinstance(method, types.Method) else "constructor" if isinstance(method, types.Constructor) else method.name if isinstance(method, types.GlobalFunction) else "???"
        type_args = method.type_arguments if isinstance(method, types.Method) else None
        return self.pre_if("/* throws */ ", method.throws) + f'''{name}{self.type_args(type_args)}({", ".join(list(map(lambda p: self.leading(p) + self.var(p), method.parameters)))})''' + ("" if isinstance(returns, astTypes.VoidType) else f''': {self.type(returns)}''') + (f''' {{\n{self.pad(self.raw_block(method.body))}\n}}''' if method.body != None else ";")
    
    def method(self, method):
        return "" if method == None else ("static " if method.is_static else "") + ("@mutates " if method.attributes != None and "mutates" in method.attributes else "") + self.method_base(method, method.returns)
    
    def class_like(self, cls_):
        res_list = []
        res_list.append("\n".join(list(map(lambda field: self.var(field) + ";", cls_.fields))))
        if isinstance(cls_, types.Class):
            res_list.append("\n".join(list(map(lambda prop: self.var(prop) + ";", cls_.properties))))
            res_list.append(self.method_base(cls_.constructor_, astTypes.VoidType.instance))
        res_list.append("\n\n".join(list(map(lambda method: self.method(method), cls_.methods))))
        return self.pad("\n\n".join(list(filter(lambda x: x != "", res_list))))
    
    def pad(self, str):
        return "\n".join(list(map(lambda x: f'''    {x}''', re.split("\\n", str))))
    
    def imp(self, imp):
        return "" + ("X" if isinstance(imp, types.UnresolvedImport) else "C" if isinstance(imp, types.Class) else "I" if isinstance(imp, types.Interface) else "E" if isinstance(imp, types.Enum) else "???") + f''':{imp.name}'''
    
    def node_repr(self, node):
        if isinstance(node, stats.Statement):
            return self.stmt(node)
        elif isinstance(node, exprs.Expression):
            return self.expr(node)
        else:
            return "/* TODO: missing */"
    
    def generate(self, source_file):
        imps = list(map(lambda imp: (f'''import * as {imp.import_as}''' if imp.import_all else f'''import {{ {", ".join(list(map(lambda x: self.imp(x), imp.imports)))} }}''') + f''' from "{imp.export_scope.package_name}{self.pre("/", imp.export_scope.scope_name)}";''', source_file.imports))
        enums = list(map(lambda enum_: f'''{self.leading(enum_)}enum {enum_.name} {{ {", ".join(list(map(lambda x: x.name, enum_.values)))} }}''', source_file.enums))
        intfs = list(map(lambda intf: f'''{self.leading(intf)}interface {intf.name}{self.type_args(intf.type_arguments)}''' + f'''{self.pre_arr(" extends ", list(map(lambda x: self.type(x), intf.base_interfaces)))} {{\n{self.class_like(intf)}\n}}''', source_file.interfaces))
        classes = list(map(lambda cls_: f'''{self.leading(cls_)}class {cls_.name}{self.type_args(cls_.type_arguments)}''' + self.pre(" extends ", self.type(cls_.base_class) if cls_.base_class != None else None) + self.pre_arr(" implements ", list(map(lambda x: self.type(x), cls_.base_interfaces))) + f''' {{\n{self.class_like(cls_)}\n}}''', source_file.classes))
        funcs = list(map(lambda func: f'''{self.leading(func)}function {func.name}{self.method_base(func, func.returns)}''', source_file.funcs))
        main = self.raw_block(source_file.main_block)
        result = f'''// export scope: {source_file.export_scope.package_name}/{source_file.export_scope.scope_name}\n''' + "\n\n".join(list(filter(lambda x: x != "", ["\n".join(imps), "\n".join(enums), "\n\n".join(intfs), "\n\n".join(classes), "\n\n".join(funcs), main])))
        return result