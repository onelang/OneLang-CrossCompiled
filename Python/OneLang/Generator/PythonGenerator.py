from OneLangStdLib import *
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.Statements as stats
import OneLang.One.Ast.Types as types
import OneLang.One.Ast.AstTypes as astTypes
import OneLang.One.Ast.References as refs
import OneLang.Generator.GeneratedFile as genFile
import OneLang.Utils.TSOverviewGenerator as tSOvervGen
import OneLang.Generator.IGeneratorPlugin as iGenPlug
import OneLang.Generator.PythonPlugins.JsToPython as jsToPyth
import OneLang.Generator.NameUtils as nameUtils
import OneLang.One.Ast.Interfaces as ints
import OneLang.Generator.IGenerator as iGen
import OneLang.One.ITransformer as iTrans
import re

class PythonGenerator:
    def __init__(self):
        self.tmpl_str_level = 0
        self.package = None
        self.current_file = None
        self.imports = None
        self.import_all_scopes = None
        self.current_class = None
        self.reserved_words = ["from", "async", "global", "lambda", "cls", "import", "pass"]
        self.field_to_method_hack = []
        self.plugins = []
        self.plugins.append(jsToPyth.JsToPython(self))
    
    def get_lang_name(self):
        return "Python"
    
    def get_extension(self):
        return "py"
    
    def get_transforms(self):
        return []
    
    def type(self, type):
        if isinstance(type, astTypes.ClassType):
            if type.decl.name == "TsString":
                return "str"
            elif type.decl.name == "TsBoolean":
                return "bool"
            elif type.decl.name == "TsNumber":
                return "int"
            else:
                return self.cls_name(type.decl)
        else:
            return "NOT-HANDLED-TYPE"
    
    def split_name(self, name):
        name_parts = []
        part_start_idx = 0
        i = 1
        
        while i < len(name):
            prev_chr_code = ord(name[i - 1])
            chr_code = ord(name[i])
            if 65 <= chr_code and chr_code <= 90 and not (65 <= prev_chr_code and prev_chr_code <= 90):
                # 'A' .. 'Z'
                name_parts.append(name[part_start_idx:i].lower())
                part_start_idx = i
            elif chr_code == 95:
                # '-'
                name_parts.append(name[part_start_idx:i])
                part_start_idx = i + 1
            i = i + 1
        name_parts.append(name[part_start_idx:].lower())
        return name_parts
    
    def name_(self, name):
        if name in self.reserved_words:
            name += "_"
        if name in self.field_to_method_hack:
            name += "()"
        return "_".join(self.split_name(name))
    
    def calc_imported_name(self, export_scope, name):
        if export_scope.get_id() in self.import_all_scopes:
            return name
        else:
            return self.calc_import_alias(export_scope) + "." + name
    
    def enum_name(self, enum_, is_decl = False):
        name = self.name_(enum_.name).upper()
        if is_decl or enum_.parent_file.export_scope == None or enum_.parent_file == self.current_file:
            return name
        return self.calc_imported_name(enum_.parent_file.export_scope, name)
    
    def enum_member_name(self, name):
        return self.name_(name).upper()
    
    def cls_name(self, cls_, is_decl = False):
        # TODO: hack
        if cls_.name == "Set":
            return "dict"
        if is_decl or cls_.parent_file.export_scope == None or cls_.parent_file == self.current_file:
            return cls_.name
        return self.calc_imported_name(cls_.parent_file.export_scope, cls_.name)
    
    def leading(self, item):
        result = ""
        if item.leading_trivia != None and len(item.leading_trivia) > 0:
            result += re.sub("//", "#", item.leading_trivia)
        #if (item.attributes !== null)
        #    result += Object.keys(item.attributes).map(x => `# @${x} ${item.attributes[x]}\n`).join("");
        return result
    
    def pre_arr(self, prefix, value):
        return f'''{prefix}{", ".join(value)}''' if len(value) > 0 else ""
    
    def pre_if(self, prefix, condition):
        return prefix if condition else ""
    
    def pre(self, prefix, value):
        return f'''{prefix}{value}''' if value != None else ""
    
    def is_ts_array(self, type):
        return isinstance(type, astTypes.ClassType) and type.decl.name == "TsArray"
    
    def vis(self, v):
        return "__" if v == types.VISIBILITY.PRIVATE else "_" if v == types.VISIBILITY.PROTECTED else "" if v == types.VISIBILITY.PUBLIC else "/* TODO: not set */public"
    
    def var_wo_init(self, v, attr):
        return self.name_(v.name)
    
    def var(self, v, attrs):
        return f'''{self.var_wo_init(v, attrs)}{(f' = {self.expr(v.initializer)}' if v.initializer != None else "")}'''
    
    def expr_call(self, args):
        return f'''({", ".join(list(map(lambda x: self.expr(x), args)))})'''
    
    def call_params(self, args):
        arg_reprs = []
        i = 0
        
        while i < len(args):
            arg_reprs.append(self.expr(args[i]))
            i = i + 1
        return f'''({", ".join(arg_reprs)})'''
    
    def method_call(self, expr):
        return self.name_(expr.method.name) + self.call_params(expr.args)
    
    def expr(self, expr):
        for plugin in self.plugins:
            result = plugin.expr(expr)
            if result != None:
                return result
        
        res = "UNKNOWN-EXPR"
        if isinstance(expr, exprs.NewExpression):
            # TODO: hack
            if expr.cls_.decl.name == "Set":
                res = "dict()" if len(expr.args) == 0 else f'''dict.fromkeys{self.call_params(expr.args)}'''
            else:
                res = f'''{self.cls_name(expr.cls_.decl)}{self.call_params(expr.args)}'''
        elif isinstance(expr, exprs.UnresolvedNewExpression):
            res = f'''/* TODO: UnresolvedNewExpression */ {expr.cls_.type_name}({", ".join(list(map(lambda x: self.expr(x), expr.args)))})'''
        elif isinstance(expr, exprs.Identifier):
            res = f'''/* TODO: Identifier */ {expr.text}'''
        elif isinstance(expr, exprs.PropertyAccessExpression):
            res = f'''/* TODO: PropertyAccessExpression */ {self.expr(expr.object)}.{expr.property_name}'''
        elif isinstance(expr, exprs.UnresolvedCallExpression):
            res = f'''/* TODO: UnresolvedCallExpression */ {self.expr(expr.func)}{self.expr_call(expr.args)}'''
        elif isinstance(expr, exprs.UnresolvedMethodCallExpression):
            res = f'''/* TODO: UnresolvedMethodCallExpression */ {self.expr(expr.object)}.{expr.method_name}{self.expr_call(expr.args)}'''
        elif isinstance(expr, exprs.InstanceMethodCallExpression):
            res = f'''{self.expr(expr.object)}.{self.method_call(expr)}'''
        elif isinstance(expr, exprs.StaticMethodCallExpression):
            #const parent = expr.method.parentInterface === this.currentClass ? "cls" : this.clsName(expr.method.parentInterface);
            parent = self.cls_name(expr.method.parent_interface)
            res = f'''{parent}.{self.method_call(expr)}'''
        elif isinstance(expr, exprs.GlobalFunctionCallExpression):
            self.imports["from OneLangStdLib import *"] = None
            res = f'''{self.name_(expr.func.name)}{self.expr_call(expr.args)}'''
        elif isinstance(expr, exprs.LambdaCallExpression):
            res = f'''{self.expr(expr.method)}({", ".join(list(map(lambda x: self.expr(x), expr.args)))})'''
        elif isinstance(expr, exprs.BooleanLiteral):
            res = f'''{("True" if expr.bool_value else "False")}'''
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
                        elif chr == "'":
                            lit += "\\'"
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
                    self.tmpl_str_level = self.tmpl_str_level + 1
                    repr = self.expr(part.expression)
                    self.tmpl_str_level = self.tmpl_str_level - 1
                    parts.append(f'''{{({repr})}}''' if isinstance(part.expression, exprs.ConditionalExpression) else f'''{{{repr}}}''')
            res = f'''f\'{"".join(parts)}\'''' if self.tmpl_str_level == 1 else f'''f\'\'\'{"".join(parts)}\'\'\''''
        elif isinstance(expr, exprs.BinaryExpression):
            op = "and" if expr.operator == "&&" else "or" if expr.operator == "||" else expr.operator
            res = f'''{self.expr(expr.left)} {op} {self.expr(expr.right)}'''
        elif isinstance(expr, exprs.ArrayLiteral):
            res = f'''[{", ".join(list(map(lambda x: self.expr(x), expr.items)))}]'''
        elif isinstance(expr, exprs.CastExpression):
            res = f'''{self.expr(expr.expression)}'''
        elif isinstance(expr, exprs.ConditionalExpression):
            res = f'''{self.expr(expr.when_true)} if {self.expr(expr.condition)} else {self.expr(expr.when_false)}'''
        elif isinstance(expr, exprs.InstanceOfExpression):
            res = f'''isinstance({self.expr(expr.expr)}, {self.type(expr.check_type)})'''
        elif isinstance(expr, exprs.ParenthesizedExpression):
            res = f'''({self.expr(expr.expression)})'''
        elif isinstance(expr, exprs.RegexLiteral):
            res = f'''RegExp({JSON.stringify(expr.pattern)})'''
        elif isinstance(expr, types.Lambda):
            body = "INVALID-BODY"
            if len(expr.body.statements) == 1 and isinstance(expr.body.statements[0], stats.ReturnStatement):
                body = self.expr((expr.body.statements[0]).expression)
            else:
                console.error(f'''Multi-line lambda is not yet supported for Python: {tSOvervGen.TSOverviewGenerator.preview.node_repr(expr)}''')
            
            params = list(map(lambda x: self.name_(x.name), expr.parameters))
            
            res = f'''lambda {", ".join(params)}: {body}'''
        elif isinstance(expr, exprs.UnaryExpression) and expr.unary_type == exprs.UNARY_TYPE.PREFIX:
            op = "not " if expr.operator == "!" else expr.operator
            if op == "++":
                res = f'''{self.expr(expr.operand)} = {self.expr(expr.operand)} + 1'''
            elif op == "--":
                res = f'''{self.expr(expr.operand)} = {self.expr(expr.operand)} - 1'''
            else:
                res = f'''{op}{self.expr(expr.operand)}'''
        elif isinstance(expr, exprs.UnaryExpression) and expr.unary_type == exprs.UNARY_TYPE.POSTFIX:
            if expr.operator == "++":
                res = f'''{self.expr(expr.operand)} = {self.expr(expr.operand)} + 1'''
            elif expr.operator == "--":
                res = f'''{self.expr(expr.operand)} = {self.expr(expr.operand)} - 1'''
            else:
                res = f'''{self.expr(expr.operand)}{expr.operator}'''
        elif isinstance(expr, exprs.MapLiteral):
            repr = ",\n".join(list(map(lambda item: f'''{JSON.stringify(item.key)}: {self.expr(item.value)}''', expr.items)))
            res = "{}" if len(expr.items) == 0 else f'''{{\n{self.pad(repr)}\n}}'''
        elif isinstance(expr, exprs.NullLiteral):
            res = f'''None'''
        elif isinstance(expr, exprs.AwaitExpression):
            res = f'''{self.expr(expr.expr)}'''
        elif isinstance(expr, refs.ThisReference):
            res = f'''self'''
        elif isinstance(expr, refs.StaticThisReference):
            res = f'''cls'''
        elif isinstance(expr, refs.EnumReference):
            res = f'''{self.enum_name(expr.decl)}'''
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
            res = f'''super()'''
        elif isinstance(expr, refs.StaticFieldReference):
            res = f'''{self.cls_name(expr.decl.parent_interface)}.{self.name_(expr.decl.name)}'''
        elif isinstance(expr, refs.StaticPropertyReference):
            res = f'''{self.cls_name(expr.decl.parent_class)}.get_{self.name_(expr.decl.name)}()'''
        elif isinstance(expr, refs.InstanceFieldReference):
            res = f'''{self.expr(expr.object)}.{self.name_(expr.field.name)}'''
        elif isinstance(expr, refs.InstancePropertyReference):
            res = f'''{self.expr(expr.object)}.get_{self.name_(expr.property.name)}()'''
        elif isinstance(expr, refs.EnumMemberReference):
            res = f'''{self.enum_name(expr.decl.parent_enum)}.{self.enum_member_name(expr.decl.name)}'''
        elif isinstance(expr, exprs.NullCoalesceExpression):
            res = f'''{self.expr(expr.default_expr)} or {self.expr(expr.expr_if_null)}'''
        else:
            pass
        return res
    
    def stmt_default(self, stmt):
        nl = "\n"
        if isinstance(stmt, stats.BreakStatement):
            return "break"
        elif isinstance(stmt, stats.ReturnStatement):
            return "return" if stmt.expression == None else f'''return {self.expr(stmt.expression)}'''
        elif isinstance(stmt, stats.UnsetStatement):
            return f'''/* unset {self.expr(stmt.expression)}; */'''
        elif isinstance(stmt, stats.ThrowStatement):
            return f'''raise {self.expr(stmt.expression)}'''
        elif isinstance(stmt, stats.ExpressionStatement):
            return f'''{self.expr(stmt.expression)}'''
        elif isinstance(stmt, stats.VariableDeclaration):
            return f'''{self.name_(stmt.name)} = {self.expr(stmt.initializer)}''' if stmt.initializer != None else ""
        elif isinstance(stmt, stats.ForeachStatement):
            return f'''for {self.name_(stmt.item_var.name)} in {self.expr(stmt.items)}:\n{self.block(stmt.body)}'''
        elif isinstance(stmt, stats.IfStatement):
            else_if = stmt.else_ != None and len(stmt.else_.statements) == 1 and isinstance(stmt.else_.statements[0], stats.IfStatement)
            return f'''if {self.expr(stmt.condition)}:\n{self.block(stmt.then)}''' + (f'''\nel{self.stmt(stmt.else_.statements[0])}''' if else_if else "") + (f'''\nelse:\n{self.block(stmt.else_)}''' if not else_if and stmt.else_ != None else "")
        elif isinstance(stmt, stats.WhileStatement):
            return f'''while {self.expr(stmt.condition)}:\n{self.block(stmt.body)}'''
        elif isinstance(stmt, stats.ForStatement):
            return (f'''{self.var(stmt.item_var, None)}\n''' if stmt.item_var != None else "") + f'''\nwhile {self.expr(stmt.condition)}:\n{self.block(stmt.body)}\n{self.pad(self.expr(stmt.incrementor))}'''
        elif isinstance(stmt, stats.DoStatement):
            return f'''while True:\n{self.block(stmt.body)}\n{self.pad(f'if not ({self.expr(stmt.condition)}):{nl}{self.pad("break")}')}'''
        elif isinstance(stmt, stats.TryStatement):
            return f'''try:\n{self.block(stmt.try_body)}''' + (f'''\nexcept Exception as {self.name_(stmt.catch_var.name)}:\n{self.block(stmt.catch_body)}''' if stmt.catch_body != None else "") + (f'''\nfinally:\n{self.block(stmt.finally_body)}''' if stmt.finally_body != None else "")
        elif isinstance(stmt, stats.ContinueStatement):
            return f'''continue'''
        else:
            return "UNKNOWN-STATEMENT"
    
    def stmt(self, stmt):
        res = None
        
        if stmt.attributes != None and "python" in stmt.attributes:
            res = stmt.attributes.get("python")
        else:
            for plugin in self.plugins:
                res = plugin.stmt(stmt)
                if res != None:
                    break
            
            if res == None:
                res = self.stmt_default(stmt)
        
        return self.leading(stmt) + res
    
    def stmts(self, stmts, skip_pass = False):
        return self.pad("pass" if len(stmts) == 0 and not skip_pass else "\n".join(list(map(lambda stmt: self.stmt(stmt), stmts))))
    
    def block(self, block, skip_pass = False):
        return self.stmts(block.statements, skip_pass)
    
    def pass_(self, str):
        return "pass" if str == "" else str
    
    def cls_(self, cls_):
        if cls_.attributes.get("external") == "true":
            return ""
        self.current_class = cls_
        res_list = []
        class_attributes = []
        
        static_fields = list(filter(lambda x: x.is_static, cls_.fields))
        
        if len(static_fields) > 0:
            self.imports["import OneLangStdLib as one"] = None
            class_attributes.append("@one.static_init")
            field_inits = list(map(lambda f: f'''cls.{self.vis(f.visibility)}{cls_.name.replace(self.var(f, f), "cls")}''', static_fields))
            res_list.append(f'''@classmethod\ndef static_init(cls):\n''' + self.pad("\n".join(field_inits)))
        
        constr_stmts = []
        
        for field in list(filter(lambda x: not x.is_static, cls_.fields)):
            init = self.name_(field.constructor_param.name) if field.constructor_param != None else self.expr(field.initializer) if field.initializer != None else "None"
            constr_stmts.append(f'''self.{self.name_(field.name)} = {init}''')
        
        if cls_.base_class != None:
            if cls_.constructor_ != None and cls_.constructor_.super_call_args != None:
                constr_stmts.append(f'''super().__init__({", ".join(list(map(lambda x: self.expr(x), cls_.constructor_.super_call_args)))})''')
            else:
                constr_stmts.append(f'''super().__init__()''')
        
        if cls_.constructor_ != None:
            for stmt in cls_.constructor_.body.statements:
                constr_stmts.append(self.stmt(stmt))
        
        res_list.append(f'''def __init__(self{("" if cls_.constructor_ == None else "".join(list(map(lambda p: f', {self.var(p, None)}', cls_.constructor_.parameters))))}):\n''' + self.pad(self.pass_("\n".join(constr_stmts))))
        
        for prop in cls_.properties:
            if prop.getter != None:
                res_list.append(f'''def get_{self.name_(prop.name)}(self):\n{self.block(prop.getter)}''')
        
        methods = []
        for method in cls_.methods:
            if method.body == None:
                continue
            # declaration only
            methods.append(("@classmethod\n" if method.is_static else "") + f'''def {self.name_(method.name)}''' + f'''({("cls" if method.is_static else "self")}{"".join(list(map(lambda p: f', {self.var(p, None)}', method.parameters)))}):''' + "\n" + self.block(method.body))
        res_list.append("\n\n".join(methods))
        res_list2 = list(filter(lambda x: x != "", res_list))
        
        cls_hdr = f'''class {self.cls_name(cls_, True)}{(f'({self.cls_name((cls_.base_class).decl)})' if cls_.base_class != None else "")}:\n'''
        return "".join(list(map(lambda x: f'''{x}\n''', class_attributes))) + cls_hdr + self.pad("\n\n".join(res_list2) if len(res_list2) > 0 else "pass")
    
    def pad(self, str):
        return "" if str == "" else "\n".join(list(map(lambda x: f'''    {x}''', re.split("\\n", str))))
    
    def calc_rel_import(self, target_path, from_path):
        target_parts = re.split("/", target_path.scope_name)
        from_parts = re.split("/", from_path.scope_name)
        
        same_level = 0
        while same_level < len(target_parts) and same_level < len(from_parts) and target_parts[same_level] == from_parts[same_level]:
            same_level = same_level + 1
        
        result = ""
        i = 1
        
        while i < len(from_parts) - same_level:
            result += "."
            i = i + 1
        
        i = same_level
        
        while i < len(target_parts):
            result += "." + target_parts[i]
            i = i + 1
        
        return result
    
    def calc_import_alias(self, target_path):
        parts = re.split("/", target_path.scope_name)
        filename = parts[len(parts) - 1]
        return nameUtils.NameUtils.short_name(filename)
    
    def gen_file(self, source_file):
        self.current_file = source_file
        self.imports = dict()
        self.import_all_scopes = dict()
        self.imports["from OneLangStdLib import *"] = None
        # TODO: do not add this globally, just for nativeResolver methods
               
        if len(source_file.enums) > 0:
            self.imports["from enum import Enum"] = None
        
        for import_ in list(filter(lambda x: not x.import_all, source_file.imports)):
            if import_.attributes.get("python-ignore") == "true":
                continue
            
            if "python-import-all" in import_.attributes:
                self.imports[f'''from {import_.attributes.get("python-import-all")} import *'''] = None
                self.import_all_scopes[import_.export_scope.get_id()] = None
            else:
                alias = self.calc_import_alias(import_.export_scope)
                self.imports[f'''import {self.package.name}.{re.sub("/", ".", import_.export_scope.scope_name)} as {alias}'''] = None
        
        enums = []
        for enum_ in source_file.enums:
            values = []
            i = 0
            
            while i < len(enum_.values):
                values.append(f'''{self.enum_member_name(enum_.values[i].name)} = {i + 1}''')
                i = i + 1
            enums.append(f'''class {self.enum_name(enum_, True)}(Enum):\n''' + self.pad("\n".join(values)))
        
        classes = []
        for cls_ in source_file.classes:
            classes.append(self.cls_(cls_))
        
        main = self.block(source_file.main_block) if len(source_file.main_block.statements) > 0 else ""
        
        imports = []
        for imp in self.imports:
            imports.append(imp)
        
        return "\n\n".join(list(filter(lambda x: x != "", ["\n".join(imports), "\n\n".join(enums), "\n\n".join(classes), main])))
    
    def generate(self, pkg):
        self.package = pkg
        result = []
        for path in pkg.files.keys():
            result.append(genFile.GeneratedFile(f'''{pkg.name}/{path}''', self.gen_file(pkg.files.get(path))))
        return result