from OneLangStdLib import *
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.Statements as stats
import OneLang.One.Ast.Types as types
import OneLang.One.Ast.AstTypes as astTypes
import OneLang.One.Ast.References as refs
import OneLang.Generator.GeneratedFile as genFile
import OneLang.Generator.NameUtils as nameUtils
import OneLang.Generator.IGenerator as iGen
import OneLang.One.Ast.Interfaces as ints
import OneLang.One.ITransformer as iTrans
import re

class CsharpGenerator:
    def __init__(self):
        self.usings = None
        self.current_class = None
        self.reserved_words = ["object", "else", "operator", "class", "enum", "void", "string", "implicit", "Type", "Enum", "params", "using", "throw", "ref", "base", "virtual", "interface", "int", "const"]
        self.field_to_method_hack = ["length", "size"]
        self.instance_of_ids = {}
    
    def get_lang_name(self):
        return "CSharp"
    
    def get_extension(self):
        return "cs"
    
    def get_transforms(self):
        return []
    
    def name_(self, name):
        if name in self.reserved_words:
            name += "_"
        if name in self.field_to_method_hack:
            name += "()"
        name_parts = re.split("-", name)
        i = 1
        
        while i < len(name_parts):
            name_parts[i] = name_parts[i][0].upper() + name_parts[i][1:]
            i = i + 1
        name = "".join(name_parts)
        return name
    
    def leading(self, item):
        result = ""
        if item.leading_trivia != None and len(item.leading_trivia) > 0:
            result += item.leading_trivia
        #if (item.attributes !== null)
        #    result += Object.keys(item.attributes).map(x => `# @${x} ${item.attributes[x]}\n`).join("");
        return result
    
    def pre_arr(self, prefix, value):
        return f'''{prefix}{", ".join(value)}''' if len(value) > 0 else ""
    
    def pre_if(self, prefix, condition):
        return prefix if condition else ""
    
    def pre(self, prefix, value):
        return f'''{prefix}{value}''' if value != None else ""
    
    def type_args(self, args):
        return f'''<{", ".join(args)}>''' if args != None and len(args) > 0 else ""
    
    def type_args2(self, args):
        return self.type_args(list(map(lambda x: self.type(x), args)))
    
    def type(self, t, mutates = True):
        if isinstance(t, astTypes.ClassType):
            type_args = self.type_args(list(map(lambda x: self.type(x), t.type_arguments)))
            if t.decl.name == "TsString":
                return "string"
            elif t.decl.name == "TsBoolean":
                return "bool"
            elif t.decl.name == "TsNumber":
                return "int"
            elif t.decl.name == "TsArray":
                if mutates:
                    self.usings["System.Collections.Generic"] = None
                    return f'''List<{self.type(t.type_arguments[0])}>'''
                else:
                    return f'''{self.type(t.type_arguments[0])}[]'''
            elif t.decl.name == "Promise":
                self.usings["System.Threading.Tasks"] = None
                return "Task" if isinstance(t.type_arguments[0], astTypes.VoidType) else f'''Task{type_args}'''
            elif t.decl.name == "Object":
                self.usings["System"] = None
                return f'''object'''
            elif t.decl.name == "TsMap":
                self.usings["System.Collections.Generic"] = None
                return f'''Dictionary<string, {self.type(t.type_arguments[0])}>'''
            return self.name_(t.decl.name) + type_args
        elif isinstance(t, astTypes.InterfaceType):
            return f'''{self.name_(t.decl.name)}{self.type_args(list(map(lambda x: self.type(x), t.type_arguments)))}'''
        elif isinstance(t, astTypes.VoidType):
            return "void"
        elif isinstance(t, astTypes.EnumType):
            return f'''{self.name_(t.decl.name)}'''
        elif isinstance(t, astTypes.AnyType):
            return f'''object'''
        elif isinstance(t, astTypes.NullType):
            return f'''null'''
        elif isinstance(t, astTypes.GenericsType):
            return f'''{t.type_var_name}'''
        elif isinstance(t, astTypes.LambdaType):
            is_func = not (isinstance(t.return_type, astTypes.VoidType))
            param_types = list(map(lambda x: self.type(x.type), t.parameters))
            if is_func:
                param_types.append(self.type(t.return_type))
            self.usings["System"] = None
            return f'''{("Func" if is_func else "Action")}<{", ".join(param_types)}>'''
        elif t == None:
            return "/* TODO */ object"
        else:
            return "/* MISSING */"
    
    def is_ts_array(self, type):
        return isinstance(type, astTypes.ClassType) and type.decl.name == "TsArray"
    
    def vis(self, v):
        return "private" if v == types.VISIBILITY.PRIVATE else "protected" if v == types.VISIBILITY.PROTECTED else "public" if v == types.VISIBILITY.PUBLIC else "/* TODO: not set */public"
    
    def var_wo_init(self, v, attr):
        
        if attr != None and attr.attributes != None and "csharp-type" in attr.attributes:
            type = attr.attributes.get("csharp-type")
        elif isinstance(v.type, astTypes.ClassType) and v.type.decl.name == "TsArray":
            if v.mutability.mutated:
                self.usings["System.Collections.Generic"] = None
                type = f'''List<{self.type(v.type.type_arguments[0])}>'''
            else:
                type = f'''{self.type(v.type.type_arguments[0])}[]'''
        else:
            type = self.type(v.type)
        return f'''{type} {self.name_(v.name)}'''
    
    def var(self, v, attrs):
        return self.var_wo_init(v, attrs) + (f''' = {self.expr(v.initializer)}''' if v.initializer != None else "")
    
    def expr_call(self, type_args, args):
        return self.type_args2(type_args) + f'''({", ".join(list(map(lambda x: self.expr(x), args)))})'''
    
    def mutate_arg(self, arg, should_be_mutable):
        if self.is_ts_array(arg.actual_type):
            if isinstance(arg, exprs.ArrayLiteral) and not should_be_mutable:
                item_type = (arg.actual_type).type_arguments[0]
                return f'''new {self.type(item_type)}[0]''' if len(arg.items) == 0 and not self.is_ts_array(item_type) else f'''new {self.type(item_type)}[] {{ {", ".join(list(map(lambda x: self.expr(x), arg.items)))} }}'''
            
            currently_mutable = should_be_mutable
            if isinstance(arg, refs.VariableReference):
                currently_mutable = arg.get_variable().mutability.mutated
            elif isinstance(arg, exprs.InstanceMethodCallExpression) or isinstance(arg, exprs.StaticMethodCallExpression):
                currently_mutable = False
            
            if currently_mutable and not should_be_mutable:
                return f'''{self.expr(arg)}.ToArray()'''
            elif not currently_mutable and should_be_mutable:
                self.usings["System.Linq"] = None
                return f'''{self.expr(arg)}.ToList()'''
        return self.expr(arg)
    
    def mutated_expr(self, expr, to_where):
        if isinstance(to_where, refs.VariableReference):
            v = to_where.get_variable()
            if self.is_ts_array(v.type):
                return self.mutate_arg(expr, v.mutability.mutated)
        return self.expr(expr)
    
    def call_params(self, args, params):
        arg_reprs = []
        i = 0
        
        while i < len(args):
            arg_reprs.append(self.mutate_arg(args[i], params[i].mutability.mutated) if self.is_ts_array(params[i].type) else self.expr(args[i]))
            i = i + 1
        return f'''({", ".join(arg_reprs)})'''
    
    def method_call(self, expr):
        return self.name_(expr.method.name) + self.type_args2(expr.type_args) + self.call_params(expr.args, expr.method.parameters)
    
    def infer_expr_name_for_type(self, type):
        if isinstance(type, astTypes.ClassType) and ArrayHelper.every(lambda x, _: isinstance(x, astTypes.ClassType), type.type_arguments):
            full_name = "".join(list(map(lambda x: (x).decl.name, type.type_arguments))) + type.decl.name
            return nameUtils.NameUtils.short_name(full_name)
        return None
    
    def expr(self, expr):
        res = "UNKNOWN-EXPR"
        if isinstance(expr, exprs.NewExpression):
            res = f'''new {self.type(expr.cls_)}{self.call_params(expr.args, expr.cls_.decl.constructor_.parameters if expr.cls_.decl.constructor_ != None else [])}'''
        elif isinstance(expr, exprs.UnresolvedNewExpression):
            res = f'''/* TODO: UnresolvedNewExpression */ new {self.type(expr.cls_)}({", ".join(list(map(lambda x: self.expr(x), expr.args)))})'''
        elif isinstance(expr, exprs.Identifier):
            res = f'''/* TODO: Identifier */ {expr.text}'''
        elif isinstance(expr, exprs.PropertyAccessExpression):
            res = f'''/* TODO: PropertyAccessExpression */ {self.expr(expr.object)}.{expr.property_name}'''
        elif isinstance(expr, exprs.UnresolvedCallExpression):
            res = f'''/* TODO: UnresolvedCallExpression */ {self.expr(expr.func)}{self.expr_call(expr.type_args, expr.args)}'''
        elif isinstance(expr, exprs.UnresolvedMethodCallExpression):
            res = f'''/* TODO: UnresolvedMethodCallExpression */ {self.expr(expr.object)}.{expr.method_name}{self.expr_call(expr.type_args, expr.args)}'''
        elif isinstance(expr, exprs.InstanceMethodCallExpression):
            res = f'''{self.expr(expr.object)}.{self.method_call(expr)}'''
        elif isinstance(expr, exprs.StaticMethodCallExpression):
            res = f'''{self.name_(expr.method.parent_interface.name)}.{self.method_call(expr)}'''
        elif isinstance(expr, exprs.GlobalFunctionCallExpression):
            res = f'''Global.{self.name_(expr.func.name)}{self.expr_call([], expr.args)}'''
        elif isinstance(expr, exprs.LambdaCallExpression):
            res = f'''{self.expr(expr.method)}({", ".join(list(map(lambda x: self.expr(x), expr.args)))})'''
        elif isinstance(expr, exprs.BooleanLiteral):
            res = f'''{("true" if expr.bool_value else "false")}'''
        elif isinstance(expr, exprs.StringLiteral):
            res = f'''{JSON.stringify(expr.string_value)}'''
        elif isinstance(expr, exprs.NumericLiteral):
            res = f'''{expr.value_as_text}'''
        elif isinstance(expr, exprs.CharacterLiteral):
            res = f'''\'{expr.char_value}\''''
        elif isinstance(expr, exprs.ElementAccessExpression):
            res = f'''{self.expr(expr.object)}[{self.expr(expr.element_expr)}]'''
        elif isinstance(expr, exprs.TemplateString):
            parts = []
            for part in expr.parts:
                # parts.push(part.literalText.replace(new RegExp("\\n"), $"\\n").replace(new RegExp("\\r"), $"\\r").replace(new RegExp("\\t"), $"\\t").replace(new RegExp("{"), "{{").replace(new RegExp("}"), "}}").replace(new RegExp("\""), $"\\\""));
                if part.is_literal:
                    lit = ""
                    i = 0
                    
                    while i < len(part.literal_text):
                        chr = part.literal_text[i]
                        if chr == "\n":
                            lit += "\\n"
                        elif chr == "\r":
                            lit += "\\r"
                        elif chr == "\t":
                            lit += "\\t"
                        elif chr == "\\":
                            lit += "\\\\"
                        elif chr == "\"":
                            lit += "\\\""
                        elif chr == "{":
                            lit += "{{"
                        elif chr == "}":
                            lit += "}}"
                        else:
                            chr_code = ord(chr[0])
                            if 32 <= chr_code and chr_code <= 126:
                                lit += chr
                            else:
                                raise Error(f'''invalid char in template string (code={chr_code})''')
                        i = i + 1
                    parts.append(lit)
                else:
                    repr = self.expr(part.expression)
                    parts.append(f'''{{({repr})}}''' if isinstance(part.expression, exprs.ConditionalExpression) else f'''{{{repr}}}''')
            res = f'''$"{"".join(parts)}"'''
        elif isinstance(expr, exprs.BinaryExpression):
            res = f'''{self.expr(expr.left)} {expr.operator} {self.mutated_expr(expr.right, expr.left if expr.operator == "=" else None)}'''
        elif isinstance(expr, exprs.ArrayLiteral):
            if len(expr.items) == 0:
                res = f'''new {self.type(expr.actual_type)}()'''
            else:
                res = f'''new {self.type(expr.actual_type)} {{ {", ".join(list(map(lambda x: self.expr(x), expr.items)))} }}'''
        elif isinstance(expr, exprs.CastExpression):
            if expr.instance_of_cast != None and expr.instance_of_cast.alias != None:
                res = self.name_(expr.instance_of_cast.alias)
            else:
                res = f'''(({self.type(expr.new_type)}){self.expr(expr.expression)})'''
        elif isinstance(expr, exprs.ConditionalExpression):
            res = f'''{self.expr(expr.condition)} ? {self.expr(expr.when_true)} : {self.mutated_expr(expr.when_false, expr.when_true)}'''
        elif isinstance(expr, exprs.InstanceOfExpression):
            if expr.implicit_casts != None and len(expr.implicit_casts) > 0:
                alias_prefix = self.infer_expr_name_for_type(expr.check_type)
                if alias_prefix == None:
                    alias_prefix = expr.expr.get_variable().name if isinstance(expr.expr, refs.VariableReference) else "obj"
                id = self.instance_of_ids.get(alias_prefix) if alias_prefix in self.instance_of_ids else 1
                self.instance_of_ids[alias_prefix] = id + 1
                expr.alias = alias_prefix + ("" if id == 1 else f'''{id}''')
            res = f'''{self.expr(expr.expr)} is {self.type(expr.check_type)}{(f' {self.name_(expr.alias)}' if expr.alias != None else "")}'''
        elif isinstance(expr, exprs.ParenthesizedExpression):
            res = f'''({self.expr(expr.expression)})'''
        elif isinstance(expr, exprs.RegexLiteral):
            res = f'''new RegExp({JSON.stringify(expr.pattern)})'''
        elif isinstance(expr, types.Lambda):
            
            if len(expr.body.statements) == 1 and isinstance(expr.body.statements[0], stats.ReturnStatement):
                body = self.expr((expr.body.statements[0]).expression)
            else:
                body = f'''{{ {self.raw_block(expr.body)} }}'''
            
            params = list(map(lambda x: self.name_(x.name), expr.parameters))
            
            res = f'''{(params[0] if len(params) == 1 else f'({", ".join(params)})')} => {body}'''
        elif isinstance(expr, exprs.UnaryExpression) and expr.unary_type == exprs.UNARY_TYPE.PREFIX:
            res = f'''{expr.operator}{self.expr(expr.operand)}'''
        elif isinstance(expr, exprs.UnaryExpression) and expr.unary_type == exprs.UNARY_TYPE.POSTFIX:
            res = f'''{self.expr(expr.operand)}{expr.operator}'''
        elif isinstance(expr, exprs.MapLiteral):
            repr = ",\n".join(list(map(lambda item: f'''[{JSON.stringify(item.key)}] = {self.expr(item.value)}''', expr.items)))
            res = f'''new {self.type(expr.actual_type)} ''' + ("{}" if repr == "" else f'''{{\n{self.pad(repr)}\n}}''' if "\n" in repr else f'''{{ {repr} }}''')
        elif isinstance(expr, exprs.NullLiteral):
            res = f'''null'''
        elif isinstance(expr, exprs.AwaitExpression):
            res = f'''await {self.expr(expr.expr)}'''
        elif isinstance(expr, refs.ThisReference):
            res = f'''this'''
        elif isinstance(expr, refs.StaticThisReference):
            res = f'''{self.current_class.name}'''
        elif isinstance(expr, refs.EnumReference):
            res = f'''{self.name_(expr.decl.name)}'''
        elif isinstance(expr, refs.ClassReference):
            res = f'''{self.name_(expr.decl.name)}'''
        elif isinstance(expr, refs.MethodParameterReference):
            res = f'''{self.name_(expr.decl.name)}'''
        elif isinstance(expr, refs.VariableDeclarationReference):
            res = f'''{self.name_(expr.decl.name)}'''
        elif isinstance(expr, refs.ForVariableReference):
            res = f'''{self.name_(expr.decl.name)}'''
        elif isinstance(expr, refs.ForeachVariableReference):
            res = f'''{self.name_(expr.decl.name)}'''
        elif isinstance(expr, refs.CatchVariableReference):
            res = f'''{self.name_(expr.decl.name)}'''
        elif isinstance(expr, refs.GlobalFunctionReference):
            res = f'''{self.name_(expr.decl.name)}'''
        elif isinstance(expr, refs.SuperReference):
            res = f'''base'''
        elif isinstance(expr, refs.StaticFieldReference):
            res = f'''{self.name_(expr.decl.parent_interface.name)}.{self.name_(expr.decl.name)}'''
        elif isinstance(expr, refs.StaticPropertyReference):
            res = f'''{self.name_(expr.decl.parent_class.name)}.{self.name_(expr.decl.name)}'''
        elif isinstance(expr, refs.InstanceFieldReference):
            res = f'''{self.expr(expr.object)}.{self.name_(expr.field.name)}'''
        elif isinstance(expr, refs.InstancePropertyReference):
            res = f'''{self.expr(expr.object)}.{self.name_(expr.property.name)}'''
        elif isinstance(expr, refs.EnumMemberReference):
            res = f'''{self.name_(expr.decl.parent_enum.name)}.{self.name_(expr.decl.name)}'''
        elif isinstance(expr, exprs.NullCoalesceExpression):
            res = f'''{self.expr(expr.default_expr)} ?? {self.mutated_expr(expr.expr_if_null, expr.default_expr)}'''
        else:
            pass
        return res
    
    def block(self, block, allow_one_liner = True):
        stmt_len = len(block.statements)
        return " { }" if stmt_len == 0 else f'''\n{self.pad(self.raw_block(block))}''' if allow_one_liner and stmt_len == 1 and not (isinstance(block.statements[0], stats.IfStatement)) else f''' {{\n{self.pad(self.raw_block(block))}\n}}'''
    
    def stmt(self, stmt):
        res = "UNKNOWN-STATEMENT"
        if stmt.attributes != None and "csharp" in stmt.attributes:
            res = stmt.attributes.get("csharp")
        elif isinstance(stmt, stats.BreakStatement):
            res = "break;"
        elif isinstance(stmt, stats.ReturnStatement):
            res = "return;" if stmt.expression == None else f'''return {self.mutate_arg(stmt.expression, False)};'''
        elif isinstance(stmt, stats.UnsetStatement):
            res = f'''/* unset {self.expr(stmt.expression)}; */'''
        elif isinstance(stmt, stats.ThrowStatement):
            res = f'''throw {self.expr(stmt.expression)};'''
        elif isinstance(stmt, stats.ExpressionStatement):
            res = f'''{self.expr(stmt.expression)};'''
        elif isinstance(stmt, stats.VariableDeclaration):
            if isinstance(stmt.initializer, exprs.NullLiteral):
                res = f'''{self.type(stmt.type, stmt.mutability.mutated)} {self.name_(stmt.name)} = null;'''
            elif stmt.initializer != None:
                res = f'''var {self.name_(stmt.name)} = {self.mutate_arg(stmt.initializer, stmt.mutability.mutated)};'''
            else:
                res = f'''{self.type(stmt.type)} {self.name_(stmt.name)};'''
        elif isinstance(stmt, stats.ForeachStatement):
            res = f'''foreach (var {self.name_(stmt.item_var.name)} in {self.expr(stmt.items)})''' + self.block(stmt.body)
        elif isinstance(stmt, stats.IfStatement):
            else_if = stmt.else_ != None and len(stmt.else_.statements) == 1 and isinstance(stmt.else_.statements[0], stats.IfStatement)
            res = f'''if ({self.expr(stmt.condition)}){self.block(stmt.then)}'''
            res += (f'''\nelse {self.stmt(stmt.else_.statements[0])}''' if else_if else "") + (f'''\nelse''' + self.block(stmt.else_) if not else_if and stmt.else_ != None else "")
        elif isinstance(stmt, stats.WhileStatement):
            res = f'''while ({self.expr(stmt.condition)})''' + self.block(stmt.body)
        elif isinstance(stmt, stats.ForStatement):
            res = f'''for ({(self.var(stmt.item_var, None) if stmt.item_var != None else "")}; {self.expr(stmt.condition)}; {self.expr(stmt.incrementor)})''' + self.block(stmt.body)
        elif isinstance(stmt, stats.DoStatement):
            res = f'''do{self.block(stmt.body)} while ({self.expr(stmt.condition)});'''
        elif isinstance(stmt, stats.TryStatement):
            res = "try" + self.block(stmt.try_body, False)
            if stmt.catch_body != None:
                self.usings["System"] = None
                res += f''' catch (Exception {self.name_(stmt.catch_var.name)}) {self.block(stmt.catch_body, False)}'''
            if stmt.finally_body != None:
                res += "finally" + self.block(stmt.finally_body)
        elif isinstance(stmt, stats.ContinueStatement):
            res = f'''continue;'''
        else:
            pass
        return self.leading(stmt) + res
    
    def stmts(self, stmts):
        return "\n".join(list(map(lambda stmt: self.stmt(stmt), stmts)))
    
    def raw_block(self, block):
        return self.stmts(block.statements)
    
    def class_like(self, cls_):
        self.current_class = cls_
        res_list = []
        
        static_constructor_stmts = []
        complex_field_inits = []
        if isinstance(cls_, types.Class):
            field_reprs = []
            for field in cls_.fields:
                is_initializer_complex = field.initializer != None and not (isinstance(field.initializer, exprs.StringLiteral)) and not (isinstance(field.initializer, exprs.BooleanLiteral)) and not (isinstance(field.initializer, exprs.NumericLiteral))
                
                prefix = f'''{self.vis(field.visibility)} {self.pre_if("static ", field.is_static)}'''
                if len(field.interface_declarations) > 0:
                    field_reprs.append(f'''{prefix}{self.var_wo_init(field, field)} {{ get; set; }}''')
                elif is_initializer_complex:
                    if field.is_static:
                        static_constructor_stmts.append(stats.ExpressionStatement(exprs.BinaryExpression(refs.StaticFieldReference(field), "=", field.initializer)))
                    else:
                        complex_field_inits.append(stats.ExpressionStatement(exprs.BinaryExpression(refs.InstanceFieldReference(refs.ThisReference(cls_), field), "=", field.initializer)))
                    
                    field_reprs.append(f'''{prefix}{self.var_wo_init(field, field)};''')
                else:
                    field_reprs.append(f'''{prefix}{self.var(field, field)};''')
            res_list.append("\n".join(field_reprs))
            
            res_list.append("\n".join(list(map(lambda prop: f'''{self.vis(prop.visibility)} {self.pre_if("static ", prop.is_static)}''' + self.var_wo_init(prop, prop) + (f''' {{\n    get {{\n{self.pad(self.block(prop.getter))}\n    }}\n}}''' if prop.getter != None else "") + (f''' {{\n    set {{\n{self.pad(self.block(prop.setter))}\n    }}\n}}''' if prop.setter != None else ""), cls_.properties))))
            
            if len(static_constructor_stmts) > 0:
                res_list.append(f'''static {self.name_(cls_.name)}()\n{{\n{self.pad(self.stmts(static_constructor_stmts))}\n}}''')
            
            if cls_.constructor_ != None:
                constr_field_inits = []
                for field in list(filter(lambda x: x.constructor_param != None, cls_.fields)):
                    field_ref = refs.InstanceFieldReference(refs.ThisReference(cls_), field)
                    mp_ref = refs.MethodParameterReference(field.constructor_param)
                    # TODO: decide what to do with "after-TypeEngine" transformations
                    mp_ref.set_actual_type(field.type, False, False)
                    constr_field_inits.append(stats.ExpressionStatement(exprs.BinaryExpression(field_ref, "=", mp_ref)))
                
                # @java var stmts = Stream.concat(Stream.concat(constrFieldInits.stream(), complexFieldInits.stream()), ((Class)cls).constructor_.getBody().statements.stream()).toArray(Statement[]::new);
                # @java-import java.util.stream.Stream
                stmts = constr_field_inits + complex_field_inits + cls_.constructor_.body.statements
                res_list.append("public " + self.pre_if("/* throws */ ", cls_.constructor_.throws) + self.name_(cls_.name) + f'''({", ".join(list(map(lambda p: self.var(p, p), cls_.constructor_.parameters)))})''' + (f''': base({", ".join(list(map(lambda x: self.expr(x), cls_.constructor_.super_call_args)))})''' if cls_.constructor_.super_call_args != None else "") + f'''\n{{\n{self.pad(self.stmts(stmts))}\n}}''')
            elif len(complex_field_inits) > 0:
                res_list.append(f'''public {self.name_(cls_.name)}()\n{{\n{self.pad(self.stmts(complex_field_inits))}\n}}''')
        elif isinstance(cls_, types.Interface):
            res_list.append("\n".join(list(map(lambda field: f'''{self.var_wo_init(field, field)} {{ get; set; }}''', cls_.fields))))
        
        methods = []
        for method in cls_.methods:
            if isinstance(cls_, types.Class) and method.body == None:
                continue
            # declaration only
            methods.append(("" if isinstance(method.parent_interface, types.Interface) else self.vis(method.visibility) + " ") + self.pre_if("static ", method.is_static) + self.pre_if("virtual ", method.overrides == None and len(method.overridden_by) > 0) + self.pre_if("override ", method.overrides != None) + self.pre_if("async ", method.async_) + self.pre_if("/* throws */ ", method.throws) + f'''{self.type(method.returns, False)} ''' + self.name_(method.name) + self.type_args(method.type_arguments) + f'''({", ".join(list(map(lambda p: self.var(p, None), method.parameters)))})''' + (f'''\n{{\n{self.pad(self.stmts(method.body.statements))}\n}}''' if method.body != None else ";"))
        res_list.append("\n\n".join(methods))
        return self.pad("\n\n".join(list(filter(lambda x: x != "", res_list))))
    
    def pad(self, str):
        return "\n".join(list(map(lambda x: f'''    {x}''', re.split("\\n", str))))
    
    def path_to_ns(self, path):
        # Generator/ExprLang/ExprLangAst.ts -> Generator.ExprLang
        parts = re.split("/", path)
        parts.pop()
        return ".".join(parts)
    
    def gen_file(self, source_file):
        self.instance_of_ids = {}
        self.usings = dict()
        enums = list(map(lambda enum_: f'''public enum {self.name_(enum_.name)} {{ {", ".join(list(map(lambda x: self.name_(x.name), enum_.values)))} }}''', source_file.enums))
        
        intfs = list(map(lambda intf: f'''public interface {self.name_(intf.name)}{self.type_args(intf.type_arguments)}''' + f'''{self.pre_arr(" : ", list(map(lambda x: self.type(x), intf.base_interfaces)))} {{\n{self.class_like(intf)}\n}}''', source_file.interfaces))
        
        classes = []
        for cls_ in source_file.classes:
            base_classes = []
            if cls_.base_class != None:
                base_classes.append(cls_.base_class)
            for intf in cls_.base_interfaces:
                base_classes.append(intf)
            classes.append(f'''public class {self.name_(cls_.name)}{self.type_args(cls_.type_arguments)}{self.pre_arr(" : ", list(map(lambda x: self.type(x), base_classes)))} {{\n{self.class_like(cls_)}\n}}''')
        
        main = f'''public class Program\n{{\n    static void Main(string[] args)\n    {{\n{self.pad(self.raw_block(source_file.main_block))}\n    }}\n}}''' if len(source_file.main_block.statements) > 0 else ""
        
        # @java var usingsSet = new LinkedHashSet<String>(Arrays.stream(sourceFile.imports).map(x -> this.pathToNs(x.exportScope.scopeName)).filter(x -> x != "").collect(Collectors.toList()));
        # @java-import java.util.LinkedHashSet
        usings_set = dict.fromkeys(list(filter(lambda x: x != "", list(map(lambda x: self.path_to_ns(x.export_scope.scope_name), source_file.imports)))))
        for using in self.usings.keys():
            usings_set[using] = None
        
        usings = []
        for using in usings_set.keys():
            usings.append(f'''using {using};''')
        usings.sort()
        
        result = "\n\n".join(list(filter(lambda x: x != "", ["\n".join(enums), "\n\n".join(intfs), "\n\n".join(classes), main])))
        nl = "\n"
        # Python fix
        result = f'''{nl.join(usings)}\n\nnamespace {self.path_to_ns(source_file.source_path.path)}\n{{\n{self.pad(result)}\n}}'''
        return result
    
    def generate(self, pkg):
        result = []
        for path in pkg.files.keys():
            result.append(genFile.GeneratedFile(path, self.gen_file(pkg.files.get(path))))
        return result