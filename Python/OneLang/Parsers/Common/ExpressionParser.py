from OneLangStdLib import *
import OneLang.Parsers.Common.Reader as read
import OneLang.Parsers.Common.NodeManager as nodeMan
import OneLang.One.Ast.AstTypes as astTypes
import OneLang.One.Ast.Expressions as exprs
import OneLang.Utils.ArrayHelper as arrayHelp
import OneLang.One.Ast.Interfaces as ints

class Operator:
    def __init__(self, text, precedence, is_binary, is_right_assoc, is_postfix):
        self.text = text
        self.precedence = precedence
        self.is_binary = is_binary
        self.is_right_assoc = is_right_assoc
        self.is_postfix = is_postfix

class PrecedenceLevel:
    def __init__(self, name, operators, binary):
        self.name = name
        self.operators = operators
        self.binary = binary

class ExpressionParserConfig:
    def __init__(self):
        self.unary = None
        self.precedence_levels = None
        self.right_assoc = None
        self.aliases = None
        self.property_access_ops = None

class ExpressionParser:
    def __init__(self, reader, hooks = None, node_manager = None, config = None):
        self.operator_map = None
        self.operators = None
        self.prefix_precedence = None
        self.string_literal_type = None
        self.numeric_literal_type = None
        self.reader = reader
        self.hooks = hooks
        self.node_manager = node_manager
        self.config = config
        if self.config == None:
            self.config = ExpressionParser.default_config()
        self.reconfigure()
    
    @classmethod
    def default_config(cls):
        config = ExpressionParserConfig()
        config.unary = ["++", "--", "!", "not", "+", "-", "~"]
        config.precedence_levels = [PrecedenceLevel("assignment", ["=", "+=", "-=", "*=", "/=", "<<=", ">>="], True), PrecedenceLevel("conditional", ["?"], False), PrecedenceLevel("or", ["||", "or"], True), PrecedenceLevel("and", ["&&", "and"], True), PrecedenceLevel("comparison", [">=", "!=", "===", "!==", "==", "<=", ">", "<"], True), PrecedenceLevel("sum", ["+", "-"], True), PrecedenceLevel("product", ["*", "/", "%"], True), PrecedenceLevel("bitwise", ["|", "&", "^"], True), PrecedenceLevel("exponent", ["**"], True), PrecedenceLevel("shift", ["<<", ">>"], True), PrecedenceLevel("range", ["..."], True), PrecedenceLevel("in", ["in"], True), PrecedenceLevel("prefix", [], False), PrecedenceLevel("postfix", ["++", "--"], False), PrecedenceLevel("call", ["("], False), PrecedenceLevel("propertyAccess", [], False), PrecedenceLevel("elementAccess", ["["], False)]
        config.right_assoc = ["**"]
        config.aliases = {
            "===": "==",
            "!==": "!=",
            "not": "!",
            "and": "&&",
            "or": "||"
        }
        config.property_access_ops = [".", "::"]
        return config
    
    def reconfigure(self):
        next(filter(lambda x: x.name == "propertyAccess", self.config.precedence_levels), None).operators = self.config.property_access_ops
        
        self.operator_map = {}
        
        i = 0
        
        while i < len(self.config.precedence_levels):
            level = self.config.precedence_levels[i]
            precedence = i + 1
            if level.name == "prefix":
                self.prefix_precedence = precedence
            
            if level.operators == None:
                continue
            
            for op_text in level.operators:
                op = Operator(op_text, precedence, level.binary, op_text in self.config.right_assoc, level.name == "postfix")
                
                self.operator_map[op_text] = op
            i = i + 1
        
        self.operators = sorted(self.operator_map.keys(), key=lambda x: -len(x))
    
    def parse_map_literal(self, key_separator = ":", start_token = "{", end_token = "}"):
        if not self.reader.read_token(start_token):
            return None
        
        items = []
        while True:
            if self.reader.peek_token(end_token):
                break
            
            name = self.reader.read_string()
            if name == None:
                name = self.reader.expect_identifier("expected string or identifier as map key")
            
            self.reader.expect_token(key_separator)
            initializer = self.parse()
            items.append(exprs.MapLiteralItem(name, initializer))
            if not (self.reader.read_token(",")):
                break
        
        self.reader.expect_token(end_token)
        return exprs.MapLiteral(items)
    
    def parse_array_literal(self, start_token = "[", end_token = "]"):
        if not self.reader.read_token(start_token):
            return None
        
        items = []
        if not self.reader.read_token(end_token):
            while True:
                item = self.parse()
                items.append(item)
                if not (self.reader.read_token(",")):
                    break
            
            self.reader.expect_token(end_token)
        return exprs.ArrayLiteral(items)
    
    def parse_left(self, required = True):
        result = self.hooks.unary_prehook() if self.hooks != None else None
        if result != None:
            return result
        
        unary = self.reader.read_any_of(self.config.unary)
        if unary != None:
            right = self.parse(self.prefix_precedence)
            return exprs.UnaryExpression(exprs.UNARY_TYPE.PREFIX, unary, right)
        
        id = self.reader.read_identifier()
        if id != None:
            return exprs.Identifier(id)
        
        num = self.reader.read_number()
        if num != None:
            return exprs.NumericLiteral(num)
        
        str = self.reader.read_string()
        if str != None:
            return exprs.StringLiteral(str)
        
        if self.reader.read_token("("):
            expr = self.parse()
            self.reader.expect_token(")")
            return exprs.ParenthesizedExpression(expr)
        
        if required:
            self.reader.fail(f'''unknown (literal / unary) token in expression''')
        
        return None
    
    def parse_operator(self):
        for op_text in self.operators:
            if self.reader.peek_token(op_text):
                return self.operator_map.get(op_text)
        
        return None
    
    def parse_call_arguments(self):
        args = []
        
        if not self.reader.read_token(")"):
            while True:
                arg = self.parse()
                args.append(arg)
                if not (self.reader.read_token(",")):
                    break
            
            self.reader.expect_token(")")
        
        return args
    
    def add_node(self, node, start):
        if self.node_manager != None:
            self.node_manager.add_node(node, start)
    
    def parse(self, precedence = 0, required = True):
        self.reader.skip_whitespace()
        left_start = self.reader.offset
        left = self.parse_left(required)
        if left == None:
            return None
        self.add_node(left, left_start)
        
        while True:
            if self.hooks != None:
                parsed = self.hooks.infix_prehook(left)
                if parsed != None:
                    left = parsed
                    self.add_node(left, left_start)
                    continue
            
            op = self.parse_operator()
            if op == None or op.precedence <= precedence:
                break
            self.reader.expect_token(op.text)
            op_text = self.config.aliases.get(op.text) if op.text in self.config.aliases else op.text
            
            if op.is_binary:
                right = self.parse(op.precedence - 1 if op.is_right_assoc else op.precedence)
                left = exprs.BinaryExpression(left, op_text, right)
            elif op.is_postfix:
                left = exprs.UnaryExpression(exprs.UNARY_TYPE.POSTFIX, op_text, left)
            elif op.text == "?":
                when_true = self.parse()
                self.reader.expect_token(":")
                when_false = self.parse(op.precedence - 1)
                left = exprs.ConditionalExpression(left, when_true, when_false)
            elif op.text == "(":
                args = self.parse_call_arguments()
                left = exprs.UnresolvedCallExpression(left, [], args)
            elif op.text == "[":
                element_expr = self.parse()
                self.reader.expect_token("]")
                left = exprs.ElementAccessExpression(left, element_expr)
            elif op.text in self.config.property_access_ops:
                prop = self.reader.expect_identifier("expected identifier as property name")
                left = exprs.PropertyAccessExpression(left, prop)
            else:
                self.reader.fail(f'''parsing \'{op.text}\' is not yet implemented''')
            
            self.add_node(left, left_start)
        
        if isinstance(left, exprs.ParenthesizedExpression) and isinstance(left.expression, exprs.Identifier):
            expr = self.parse(0, False)
            if expr != None:
                return exprs.CastExpression(astTypes.UnresolvedType(left.expression.text, []), expr)
        
        return left