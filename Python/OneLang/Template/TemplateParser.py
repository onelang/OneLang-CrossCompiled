from onelang_core import *
import OneLang.Parsers.Common.Reader as read
import OneLang.Parsers.Common.ExpressionParser as exprPars
import OneLang.Template.Nodes as nodes
import OneLang.One.Ast.Expressions as exprs
import OneLang.Parsers.TypeScriptParser as typeScrPars

class TemplateParser:
    def __init__(self, template):
        self.reader = None
        self.parser = None
        self.template = template
        self.parser = typeScrPars.TypeScriptParser2(template)
        self.parser.allow_dollar_ids = True
        self.reader = self.parser.reader
    
    def parse_attributes(self):
        result = {}
        while self.reader.read_token(","):
            key = self.reader.expect_identifier()
            value = self.reader.expect_string() if self.reader.read_token("=") else None
            result[key] = value
        return result
    
    def parse_block(self):
        items = []
        while not self.reader.get_eof():
            if self.reader.peek_token("{{/"):
                break
            if self.reader.read_token("${"):
                expr = self.parser.parse_expression()
                items.append(nodes.ExpressionNode(expr))
                self.reader.expect_token("}")
            elif self.reader.read_token("$"):
                id = self.reader.read_identifier()
                items.append(nodes.ExpressionNode(exprs.Identifier(id)))
            elif self.reader.read_token("{{"):
                if self.reader.read_token("for"):
                    var_name = self.reader.read_identifier()
                    self.reader.expect_token("of")
                    items_expr = self.parser.parse_expression()
                    attrs = self.parse_attributes()
                    self.reader.expect_token("}}")
                    body = self.parse_block()
                    self.reader.expect_token("{{/for}}")
                    items.append(nodes.ForNode(var_name, items_expr, body, attrs.get("joiner")))
                else:
                    expr = self.parser.parse_expression()
                    items.append(nodes.ExpressionNode(expr))
                    self.reader.expect_token("}}")
            else:
                literal = self.reader.read_regex("([^\\\\]\\\\(\\{\\{|\\$)|\r|\n|(?!\\{\\{|\\$\\{|\\$).)*")[0]
                if literal == "":
                    raise Error("This should not happen!")
                items.append(nodes.LiteralNode(literal))
        return nodes.TemplateBlock(items)
    
    def parse(self):
        return self.parse_block()