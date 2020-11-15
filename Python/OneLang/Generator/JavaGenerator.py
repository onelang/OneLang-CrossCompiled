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
import OneLang.Generator.IGeneratorPlugin as iGenPlug
import OneLang.Generator.JavaPlugins.JsToJava as jsToJava
import OneLang.One.ITransformer as iTrans
import OneLang.One.Transforms.ConvertNullCoalesce as convNullCoal
import OneLang.One.Transforms.UseDefaultCallArgsExplicitly as useDefCallArgsExpl
import re

class JavaGenerator:
    def __init__(self):
        self.imports = dict()
        self.current_class = None
        self.reserved_words = ["class", "interface", "throws", "package", "throw", "boolean"]
        self.field_to_method_hack = []
        self.plugins = []
        self.plugins.append(jsToJava.JsToJava(self))
    
    def get_lang_name(self):
        return "Java"
    
    def get_extension(self):
        return "java"
    
    def get_transforms(self):
        return [convNullCoal.ConvertNullCoalesce(), useDefCallArgsExpl.UseDefaultCallArgsExplicitly()]
    
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
        if name == "_":
            name = "unused"
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
    
    def type(self, t, mutates = True, is_new = False):
        if isinstance(t, astTypes.ClassType) or isinstance(t, astTypes.InterfaceType):
            decl = (t).get_decl()
            if decl.parent_file.export_scope != None:
                self.imports[self.to_import(decl.parent_file.export_scope) + "." + decl.name] = None
        
        if isinstance(t, astTypes.ClassType):
            type_args = self.type_args(list(map(lambda x: self.type(x), t.type_arguments)))
            if t.decl.name == "TsString":
                return "String"
            elif t.decl.name == "TsBoolean":
                return "Boolean"
            elif t.decl.name == "TsNumber":
                return "Integer"
            elif t.decl.name == "TsArray":
                real_type = "ArrayList" if is_new else "List"
                if mutates:
                    self.imports[f'''java.util.{real_type}'''] = None
                    return f'''{real_type}<{self.type(t.type_arguments[0])}>'''
                else:
                    return f'''{self.type(t.type_arguments[0])}[]'''
            elif t.decl.name == "Map":
                real_type = "LinkedHashMap" if is_new else "Map"
                self.imports[f'''java.util.{real_type}'''] = None
                return f'''{real_type}<{self.type(t.type_arguments[0])}, {self.type(t.type_arguments[1])}>'''
            elif t.decl.name == "Set":
                real_type = "LinkedHashSet" if is_new else "Set"
                self.imports[f'''java.util.{real_type}'''] = None
                return f'''{real_type}<{self.type(t.type_arguments[0])}>'''
            elif t.decl.name == "Promise":
                return "void" if isinstance(t.type_arguments[0], astTypes.VoidType) else f'''{self.type(t.type_arguments[0])}'''
            elif t.decl.name == "Object":
                #this.imports.add("System");
                return f'''Object'''
            elif t.decl.name == "TsMap":
                real_type = "LinkedHashMap" if is_new else "Map"
                self.imports[f'''java.util.{real_type}'''] = None
                return f'''{real_type}<String, {self.type(t.type_arguments[0])}>'''
            return self.name_(t.decl.name) + type_args
        elif isinstance(t, astTypes.InterfaceType):
            return f'''{self.name_(t.decl.name)}{self.type_args(list(map(lambda x: self.type(x), t.type_arguments)))}'''
        elif isinstance(t, astTypes.VoidType):
            return "void"
        elif isinstance(t, astTypes.EnumType):
            return f'''{self.name_(t.decl.name)}'''
        elif isinstance(t, astTypes.AnyType):
            return f'''Object'''
        elif isinstance(t, astTypes.NullType):
            return f'''null'''
        elif isinstance(t, astTypes.GenericsType):
            return f'''{t.type_var_name}'''
        elif isinstance(t, astTypes.LambdaType):
            is_func = not (isinstance(t.return_type, astTypes.VoidType))
            param_types = list(map(lambda x: self.type(x.type), t.parameters))
            if is_func:
                param_types.append(self.type(t.return_type))
            self.imports["java.util.function." + ("Function" if is_func else "Consumer")] = None
            return f'''{("Function" if is_func else "Consumer")}<{", ".join(param_types)}>'''
        elif t == None:
            return "/* TODO */ object"
        else:
            return "/* MISSING */"
    
    def is_ts_array(self, type):
        return isinstance(type, astTypes.ClassType) and type.decl.name == "TsArray"
    
    def vis(self, v):
        return "private" if v == types.VISIBILITY.PRIVATE else "protected" if v == types.VISIBILITY.PROTECTED else "public" if v == types.VISIBILITY.PUBLIC else "/* TODO: not set */public"
    
    def var_type(self, v, attr):
        
        if attr != None and attr.attributes != None and "java-type" in attr.attributes:
            type = attr.attributes.get("java-type")
        elif isinstance(v.type, astTypes.ClassType) and v.type.decl.name == "TsArray":
            if v.mutability.mutated:
                self.imports["java.util.List"] = None
                type = f'''List<{self.type(v.type.type_arguments[0])}>'''
            else:
                type = f'''{self.type(v.type.type_arguments[0])}[]'''
        else:
            type = self.type(v.type)
        return type
    
    def var_wo_init(self, v, attr):
        return f'''{self.var_type(v, attr)} {self.name_(v.name)}'''
    
    def var(self, v, attrs):
        return self.var_wo_init(v, attrs) + (f''' = {self.expr(v.initializer)}''' if v.initializer != None else "")
    
    def expr_call(self, type_args, args):
        return self.type_args2(type_args) + f'''({", ".join(list(map(lambda x: self.expr(x), args)))})'''
    
    def mutate_arg(self, arg, should_be_mutable):
        if self.is_ts_array(arg.actual_type):
            item_type = (arg.actual_type).type_arguments[0]
            if isinstance(arg, exprs.ArrayLiteral) and not should_be_mutable:
                return f'''new {self.type(item_type)}[0]''' if len(arg.items) == 0 and not self.is_ts_array(item_type) else f'''new {self.type(item_type)}[] {{ {", ".join(list(map(lambda x: self.expr(x), arg.items)))} }}'''
            
            currently_mutable = should_be_mutable
            if isinstance(arg, refs.VariableReference):
                currently_mutable = arg.get_variable().mutability.mutated
            elif isinstance(arg, exprs.InstanceMethodCallExpression) or isinstance(arg, exprs.StaticMethodCallExpression):
                currently_mutable = False
            
            if currently_mutable and not should_be_mutable:
                return f'''{self.expr(arg)}.toArray({self.type(item_type)}[]::new)'''
            elif not currently_mutable and should_be_mutable:
                self.imports["java.util.Arrays"] = None
                self.imports["java.util.ArrayList"] = None
                return f'''new ArrayList<>(Arrays.asList({self.expr(arg)}))'''
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
    
    def is_set_expr(self, var_ref):
        return isinstance(var_ref.parent_node, exprs.BinaryExpression) and var_ref.parent_node.left == var_ref and var_ref.parent_node.operator in ["=", "+=", "-="]
    
    def expr(self, expr):
        for plugin in self.plugins:
            result = plugin.expr(expr)
            if result != None:
                return result
        
        res = "UNKNOWN-EXPR"
        if isinstance(expr, exprs.NewExpression):
            res = f'''new {self.type(expr.cls_, True, True)}{self.call_params(expr.args, expr.cls_.decl.constructor_.parameters if expr.cls_.decl.constructor_ != None else [])}'''
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
            res = f'''{self.expr(expr.method)}.apply({", ".join(list(map(lambda x: self.expr(x), expr.args)))})'''
        elif isinstance(expr, exprs.BooleanLiteral):
            res = f'''{("true" if expr.bool_value else "false")}'''
        elif isinstance(expr, exprs.StringLiteral):
            res = f'''{JSON.stringify(expr.string_value)}'''
        elif isinstance(expr, exprs.NumericLiteral):
            res = f'''{expr.value_as_text}'''
        elif isinstance(expr, exprs.CharacterLiteral):
            res = f'''\'{expr.char_value}\''''
        elif isinstance(expr, exprs.ElementAccessExpression):
            res = f'''{self.expr(expr.object)}.get({self.expr(expr.element_expr)})'''
        elif isinstance(expr, exprs.TemplateString):
            parts = []
            for part in expr.parts:
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
                        else:
                            chr_code = ord(chr[0])
                            if 32 <= chr_code and chr_code <= 126:
                                lit += chr
                            else:
                                raise Error(f'''invalid char in template string (code={chr_code})''')
                        i = i + 1
                    parts.append(f'''"{lit}"''')
                else:
                    repr = self.expr(part.expression)
                    parts.append(f'''({repr})''' if isinstance(part.expression, exprs.ConditionalExpression) else repr)
            res = " + ".join(parts)
        elif isinstance(expr, exprs.BinaryExpression):
            modifies = expr.operator in ["=", "+=", "-="]
            if modifies and isinstance(expr.left, refs.InstanceFieldReference) and self.use_getter_setter(expr.left):
                res = f'''{self.expr(expr.left.object)}.set{self.uc_first(expr.left.field.name)}({self.mutated_expr(expr.right, expr.left if expr.operator == "=" else None)})'''
            elif expr.operator in ["==", "!="]:
                lit = self.current_class.parent_file.literal_types
                left_type = expr.left.get_type()
                right_type = expr.right.get_type()
                use_equals = astTypes.TypeHelper.equals(left_type, lit.string) and right_type != None and astTypes.TypeHelper.equals(right_type, lit.string)
                if use_equals:
                    self.imports["OneStd.Objects"] = None
                    res = f'''{("!" if expr.operator == "!=" else "")}Objects.equals({self.expr(expr.left)}, {self.expr(expr.right)})'''
                else:
                    res = f'''{self.expr(expr.left)} {expr.operator} {self.expr(expr.right)}'''
            else:
                res = f'''{self.expr(expr.left)} {expr.operator} {self.mutated_expr(expr.right, expr.left if expr.operator == "=" else None)}'''
        elif isinstance(expr, exprs.ArrayLiteral):
            if len(expr.items) == 0:
                res = f'''new {self.type(expr.actual_type, True, True)}()'''
            else:
                self.imports[f'''java.util.List'''] = None
                self.imports[f'''java.util.ArrayList'''] = None
                res = f'''new ArrayList<>(List.of({", ".join(list(map(lambda x: self.expr(x), expr.items)))}))'''
        elif isinstance(expr, exprs.CastExpression):
            res = f'''(({self.type(expr.new_type)}){self.expr(expr.expression)})'''
        elif isinstance(expr, exprs.ConditionalExpression):
            res = f'''{self.expr(expr.condition)} ? {self.expr(expr.when_true)} : {self.mutated_expr(expr.when_false, expr.when_true)}'''
        elif isinstance(expr, exprs.InstanceOfExpression):
            res = f'''{self.expr(expr.expr)} instanceof {self.type(expr.check_type)}'''
        elif isinstance(expr, exprs.ParenthesizedExpression):
            res = f'''({self.expr(expr.expression)})'''
        elif isinstance(expr, exprs.RegexLiteral):
            self.imports[f'''OneStd.RegExp'''] = None
            res = f'''new RegExp({JSON.stringify(expr.pattern)})'''
        elif isinstance(expr, types.Lambda):
            
            if len(expr.body.statements) == 1 and isinstance(expr.body.statements[0], stats.ReturnStatement):
                body = " " + self.expr((expr.body.statements[0]).expression)
            else:
                body = self.block(expr.body, False)
            
            params = list(map(lambda x: self.name_(x.name), expr.parameters))
            
            res = f'''{(params[0] if len(params) == 1 else f'({", ".join(params)})')} ->{body}'''
        elif isinstance(expr, exprs.UnaryExpression) and expr.unary_type == exprs.UNARY_TYPE.PREFIX:
            res = f'''{expr.operator}{self.expr(expr.operand)}'''
        elif isinstance(expr, exprs.UnaryExpression) and expr.unary_type == exprs.UNARY_TYPE.POSTFIX:
            res = f'''{self.expr(expr.operand)}{expr.operator}'''
        elif isinstance(expr, exprs.MapLiteral):
            if len(expr.items) > 10:
                raise Error("MapLiteral is only supported with maximum of 10 items")
            if len(expr.items) == 0:
                res = f'''new {self.type(expr.actual_type, True, True)}()'''
            else:
                self.imports[f'''java.util.Map'''] = None
                repr = ", ".join(list(map(lambda item: f'''{JSON.stringify(item.key)}, {self.expr(item.value)}''', expr.items)))
                res = f'''Map.of({repr})'''
        elif isinstance(expr, exprs.NullLiteral):
            res = f'''null'''
        elif isinstance(expr, exprs.AwaitExpression):
            res = f'''{self.expr(expr.expr)}'''
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
            res = f'''super'''
        elif isinstance(expr, refs.StaticFieldReference):
            res = f'''{self.name_(expr.decl.parent_interface.name)}.{self.name_(expr.decl.name)}'''
        elif isinstance(expr, refs.StaticPropertyReference):
            res = f'''{self.name_(expr.decl.parent_class.name)}.{self.name_(expr.decl.name)}'''
        elif isinstance(expr, refs.InstanceFieldReference):
            # TODO: unified handling of field -> property conversion?
            if self.use_getter_setter(expr):
                res = f'''{self.expr(expr.object)}.get{self.uc_first(expr.field.name)}()'''
            else:
                res = f'''{self.expr(expr.object)}.{self.name_(expr.field.name)}'''
        elif isinstance(expr, refs.InstancePropertyReference):
            res = f'''{self.expr(expr.object)}.{("set" if self.is_set_expr(expr) else "get")}{self.uc_first(expr.property.name)}()'''
        elif isinstance(expr, refs.EnumMemberReference):
            res = f'''{self.name_(expr.decl.parent_enum.name)}.{self.name_(expr.decl.name)}'''
        elif isinstance(expr, exprs.NullCoalesceExpression):
            res = f'''{self.expr(expr.default_expr)} != null ? {self.expr(expr.default_expr)} : {self.mutated_expr(expr.expr_if_null, expr.default_expr)}'''
        else:
            pass
        return res
    
    def use_getter_setter(self, field_ref):
        return isinstance(field_ref.object.actual_type, astTypes.InterfaceType) or (field_ref.field.interface_declarations != None and len(field_ref.field.interface_declarations) > 0)
    
    def block(self, block, allow_one_liner = True):
        stmt_len = len(block.statements)
        return " { }" if stmt_len == 0 else f'''\n{self.pad(self.raw_block(block))}''' if allow_one_liner and stmt_len == 1 and not (isinstance(block.statements[0], stats.IfStatement)) and not (isinstance(block.statements[0], stats.VariableDeclaration)) else f''' {{\n{self.pad(self.raw_block(block))}\n}}'''
    
    def stmt_default(self, stmt):
        res = "UNKNOWN-STATEMENT"
        if isinstance(stmt, stats.BreakStatement):
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
            res = f'''for (var {self.name_(stmt.item_var.name)} : {self.expr(stmt.items)})''' + self.block(stmt.body)
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
                #this.imports.add("System");
                res += f''' catch (Exception {self.name_(stmt.catch_var.name)}) {self.block(stmt.catch_body, False)}'''
            if stmt.finally_body != None:
                res += "finally" + self.block(stmt.finally_body)
        elif isinstance(stmt, stats.ContinueStatement):
            res = f'''continue;'''
        else:
            pass
        return res
    
    def stmt(self, stmt):
        res = None
        
        if stmt.attributes != None and "java-import" in stmt.attributes:
            self.imports[stmt.attributes.get("java-import")] = None
        
        if stmt.attributes != None and "java" in stmt.attributes:
            res = stmt.attributes.get("java")
        else:
            for plugin in self.plugins:
                res = plugin.stmt(stmt)
                if res != None:
                    break
            
            if res == None:
                res = self.stmt_default(stmt)
        
        return self.leading(stmt) + res
    
    def stmts(self, stmts):
        return "\n".join(list(map(lambda stmt: self.stmt(stmt), stmts)))
    
    def raw_block(self, block):
        return self.stmts(block.statements)
    
    def method_gen(self, prefix, params, body):
        return f'''{prefix}({", ".join(list(map(lambda p: self.var_wo_init(p, p), params)))}){body}'''
    
    def method(self, method, is_cls):
        # TODO: final
        prefix = (self.vis(method.visibility) + " " if is_cls else "") + self.pre_if("static ", method.is_static) + self.pre_if("/* throws */ ", method.throws) + (f'''<{", ".join(method.type_arguments)}> ''' if len(method.type_arguments) > 0 else "") + f'''{self.type(method.returns, False)} ''' + self.name_(method.name)
        
        return self.method_gen(prefix, method.parameters, ";" if method.body == None else f''' {{\n{self.pad(self.stmts(method.body.statements))}\n}}''')
    
    def class(self, cls_):
        self.current_class = cls_
        res_list = []
        
        static_constructor_stmts = []
        complex_field_inits = []
        field_reprs = []
        prop_reprs = []
        for field in cls_.fields:
            is_initializer_complex = field.initializer != None and not (isinstance(field.initializer, exprs.StringLiteral)) and not (isinstance(field.initializer, exprs.BooleanLiteral)) and not (isinstance(field.initializer, exprs.NumericLiteral))
            
            prefix = f'''{self.vis(field.visibility)} {self.pre_if("static ", field.is_static)}'''
            if len(field.interface_declarations) > 0:
                var_type = self.var_type(field, field)
                name = self.name_(field.name)
                pname = self.uc_first(field.name)
                set_to_false = astTypes.TypeHelper.equals(field.type, self.current_class.parent_file.literal_types.boolean)
                prop_reprs.append(f'''{var_type} {name}{(" = false" if set_to_false else f' = {self.expr(field.initializer)}' if field.initializer != None else "")};\n''' + f'''{prefix}{var_type} get{pname}() {{ return this.{name}; }}\n''' + f'''{prefix}void set{pname}({var_type} value) {{ this.{name} = value; }}''')
            elif is_initializer_complex:
                if field.is_static:
                    static_constructor_stmts.append(stats.ExpressionStatement(exprs.BinaryExpression(refs.StaticFieldReference(field), "=", field.initializer)))
                else:
                    complex_field_inits.append(stats.ExpressionStatement(exprs.BinaryExpression(refs.InstanceFieldReference(refs.ThisReference(cls_), field), "=", field.initializer)))
                
                field_reprs.append(f'''{prefix}{self.var_wo_init(field, field)};''')
            else:
                field_reprs.append(f'''{prefix}{self.var(field, field)};''')
        res_list.append("\n".join(field_reprs))
        res_list.append("\n\n".join(prop_reprs))
        
        for prop in cls_.properties:
            prefix = f'''{self.vis(prop.visibility)} {self.pre_if("static ", prop.is_static)}'''
            if prop.getter != None:
                res_list.append(f'''{prefix}{self.type(prop.type)} get{self.uc_first(prop.name)}(){self.block(prop.getter, False)}''')
            
            if prop.setter != None:
                res_list.append(f'''{prefix}void set{self.uc_first(prop.name)}({self.type(prop.type)} value){self.block(prop.setter, False)}''')
        
        if len(static_constructor_stmts) > 0:
            res_list.append(f'''static {{\n{self.pad(self.stmts(static_constructor_stmts))}\n}}''')
        
        if cls_.constructor_ != None:
            constr_field_inits = []
            for field in list(filter(lambda x: x.constructor_param != None, cls_.fields)):
                field_ref = refs.InstanceFieldReference(refs.ThisReference(cls_), field)
                mp_ref = refs.MethodParameterReference(field.constructor_param)
                # TODO: decide what to do with "after-TypeEngine" transformations
                mp_ref.set_actual_type(field.type, False, False)
                constr_field_inits.append(stats.ExpressionStatement(exprs.BinaryExpression(field_ref, "=", mp_ref)))
            
            super_call = f'''super({", ".join(list(map(lambda x: self.expr(x), cls_.constructor_.super_call_args)))});\n''' if cls_.constructor_.super_call_args != None else ""
            
            # TODO: super calls
            res_list.append(self.method_gen("public " + self.pre_if("/* throws */ ", cls_.constructor_.throws) + self.name_(cls_.name), cls_.constructor_.parameters, f'''\n{{\n{self.pad(super_call + self.stmts(constr_field_inits + complex_field_inits + cls_.constructor_.body.statements))}\n}}'''))
        elif len(complex_field_inits) > 0:
            res_list.append(f'''public {self.name_(cls_.name)}()\n{{\n{self.pad(self.stmts(complex_field_inits))}\n}}''')
        
        methods = []
        for method in cls_.methods:
            if method.body == None:
                continue
            # declaration only
            methods.append(self.method(method, True))
        res_list.append("\n\n".join(methods))
        return self.pad("\n\n".join(list(filter(lambda x: x != "", res_list))))
    
    def uc_first(self, str):
        return str[0].upper() + str[1:]
    
    def interface(self, intf):
        self.current_class = intf
        
        res_list = []
        for field in intf.fields:
            var_type = self.var_type(field, field)
            name = self.uc_first(field.name)
            res_list.append(f'''{var_type} get{name}();\nvoid set{name}({var_type} value);''')
        
        res_list.append("\n".join(list(map(lambda method: self.method(method, False), intf.methods))))
        return self.pad("\n\n".join(list(filter(lambda x: x != "", res_list))))
    
    def pad(self, str):
        return "\n".join(list(map(lambda x: f'''    {x}''', re.split("\\n", str))))
    
    def path_to_ns(self, path):
        # Generator/ExprLang/ExprLangAst.ts -> Generator.ExprLang
        parts = re.split("/", path)
        parts.pop()
        return ".".join(parts)
    
    def imports_head(self):
        imports = []
        for imp in self.imports.keys():
            imports.append(imp)
        self.imports = dict()
        return "" if len(imports) == 0 else "\n".join(list(map(lambda x: f'''import {x};''', imports))) + "\n\n"
    
    def to_import(self, scope):
        return f'''OneStd''' if scope.scope_name == "index" else f'''{scope.package_name}.{re.sub("/", ".", re.sub("\\.ts$", "", scope.scope_name))}'''
    
    def generate(self, pkg):
        result = []
        for path in pkg.files.keys():
            file = pkg.files.get(path)
            package_path = f'''{pkg.name}/{re.sub("\\.ts$", "", file.source_path.path)}'''
            dst_dir = f'''src/main/java/{package_path}'''
            package_name = re.sub("/", ".", package_path)
            
            imports = dict()
            for imp_list in file.imports:
                imp_pkg = self.to_import(imp_list.export_scope)
                for imp in imp_list.imports:
                    imports[f'''{imp_pkg}.{imp.name}'''] = None
            
            head = f'''package {package_name};\n\n{"\n".join(list(map(lambda x: f'import {x};', Array.from_(imports.keys()))))}\n\n'''
            
            for enum_ in file.enums:
                result.append(genFile.GeneratedFile(f'''{dst_dir}/{enum_.name}.java''', f'''{head}public enum {self.name_(enum_.name)} {{ {", ".join(list(map(lambda x: self.name_(x.name), enum_.values)))} }}'''))
            
            for intf in file.interfaces:
                res = f'''public interface {self.name_(intf.name)}{self.type_args(intf.type_arguments)}''' + f'''{self.pre_arr(" extends ", list(map(lambda x: self.type(x), intf.base_interfaces)))} {{\n{self.interface(intf)}\n}}'''
                result.append(genFile.GeneratedFile(f'''{dst_dir}/{intf.name}.java''', f'''{head}{self.imports_head()}{res}'''))
            
            for cls_ in file.classes:
                res = f'''public class {self.name_(cls_.name)}{self.type_args(cls_.type_arguments)}''' + (f''' extends {self.type(cls_.base_class)}''' if cls_.base_class != None else "") + self.pre_arr(" implements ", list(map(lambda x: self.type(x), cls_.base_interfaces))) + f''' {{\n{self.class(cls_)}\n}}'''
                result.append(genFile.GeneratedFile(f'''{dst_dir}/{cls_.name}.java''', f'''{head}{self.imports_head()}{res}'''))
        return result