from OneLangStdLib import *
import OneLang.Parsers.Common.Reader as read
import OneLang.Parsers.Common.ExpressionParser as exprPars
import OneLang.Parsers.Common.NodeManager as nodeMan
import OneLang.Parsers.Common.IParser as iPars
import OneLang.One.Ast.AstTypes as astTypes
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.Statements as stats
import OneLang.One.Ast.Types as types
import OneLang.One.Ast.Interfaces as ints
import re

class TypeAndInit:
    def __init__(self, type, init):
        self.type = type
        self.init = init

class MethodSignature:
    def __init__(self, params, fields, body, returns, super_call_args):
        self.params = params
        self.fields = fields
        self.body = body
        self.returns = returns
        self.super_call_args = super_call_args

class TypeScriptParser2:
    def __init__(self, source, path = None):
        self.context = []
        self.reader = None
        self.expression_parser = None
        self.node_manager = None
        self.export_scope = None
        self.missing_return_type_is_void = False
        self.path = path
        self.reader = read.Reader(source)
        self.reader.hooks = self
        self.node_manager = nodeMan.NodeManager(self.reader)
        self.expression_parser = self.create_expression_parser(self.reader, self.node_manager)
        self.export_scope = types.ExportScopeRef(self.path.pkg.name, re.sub(".ts$", "", self.path.path) if self.path.path != None else None) if self.path != None else None
    
    def create_expression_parser(self, reader, node_manager = None):
        expression_parser = exprPars.ExpressionParser(reader, self, node_manager)
        expression_parser.string_literal_type = astTypes.UnresolvedType("TsString", [])
        expression_parser.numeric_literal_type = astTypes.UnresolvedType("TsNumber", [])
        return expression_parser
    
    def error_callback(self, error):
        raise Error(f'''[TypeScriptParser] {error.message} at {error.cursor.line}:{error.cursor.column} (context: {"/".join(self.context)})\n{self.reader.line_preview(error.cursor)}''')
    
    def infix_prehook(self, left):
        if isinstance(left, exprs.PropertyAccessExpression) and self.reader.peek_regex("<[A-Za-z0-9_<>]*?>\\(") != None:
            type_args = self.parse_type_args()
            self.reader.expect_token("(")
            args = self.expression_parser.parse_call_arguments()
            return exprs.UnresolvedCallExpression(left, type_args, args)
        elif self.reader.read_token("instanceof"):
            type = self.parse_type()
            return exprs.InstanceOfExpression(left, type)
        elif isinstance(left, exprs.Identifier) and self.reader.read_token("=>"):
            block = self.parse_lambda_block()
            return types.Lambda([types.MethodParameter(left.text, None, None, None)], block)
        return None
    
    def parse_lambda_params(self):
        if not self.reader.read_token("("):
            return None
        
        params = []
        if not self.reader.read_token(")"):
            while True:
                param_name = self.reader.expect_identifier()
                type = self.parse_type() if self.reader.read_token(":") else None
                params.append(types.MethodParameter(param_name, type, None, None))
                if not (self.reader.read_token(",")):
                    break
            self.reader.expect_token(")")
        return params
    
    def parse_type(self):
        if self.reader.read_token("{"):
            self.reader.expect_token("[")
            self.reader.read_identifier()
            self.reader.expect_token(":")
            self.reader.expect_token("string")
            self.reader.expect_token("]")
            self.reader.expect_token(":")
            map_value_type = self.parse_type()
            self.reader.read_token(";")
            self.reader.expect_token("}")
            return astTypes.UnresolvedType("TsMap", [map_value_type])
        
        if self.reader.peek_token("("):
            params = self.parse_lambda_params()
            self.reader.expect_token("=>")
            return_type = self.parse_type()
            return astTypes.LambdaType(params, return_type)
        
        type_name = self.reader.expect_identifier()
        start_pos = self.reader.prev_token_offset
        
        
        if type_name == "string":
            type = astTypes.UnresolvedType("TsString", [])
        elif type_name == "boolean":
            type = astTypes.UnresolvedType("TsBoolean", [])
        elif type_name == "number":
            type = astTypes.UnresolvedType("TsNumber", [])
        elif type_name == "any":
            type = astTypes.AnyType.instance
        elif type_name == "void":
            type = astTypes.VoidType.instance
        else:
            type_arguments = self.parse_type_args()
            type = astTypes.UnresolvedType(type_name, type_arguments)
        
        self.node_manager.add_node(type, start_pos)
        
        while self.reader.read_token("[]"):
            type = astTypes.UnresolvedType("TsArray", [type])
            self.node_manager.add_node(type, start_pos)
        
        return type
    
    def parse_expression(self):
        return self.expression_parser.parse()
    
    def unary_prehook(self):
        if self.reader.read_token("null"):
            return exprs.NullLiteral()
        elif self.reader.read_token("true"):
            return exprs.BooleanLiteral(True)
        elif self.reader.read_token("false"):
            return exprs.BooleanLiteral(False)
        elif self.reader.read_token("`"):
            parts = []
            lit_part = ""
            while True:
                if self.reader.read_exactly("`"):
                    if lit_part != "":
                        parts.append(exprs.TemplateStringPart.literal(lit_part))
                        lit_part = ""
                    
                    break
                elif self.reader.read_exactly("${"):
                    if lit_part != "":
                        parts.append(exprs.TemplateStringPart.literal(lit_part))
                        lit_part = ""
                    
                    expr = self.parse_expression()
                    parts.append(exprs.TemplateStringPart.expression(expr))
                    self.reader.expect_token("}")
                elif self.reader.read_exactly("\\"):
                    chr = self.reader.read_char()
                    if chr == "n":
                        lit_part += "\n"
                    elif chr == "r":
                        lit_part += "\r"
                    elif chr == "t":
                        lit_part += "\t"
                    elif chr == "`":
                        lit_part += "`"
                    elif chr == "$":
                        lit_part += "$"
                    elif chr == "\\":
                        lit_part += "\\"
                    else:
                        self.reader.fail("invalid escape", self.reader.offset - 1)
                else:
                    chr = self.reader.read_char()
                    chr_code = ord(chr[0])
                    if not (32 <= chr_code and chr_code <= 126) or chr == "`" or chr == "\\":
                        self.reader.fail(f'''not allowed character (code={chr_code})''', self.reader.offset - 1)
                    lit_part += chr
            return exprs.TemplateString(parts)
        elif self.reader.read_token("new"):
            type = self.parse_type()
            if isinstance(type, astTypes.UnresolvedType):
                self.reader.expect_token("(")
                args = self.expression_parser.parse_call_arguments()
                return exprs.UnresolvedNewExpression(type, args)
            else:
                raise Error(f'''[TypeScriptParser2] Expected UnresolvedType here!''')
        elif self.reader.read_token("<"):
            new_type = self.parse_type()
            self.reader.expect_token(">")
            expression = self.parse_expression()
            return exprs.CastExpression(new_type, expression)
        elif self.reader.read_token("/"):
            pattern = ""
            while True:
                chr = self.reader.read_char()
                if chr == "\\":
                    chr2 = self.reader.read_char()
                    pattern += "/" if chr2 == "/" else "\\" + chr2
                elif chr == "/":
                    break
                else:
                    pattern += chr
            modifiers = self.reader.read_modifiers(["g", "i"])
            return exprs.RegexLiteral(pattern, "i" in modifiers, "g" in modifiers)
        elif self.reader.read_token("typeof"):
            expr = self.expression_parser.parse(self.expression_parser.prefix_precedence)
            self.reader.expect_token("===")
            check = self.reader.expect_string()
            
            ts_type = None
            if check == "string":
                ts_type = "TsString"
            elif check == "boolean":
                ts_type = "TsBoolean"
            elif check == "object":
                ts_type = "Object"
            elif check == "function":
                # TODO: ???
                ts_type = "Function"
            elif check == "undefined":
                # TODO: ???
                ts_type = "Object"
            else:
                self.reader.fail("unexpected typeof comparison")
            
            return exprs.InstanceOfExpression(expr, astTypes.UnresolvedType(ts_type, []))
        elif self.reader.peek_regex("\\([A-Za-z0-9_]+\\s*[:,]|\\(\\)") != None:
            params = self.parse_lambda_params()
            self.reader.expect_token("=>")
            block = self.parse_lambda_block()
            return types.Lambda(params, block)
        elif self.reader.read_token("await"):
            expression = self.parse_expression()
            return exprs.AwaitExpression(expression)
        
        map_literal = self.expression_parser.parse_map_literal()
        if map_literal != None:
            return map_literal
        
        array_literal = self.expression_parser.parse_array_literal()
        if array_literal != None:
            return array_literal
        
        return None
    
    def parse_lambda_block(self):
        block = self.parse_block()
        if block != None:
            return block
        
        return_expr = self.parse_expression()
        if isinstance(return_expr, exprs.ParenthesizedExpression):
            return_expr = return_expr.expression
        return stats.Block([stats.ReturnStatement(return_expr)])
    
    def parse_type_and_init(self):
        type = self.parse_type() if self.reader.read_token(":") else None
        init = self.parse_expression() if self.reader.read_token("=") else None
        
        if type == None and init == None:
            self.reader.fail(f'''expected type declaration or initializer''')
        
        return TypeAndInit(type, init)
    
    def expect_block_or_statement(self):
        block = self.parse_block()
        if block != None:
            return block
        
        stmts = []
        stmt = self.expect_statement()
        if stmt != None:
            stmts.append(stmt)
        return stats.Block(stmts)
    
    def expect_statement(self):
        statement = None
        
        leading_trivia = self.reader.read_leading_trivia()
        start_pos = self.reader.offset
        
        requires_closing = True
        var_decl_matches = self.reader.read_regex("(const|let|var)\\b")
        if var_decl_matches != None:
            name = self.reader.expect_identifier("expected variable name")
            type_and_init = self.parse_type_and_init()
            statement = stats.VariableDeclaration(name, type_and_init.type, type_and_init.init)
        elif self.reader.read_token("delete"):
            statement = stats.UnsetStatement(self.parse_expression())
        elif self.reader.read_token("if"):
            requires_closing = False
            self.reader.expect_token("(")
            condition = self.parse_expression()
            self.reader.expect_token(")")
            then = self.expect_block_or_statement()
            else_ = self.expect_block_or_statement() if self.reader.read_token("else") else None
            statement = stats.IfStatement(condition, then, else_)
        elif self.reader.read_token("while"):
            requires_closing = False
            self.reader.expect_token("(")
            condition = self.parse_expression()
            self.reader.expect_token(")")
            body = self.expect_block_or_statement()
            statement = stats.WhileStatement(condition, body)
        elif self.reader.read_token("do"):
            requires_closing = False
            body = self.expect_block_or_statement()
            self.reader.expect_token("while")
            self.reader.expect_token("(")
            condition = self.parse_expression()
            self.reader.expect_token(")")
            statement = stats.DoStatement(condition, body)
        elif self.reader.read_token("for"):
            requires_closing = False
            self.reader.expect_token("(")
            var_decl_mod = self.reader.read_any_of(["const", "let", "var"])
            item_var_name = None if var_decl_mod == None else self.reader.expect_identifier()
            if item_var_name != None and self.reader.read_token("of"):
                items = self.parse_expression()
                self.reader.expect_token(")")
                body = self.expect_block_or_statement()
                statement = stats.ForeachStatement(stats.ForeachVariable(item_var_name), items, body)
            else:
                for_var = None
                if item_var_name != None:
                    type_and_init = self.parse_type_and_init()
                    for_var = stats.ForVariable(item_var_name, type_and_init.type, type_and_init.init)
                self.reader.expect_token(";")
                condition = self.parse_expression()
                self.reader.expect_token(";")
                incrementor = self.parse_expression()
                self.reader.expect_token(")")
                body = self.expect_block_or_statement()
                statement = stats.ForStatement(for_var, condition, incrementor, body)
        elif self.reader.read_token("try"):
            block = self.expect_block("try body is missing")
            
            catch_var = None
            catch_body = None
            if self.reader.read_token("catch"):
                self.reader.expect_token("(")
                catch_var = stats.CatchVariable(self.reader.expect_identifier(), None)
                self.reader.expect_token(")")
                catch_body = self.expect_block("catch body is missing")
            
            finally_body = self.expect_block() if self.reader.read_token("finally") else None
            return stats.TryStatement(block, catch_var, catch_body, finally_body)
        elif self.reader.read_token("return"):
            expr = None if self.reader.peek_token(";") else self.parse_expression()
            statement = stats.ReturnStatement(expr)
        elif self.reader.read_token("throw"):
            expr = self.parse_expression()
            statement = stats.ThrowStatement(expr)
        elif self.reader.read_token("break"):
            statement = stats.BreakStatement()
        elif self.reader.read_token("continue"):
            statement = stats.ContinueStatement()
        elif self.reader.read_token("debugger;"):
            return None
        else:
            expr = self.parse_expression()
            statement = stats.ExpressionStatement(expr)
            is_binary_set = isinstance(expr, exprs.BinaryExpression) and expr.operator in ["=", "+=", "-="]
            is_unary_set = isinstance(expr, exprs.UnaryExpression) and expr.operator in ["++", "--"]
            if not (isinstance(expr, exprs.UnresolvedCallExpression) or is_binary_set or is_unary_set or isinstance(expr, exprs.AwaitExpression)):
                self.reader.fail("this expression is not allowed as statement")
        
        if statement == None:
            self.reader.fail("unknown statement")
        
        statement.leading_trivia = leading_trivia
        self.node_manager.add_node(statement, start_pos)
        
        statement_last_line = self.reader.ws_line_counter
        if not self.reader.read_token(";") and requires_closing and self.reader.ws_line_counter == statement_last_line:
            self.reader.fail("statement is not closed", self.reader.ws_offset)
        
        return statement
    
    def parse_block(self):
        if not self.reader.read_token("{"):
            return None
        start_pos = self.reader.prev_token_offset
        
        statements = []
        if not self.reader.read_token("}"):
            while True:
                statement = self.expect_statement()
                if statement != None:
                    statements.append(statement)
                if not (not self.reader.read_token("}")):
                    break
        
        block = stats.Block(statements)
        self.node_manager.add_node(block, start_pos)
        return block
    
    def expect_block(self, error_msg = None):
        block = self.parse_block()
        if block == None:
            self.reader.fail(error_msg or "expected block here")
        return block
    
    def parse_type_args(self):
        type_arguments = []
        if self.reader.read_token("<"):
            while True:
                generics = self.parse_type()
                type_arguments.append(generics)
                if not (self.reader.read_token(",")):
                    break
            self.reader.expect_token(">")
        return type_arguments
    
    def parse_generics_args(self):
        type_arguments = []
        if self.reader.read_token("<"):
            while True:
                generics = self.reader.expect_identifier()
                type_arguments.append(generics)
                if not (self.reader.read_token(",")):
                    break
            self.reader.expect_token(">")
        return type_arguments
    
    def parse_expr_stmt_from_string(self, expression):
        expr = self.create_expression_parser(read.Reader(expression)).parse()
        return stats.ExpressionStatement(expr)
    
    def parse_method_signature(self, is_constructor, declaration_only):
        params = []
        fields = []
        if not self.reader.read_token(")"):
            while True:
                leading_trivia = self.reader.read_leading_trivia()
                param_start = self.reader.offset
                is_public = self.reader.read_token("public")
                if is_public and not is_constructor:
                    self.reader.fail("public modifier is only allowed in constructor definition")
                
                param_name = self.reader.expect_identifier()
                self.context.append(f'''arg:{param_name}''')
                type_and_init = self.parse_type_and_init()
                param = types.MethodParameter(param_name, type_and_init.type, type_and_init.init, leading_trivia)
                params.append(param)
                
                # init should be used as only the constructor's method parameter, but not again as a field initializer too
                #   (otherwise it would called twice if cloned or cause AST error is just referenced from two separate places)
                if is_public:
                    field = types.Field(param_name, type_and_init.type, None, types.VISIBILITY.PUBLIC, False, param, param.leading_trivia)
                    fields.append(field)
                    param.field_decl = field
                
                self.node_manager.add_node(param, param_start)
                self.context.pop()
                if not (self.reader.read_token(",")):
                    break
            
            self.reader.expect_token(")")
        
        returns = None
        if not is_constructor:
            # in case of constructor, "returns" won't be used
            returns = self.parse_type() if self.reader.read_token(":") else astTypes.VoidType.instance if self.missing_return_type_is_void else None
        
        body = None
        super_call_args = None
        if declaration_only:
            self.reader.expect_token(";")
        else:
            body = self.expect_block("method body is missing")
            first_stmt = body.statements[0] if len(body.statements) > 0 else None
            if isinstance(first_stmt, stats.ExpressionStatement) and isinstance(first_stmt.expression, exprs.UnresolvedCallExpression) and isinstance(first_stmt.expression.func, exprs.Identifier) and first_stmt.expression.func.text == "super":
                super_call_args = first_stmt.expression.args
                body.statements.pop(0)
        
        return MethodSignature(params, fields, body, returns, super_call_args)
    
    def parse_identifier_or_string(self):
        return self.reader.read_string() or self.reader.expect_identifier()
    
    def parse_interface(self, leading_trivia, is_exported):
        if not self.reader.read_token("interface"):
            return None
        intf_start = self.reader.prev_token_offset
        
        intf_name = self.reader.expect_identifier("expected identifier after 'interface' keyword")
        self.context.append(f'''I:{intf_name}''')
        
        intf_type_args = self.parse_generics_args()
        
        base_interfaces = []
        if self.reader.read_token("extends"):
            while True:
                base_interfaces.append(self.parse_type())
                if not (self.reader.read_token(",")):
                    break
        
        methods = []
        fields = []
        
        self.reader.expect_token("{")
        while not self.reader.read_token("}"):
            member_leading_trivia = self.reader.read_leading_trivia()
            
            member_start = self.reader.offset
            member_name = self.parse_identifier_or_string()
            if self.reader.read_token(":"):
                self.context.append(f'''F:{member_name}''')
                
                field_type = self.parse_type()
                self.reader.expect_token(";")
                
                field = types.Field(member_name, field_type, None, types.VISIBILITY.PUBLIC, False, None, member_leading_trivia)
                fields.append(field)
                
                self.node_manager.add_node(field, member_start)
                self.context.pop()
            else:
                self.context.append(f'''M:{member_name}''')
                method_type_args = self.parse_generics_args()
                self.reader.expect_token("(")
                # method
                   
                sig = self.parse_method_signature(False, True)
                
                method = types.Method(member_name, method_type_args, sig.params, sig.body, types.VISIBILITY.PUBLIC, False, sig.returns, False, member_leading_trivia)
                methods.append(method)
                self.node_manager.add_node(method, member_start)
                self.context.pop()
        
        intf = types.Interface(intf_name, intf_type_args, base_interfaces, fields, methods, is_exported, leading_trivia)
        self.node_manager.add_node(intf, intf_start)
        self.context.pop()
        return intf
    
    def parse_specified_type(self):
        type_name = self.reader.read_identifier()
        type_args = self.parse_type_args()
        return astTypes.UnresolvedType(type_name, type_args)
    
    def parse_class(self, leading_trivia, is_exported, declaration_only):
        cls_modifiers = self.reader.read_modifiers(["abstract"])
        if not self.reader.read_token("class"):
            return None
        cls_start = self.reader.prev_token_offset
        
        cls_name = self.reader.expect_identifier("expected identifier after 'class' keyword")
        self.context.append(f'''C:{cls_name}''')
        
        type_args = self.parse_generics_args()
        base_class = self.parse_specified_type() if self.reader.read_token("extends") else None
        
        base_interfaces = []
        if self.reader.read_token("implements"):
            while True:
                base_interfaces.append(self.parse_specified_type())
                if not (self.reader.read_token(",")):
                    break
        
        constructor = None
        fields = []
        methods = []
        properties = []
        
        self.reader.expect_token("{")
        while not self.reader.read_token("}"):
            member_leading_trivia = self.reader.read_leading_trivia()
            
            member_start = self.reader.offset
            modifiers = self.reader.read_modifiers(["static", "public", "protected", "private", "readonly", "async", "abstract"])
            is_static = "static" in modifiers
            is_async = "async" in modifiers
            is_abstract = "abstract" in modifiers
            visibility = types.VISIBILITY.PRIVATE if "private" in modifiers else types.VISIBILITY.PROTECTED if "protected" in modifiers else types.VISIBILITY.PUBLIC
            
            member_name = self.parse_identifier_or_string()
            method_type_args = self.parse_generics_args()
            if self.reader.read_token("("):
                # method
                is_constructor = member_name == "constructor"
                
                
                sig = self.parse_method_signature(is_constructor, declaration_only or is_abstract)
                if is_constructor:
                    member = constructor = types.Constructor(sig.params, sig.body, sig.super_call_args, member_leading_trivia)
                    for field in sig.fields:
                        fields.append(field)
                else:
                    method = types.Method(member_name, method_type_args, sig.params, sig.body, visibility, is_static, sig.returns, is_async, member_leading_trivia)
                    methods.append(method)
                    member = method
                
                self.node_manager.add_node(member, member_start)
            elif member_name == "get" or member_name == "set":
                # property
                prop_name = self.reader.expect_identifier()
                prop = next(filter(lambda x: x.name == prop_name, properties), None)
                prop_type = None
                getter = None
                setter = None
                
                if member_name == "get":
                    # get propName(): propType { return ... }
                    self.context.append(f'''P[G]:{prop_name}''')
                    self.reader.expect_token("()", "expected '()' after property getter name")
                    prop_type = self.parse_type() if self.reader.read_token(":") else None
                    if declaration_only:
                        if prop_type == None:
                            self.reader.fail("Type is missing for property in declare class")
                        self.reader.expect_token(";")
                    else:
                        getter = self.expect_block("property getter body is missing")
                        if prop != None:
                            prop.getter = getter
                elif member_name == "set":
                    # set propName(value: propType) { ... }
                    self.context.append(f'''P[S]:{prop_name}''')
                    self.reader.expect_token("(", "expected '(' after property setter name")
                    self.reader.expect_identifier()
                    prop_type = self.parse_type() if self.reader.read_token(":") else None
                    self.reader.expect_token(")")
                    if declaration_only:
                        if prop_type == None:
                            self.reader.fail("Type is missing for property in declare class")
                        self.reader.expect_token(";")
                    else:
                        setter = self.expect_block("property setter body is missing")
                        if prop != None:
                            prop.setter = setter
                
                if prop == None:
                    prop = types.Property(prop_name, prop_type, getter, setter, visibility, is_static, member_leading_trivia)
                    properties.append(prop)
                    self.node_manager.add_node(prop, member_start)
                
                self.context.pop()
            else:
                self.context.append(f'''F:{member_name}''')
                
                type_and_init = self.parse_type_and_init()
                self.reader.expect_token(";")
                
                field = types.Field(member_name, type_and_init.type, type_and_init.init, visibility, is_static, None, member_leading_trivia)
                fields.append(field)
                
                self.node_manager.add_node(field, member_start)
                self.context.pop()
        
        cls_ = types.Class(cls_name, type_args, base_class, base_interfaces, fields, properties, constructor, methods, is_exported, leading_trivia)
        self.node_manager.add_node(cls_, cls_start)
        self.context.pop()
        return cls_
    
    def parse_enum(self, leading_trivia, is_exported):
        if not self.reader.read_token("enum"):
            return None
        enum_start = self.reader.prev_token_offset
        
        name = self.reader.expect_identifier("expected identifier after 'enum' keyword")
        self.context.append(f'''E:{name}''')
        
        members = []
        
        self.reader.expect_token("{")
        if not self.reader.read_token("}"):
            while True:
                if self.reader.peek_token("}"):
                    break
                # eg. "enum { A, B, }" (but multiline)
                
                enum_member = types.EnumMember(self.reader.expect_identifier())
                members.append(enum_member)
                self.node_manager.add_node(enum_member, self.reader.prev_token_offset)
                
                # TODO: generated code compatibility
                self.reader.read_token(f'''= "{enum_member.name}"''')
                if not (self.reader.read_token(",")):
                    break
            self.reader.expect_token("}")
        
        enum_obj = types.Enum(name, members, is_exported, leading_trivia)
        self.node_manager.add_node(enum_obj, enum_start)
        self.context.pop()
        return enum_obj
    
    @classmethod
    def calculate_relative_path(cls, curr_file, rel_path):
        if not rel_path.startswith("."):
            raise Error(f'''relPath must start with \'.\', but got \'{rel_path}\'''')
        
        curr = re.split("/", curr_file)
        curr.pop()
        # filename does not matter
        for part in re.split("/", rel_path):
            if part == "":
                raise Error(f'''relPath should not contain multiple \'/\' next to each other (relPath=\'{rel_path}\')''')
            if part == ".":
                # "./" == stay in current directory
                continue
            elif part == "..":
                # "../" == parent directory
                if len(curr) == 0:
                    raise Error(f'''relPath goes out of root (curr=\'{curr_file}\', relPath=\'{rel_path}\')''')
                curr.pop()
            else:
                curr.append(part)
        return "/".join(curr)
    
    @classmethod
    def calculate_import_scope(cls, curr_scope, import_file):
        if import_file.startswith("."):
            # relative
            return types.ExportScopeRef(curr_scope.package_name, TypeScriptParser2.calculate_relative_path(curr_scope.scope_name, import_file))
        else:
            path = re.split("/", import_file)
            pkg_name = path.pop(0)
            return types.ExportScopeRef(pkg_name, types.Package.index if len(path) == 0 else "/".join(path))
    
    def read_identifier(self):
        raw_id = self.reader.read_identifier()
        return re.sub("_+$", "", raw_id)
    
    def parse_import(self, leading_trivia):
        if not self.reader.read_token("import"):
            return None
        import_start = self.reader.prev_token_offset
        
        import_all_alias = None
        import_parts = []
        
        if self.reader.read_token("*"):
            self.reader.expect_token("as")
            import_all_alias = self.reader.expect_identifier()
        else:
            self.reader.expect_token("{")
            while True:
                if self.reader.peek_token("}"):
                    break
                
                imp = self.reader.expect_identifier()
                if self.reader.read_token("as"):
                    self.reader.fail("This is not yet supported")
                import_parts.append(types.UnresolvedImport(imp))
                self.node_manager.add_node(imp, self.reader.prev_token_offset)
                if not (self.reader.read_token(",")):
                    break
            self.reader.expect_token("}")
        
        self.reader.expect_token("from")
        module_name = self.reader.expect_string()
        self.reader.expect_token(";")
        
        import_scope = TypeScriptParser2.calculate_import_scope(self.export_scope, module_name) if self.export_scope != None else None
        
        imports = []
        if len(import_parts) > 0:
            imports.append(types.Import(import_scope, False, import_parts, None, leading_trivia))
        
        if import_all_alias != None:
            imports.append(types.Import(import_scope, True, None, import_all_alias, leading_trivia))
        #this.nodeManager.addNode(imports, importStart);
        return imports
    
    def parse_source_file(self):
        imports = []
        enums = []
        intfs = []
        classes = []
        funcs = []
        while True:
            leading_trivia = self.reader.read_leading_trivia()
            if self.reader.get_eof():
                break
            
            imps = self.parse_import(leading_trivia)
            if imps != None:
                for imp in imps:
                    imports.append(imp)
                continue
            
            modifiers = self.reader.read_modifiers(["export", "declare"])
            is_exported = "export" in modifiers
            is_declaration = "declare" in modifiers
            
            cls_ = self.parse_class(leading_trivia, is_exported, is_declaration)
            if cls_ != None:
                classes.append(cls_)
                continue
            
            enum_obj = self.parse_enum(leading_trivia, is_exported)
            if enum_obj != None:
                enums.append(enum_obj)
                continue
            
            intf = self.parse_interface(leading_trivia, is_exported)
            if intf != None:
                intfs.append(intf)
                continue
            
            if self.reader.read_token("function"):
                func_name = self.read_identifier()
                self.reader.expect_token("(")
                sig = self.parse_method_signature(False, is_declaration)
                funcs.append(types.GlobalFunction(func_name, sig.params, sig.body, sig.returns, is_exported, leading_trivia))
                continue
            
            break
        
        self.reader.skip_whitespace()
        
        stmts = []
        while True:
            leading_trivia = self.reader.read_leading_trivia()
            if self.reader.get_eof():
                break
            
            stmt = self.expect_statement()
            if stmt == None:
                continue
            
            stmt.leading_trivia = leading_trivia
            stmts.append(stmt)
        
        return types.SourceFile(imports, intfs, classes, enums, funcs, stats.Block(stmts), self.path, self.export_scope)
    
    def parse(self):
        return self.parse_source_file()
    
    @classmethod
    def parse_file(cls, source, path = None):
        return TypeScriptParser2(source, path).parse_source_file()