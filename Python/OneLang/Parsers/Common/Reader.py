from OneLangStdLib import *
import OneLang.Parsers.Common.Utils as utils
import re

class Cursor:
    def __init__(self, offset, line, column, line_start, line_end):
        self.offset = offset
        self.line = line
        self.column = column
        self.line_start = line_start
        self.line_end = line_end

class ParseError:
    def __init__(self, message, cursor = None, reader = None):
        self.message = message
        self.cursor = cursor
        self.reader = reader

class Reader:
    def __init__(self, input):
        self.ws_offset = 0
        self.offset = 0
        self.cursor_search = None
        self.line_comment = "//"
        self.supports_block_comment = True
        self.block_comment_start = "/*"
        self.block_comment_end = "*/"
        self.comment_disabled = False
        self.identifier_regex = "[A-Za-z_][A-Za-z0-9_]*"
        self.number_regex = "[+-]?(\\d*\\.\\d+|\\d+\\.\\d+|0x[0-9a-fA-F_]+|0b[01_]+|[0-9_]+)"
        self.errors = []
        self.hooks = None
        self.ws_line_counter = 0
        self.move_ws_offset = True
        self.prev_token_offset = -1
        self.input = input
        self.cursor_search = CursorPositionSearch(input)
    
    def get_eof(self):
        return self.offset >= len(self.input)
    
    def get_cursor(self):
        return self.cursor_search.get_cursor_for_offset(self.offset)
    
    def get_preview(self):
        preview = re.sub("\\n", "\\n", self.input[self.offset:self.offset + 30])
        if len(preview) == 30:
            preview += "..."
        return preview
    
    def line_preview(self, cursor):
        line = self.input[cursor.line_start:cursor.line_end - 1]
        return f'''{line}\n{" " * (cursor.column - 1)}^^^'''
    
    def fail(self, message, offset = -1):
        error = ParseError(message, self.cursor_search.get_cursor_for_offset(self.offset if offset == -1 else offset), self)
        self.errors.append(error)
        
        if self.hooks != None:
            self.hooks.error_callback(error)
        else:
            raise Error(f'''{message} at {error.cursor.line}:{error.cursor.column}\n{self.line_preview(error.cursor)}''')
    
    def skip_whitespace(self, include_in_trivia = False):
        
        while self.offset < len(self.input):
            c = self.input[self.offset]
            
            if c == "\n":
                self.ws_line_counter = self.ws_line_counter + 1
            
            if not (c == "\n" or c == "\r" or c == "\t" or c == " "):
                break
            self.offset = self.offset + 1
        
        if not include_in_trivia:
            self.ws_offset = self.offset
    
    def skip_until(self, token):
        index = self.input.find(token, self.offset)
        if index == -1:
            return False
        self.offset = index + len(token)
        if self.move_ws_offset:
            self.ws_offset = self.offset
        return True
    
    def skip_line(self):
        if not self.skip_until("\n"):
            self.offset = len(self.input)
    
    def is_alpha_num(self, c):
        n = ord(c[0])
        return (97 <= n and n <= 122) or (65 <= n and n <= 90) or (48 <= n and n <= 57) or n == 95
    
    def read_exactly(self, what):
        if self.input.startswith(what, self.offset):
            self.ws_offset = self.offset = self.offset + len(what)
            return True
        return False
    
    def read_char(self):
        # TODO: should we move wsOffset?
        self.offset = self.offset + 1
        return self.input[self.offset - 1]
    
    def peek_token(self, token):
        self.skip_whitespace_and_comment()
        
        if self.input.startswith(token, self.offset):
            # TODO: hackish way to make sure space comes after word tokens
            if self.is_alpha_num(token[len(token) - 1]) and self.is_alpha_num(self.input[self.offset + len(token)]):
                return False
            return True
        else:
            return False
    
    def read_token(self, token):
        if self.peek_token(token):
            self.prev_token_offset = self.offset
            self.ws_offset = self.offset = self.offset + len(token)
            return True
        return False
    
    def read_any_of(self, tokens):
        for token in tokens:
            if self.read_token(token):
                return token
        return None
    
    def expect_token(self, token, error_msg = None):
        if not self.read_token(token):
            self.fail(error_msg or f'''expected token \'{token}\'''')
    
    def expect_string(self, error_msg = None):
        result = self.read_string()
        if result == None:
            self.fail(error_msg or f'''expected string''')
        return result
    
    def expect_one_of(self, tokens):
        result = self.read_any_of(tokens)
        if result == None:
            self.fail(f'''expected one of the following tokens: {", ".join(tokens)}''')
        return result
    
    @classmethod
    def match_from_index(cls, pattern, input, offset):
        regex = RegExp(pattern, "gy")
        regex.last_index = offset
        matches = regex.exec(input)
        if matches == None:
            return None
        else:
            result = []
            i = 0
            
            while i < len(matches):
                result.append(matches[i])
                i = i + 1
            return result
    
    def peek_regex(self, pattern):
        matches = Reader.match_from_index(pattern, self.input, self.offset)
        return matches
    
    def read_regex(self, pattern):
        matches = Reader.match_from_index(pattern, self.input, self.offset)
        if matches != None:
            self.prev_token_offset = self.offset
            self.ws_offset = self.offset = self.offset + len(matches[0])
        return matches
    
    def skip_whitespace_and_comment(self):
        if self.comment_disabled:
            return
        
        self.move_ws_offset = False
        while True:
            self.skip_whitespace(True)
            if self.input.startswith(self.line_comment, self.offset):
                self.skip_line()
            elif self.supports_block_comment and self.input.startswith(self.block_comment_start, self.offset):
                if not self.skip_until(self.block_comment_end):
                    self.fail(f'''block comment end ("{self.block_comment_end}") was not found''')
            else:
                break
        self.move_ws_offset = True
    
    def read_leading_trivia(self):
        self.skip_whitespace_and_comment()
        this_line_start = self.input.rfind("\n", 0, self.offset)
        if this_line_start <= self.ws_offset:
            return ""
        
        result = self.input[self.ws_offset:this_line_start + 1]
        result = utils.Utils.deindent(result)
        self.ws_offset = this_line_start
        return result
    
    def read_identifier(self):
        self.skip_whitespace()
        id_match = self.read_regex(self.identifier_regex)
        if id_match == None:
            return None
        
        return id_match[0]
    
    def read_number(self):
        self.skip_whitespace()
        num_match = self.read_regex(self.number_regex)
        if num_match == None:
            return None
        
        if self.read_regex("[0-9a-zA-Z]") != None:
            self.fail("invalid character in number")
        
        return num_match[0]
    
    def read_string(self):
        self.skip_whitespace()
        
        sep_char = self.input[self.offset]
        if sep_char != "'" and sep_char != "\"":
            return None
        
        str = ""
        self.read_exactly(sep_char)
        while not self.read_exactly(sep_char):
            chr = self.read_char()
            if chr == "\\":
                esc = self.read_char()
                if esc == "n":
                    str += "\n"
                elif esc == "r":
                    str += "\r"
                elif esc == "t":
                    str += "\t"
                elif esc == "\\":
                    str += "\\"
                elif esc == sep_char:
                    str += sep_char
                else:
                    self.fail("invalid escape", self.offset - 1)
            else:
                chr_code = ord(chr[0])
                if not (32 <= chr_code and chr_code <= 126) or chr == "\\" or chr == sep_char:
                    self.fail(f'''not allowed character (code={chr_code})''', self.offset - 1)
                str += chr
        return str
    
    def expect_identifier(self, error_msg = None):
        id = self.read_identifier()
        if id == None:
            self.fail(error_msg or "expected identifier")
        return id
    
    def read_modifiers(self, modifiers):
        result = []
        while True:
            success = False
            for modifier in modifiers:
                if self.read_token(modifier):
                    result.append(modifier)
                    success = True
            if not success:
                break
        return result

class CursorPositionSearch:
    def __init__(self, input):
        self.line_offsets = [0]
        self.input = input
        i = 0
        
        while i < len(input):
            if input[i] == "\n":
                self.line_offsets.append(i + 1)
            i = i + 1
        self.line_offsets.append(len(input))
    
    def get_line_idx_for_offset(self, offset):
        low = 0
        high = len(self.line_offsets) - 1
        
        while low <= high:
            # @java var middle = (int)Math.floor((low + high) / 2);
            middle = Math.floor((low + high) / 2)
            middle_offset = self.line_offsets[middle]
            if offset == middle_offset:
                return middle
            elif offset <= middle_offset:
                high = middle - 1
            else:
                low = middle + 1
        
        return low - 1
    
    def get_cursor_for_offset(self, offset):
        line_idx = self.get_line_idx_for_offset(offset)
        line_start = self.line_offsets[line_idx]
        line_end = self.line_offsets[line_idx + 1]
        column = offset - line_start + 1
        if column < 1:
            raise Error("Column should not be < 1")
        return Cursor(offset, line_idx + 1, offset - line_start + 1, line_start, line_end)