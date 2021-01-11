from onelang_core import *
import OneLang.Parsers.Common.Reader as read
import OneLang.Template.Nodes as nodes
import OneLang.One.Ast.Expressions as exprs
import OneLang.Parsers.TypeScriptParser as typeScrPars
import re

class BlockIndentManager:
    def __init__(self, block, indent_len):
        self.deindent_len = -1
        self.block = block
        self.indent_len = indent_len
    
    def remove_prev_indent(self):
        if len(self.block.items) == 0:
            return 0
        last_item = self.block.items[len(self.block.items) - 1]
        
        if not (isinstance(last_item, nodes.LiteralNode)):
            return 0
        lit = last_item
        
        pos = len(lit.value) - 1
        
        while pos >= 0:
            if lit.value[pos] == "\n":
                indent = len(lit.value) - pos - 1
                lit.value = lit.value[0:pos]
                if indent < 0:
                    pass
                return indent
            
            if lit.value[pos] != " ":
                break
            pos = pos - 1
        
        return 0
    
    def deindent(self, lit):
        if self.indent_len == 0:
            return lit
        # do not deindent root nodes
               
        lines = re.split("\\r?\\n", lit.value)
        if len(lines) == 1:
            return lit
        
        new_lines = [lines[0]]
        i_line = 1
        
        while i_line < len(lines):
            line = lines[i_line]
            
            if self.deindent_len == -1:
                i = 0
                
                while i < len(line):
                    if line[i] != " ":
                        self.deindent_len = i
                        if self.deindent_len - self.indent_len < 0:
                            pass
                        break
                    i = i + 1
            
            if self.deindent_len == -1:
                new_lines.append(line)
            else:
                space_len = len(line) if len(line) < self.deindent_len else self.deindent_len
                i = 0
                
                while i < space_len:
                    if line[i] != " ":
                        raise Error("invalid indent")
                    i = i + 1
                new_lines.append(line[self.deindent_len - self.indent_len:])
            i_line = i_line + 1
        lit.value = "\n".join(new_lines)
        return lit

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
    
    def parse_block(self, indent_len = 0):
        block = nodes.TemplateBlock([])
        indent_man = BlockIndentManager(block, indent_len)
        while not self.reader.get_eof():
            if self.reader.peek_exactly("{{/"):
                break
            if self.reader.read_exactly("${"):
                expr = self.parser.parse_expression()
                block.items.append(nodes.ExpressionNode(expr))
                self.reader.expect_token("}")
            elif self.reader.read_exactly("$"):
                id = self.reader.expect_identifier()
                block.items.append(nodes.ExpressionNode(exprs.Identifier(id)))
            elif self.reader.read_exactly("{{"):
                block_indent_len = indent_man.remove_prev_indent()
                
                if self.reader.read_token("for"):
                    var_name = self.reader.expect_identifier()
                    self.reader.expect_token("of")
                    items_expr = self.parser.parse_expression()
                    attrs = self.parse_attributes()
                    self.reader.expect_token("}}")
                    body = self.parse_block(block_indent_len)
                    self.reader.expect_token("{{/for}}")
                    block.items.append(nodes.ForNode(var_name, items_expr, body, attrs.get("joiner")))
                else:
                    expr = self.parser.parse_expression()
                    block.items.append(nodes.ExpressionNode(expr))
                    self.reader.expect_token("}}")
            else:
                literal = self.reader.read_regex("([^\\\\]\\\\(\\{\\{|\\$)|\r|\n|(?!\\{\\{|\\$\\{|\\$).)*")[0]
                if literal == "":
                    raise Error("This should not happen!")
                block.items.append(indent_man.deindent(nodes.LiteralNode(literal)))
        
        if indent_len != 0:
            indent_man.remove_prev_indent()
        
        return block
    
    def parse(self):
        return self.parse_block()