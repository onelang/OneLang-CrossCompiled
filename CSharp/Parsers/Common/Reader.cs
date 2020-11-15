using Parsers.Common;
using System.Collections.Generic;

namespace Parsers.Common
{
    public interface IReaderHooks {
        void errorCallback(ParseError error);
    }
    
    public class Cursor {
        public int offset;
        public int line;
        public int column;
        public int lineStart;
        public int lineEnd;
        
        public Cursor(int offset, int line, int column, int lineStart, int lineEnd)
        {
            this.offset = offset;
            this.line = line;
            this.column = column;
            this.lineStart = lineStart;
            this.lineEnd = lineEnd;
        }
    }
    
    public class ParseError {
        public string message;
        public Cursor cursor;
        public Reader reader;
        
        public ParseError(string message, Cursor cursor = null, Reader reader = null)
        {
            this.message = message;
            this.cursor = cursor;
            this.reader = reader;
        }
    }
    
    public class Reader {
        public int wsOffset = 0;
        public int offset = 0;
        public CursorPositionSearch cursorSearch;
        public string lineComment = "//";
        public bool supportsBlockComment = true;
        public string blockCommentStart = "/*";
        public string blockCommentEnd = "*/";
        public bool commentDisabled = false;
        public string identifierRegex = "[A-Za-z_][A-Za-z0-9_]*";
        public string numberRegex = "[+-]?(\\d*\\.\\d+|\\d+\\.\\d+|0x[0-9a-fA-F_]+|0b[01_]+|[0-9_]+)";
        public List<ParseError> errors;
        public IReaderHooks hooks;
        public int wsLineCounter = 0;
        public bool moveWsOffset = true;
        public int prevTokenOffset;
        public string input;
        
        public bool eof {
            get {
            
                return this.offset >= this.input.length();
            }
        }
        public Cursor cursor {
            get {
            
                return this.cursorSearch.getCursorForOffset(this.offset);
            }
        }
        public string preview {
            get {
             {
                var preview = this.input.substr(this.offset, 30).replace(new RegExp("\\n"), "\\n");
                if (preview.length() == 30)
                    preview += "...";
                return preview;
            }
            }
        }
        
        public Reader(string input)
        {
            this.input = input;
            this.errors = new List<ParseError>();
            this.hooks = null;
            this.prevTokenOffset = -1;
            this.cursorSearch = new CursorPositionSearch(input);
        }
        
        public string linePreview(Cursor cursor)
        {
            var line = this.input.substring(cursor.lineStart, cursor.lineEnd - 1);
            return $"{line}\n{" ".repeat(cursor.column - 1)}^^^";
        }
        
        public void fail(string message, int offset = -1)
        {
            var error = new ParseError(message, this.cursorSearch.getCursorForOffset(offset == -1 ? this.offset : offset), this);
            this.errors.push(error);
            
            if (this.hooks != null)
                this.hooks.errorCallback(error);
            else
                throw new Error($"{message} at {error.cursor.line}:{error.cursor.column}\n{this.linePreview(error.cursor)}");
        }
        
        public void skipWhitespace(bool includeInTrivia = false)
        {
            for (; this.offset < this.input.length(); this.offset++) {
                var c = this.input.get(this.offset);
                
                if (c == "\n")
                    this.wsLineCounter++;
                
                if (!(c == "\n" || c == "\r" || c == "\t" || c == " "))
                    break;
            }
            
            if (!includeInTrivia)
                this.wsOffset = this.offset;
        }
        
        public bool skipUntil(string token)
        {
            var index = this.input.indexOf(token, this.offset);
            if (index == -1)
                return false;
            this.offset = index + token.length();
            if (this.moveWsOffset)
                this.wsOffset = this.offset;
            return true;
        }
        
        public void skipLine()
        {
            if (!this.skipUntil("\n"))
                this.offset = this.input.length();
        }
        
        public bool isAlphaNum(string c)
        {
            var n = c.charCodeAt(0);
            return (97 <= n && n <= 122) || (65 <= n && n <= 90) || (48 <= n && n <= 57) || n == 95;
        }
        
        public bool readExactly(string what)
        {
            if (this.input.startsWith(what, this.offset)) {
                this.wsOffset = this.offset = this.offset + what.length();
                return true;
            }
            return false;
        }
        
        public string readChar()
        {
            // TODO: should we move wsOffset?
            this.offset++;
            return this.input.get(this.offset - 1);
        }
        
        public bool peekToken(string token)
        {
            this.skipWhitespaceAndComment();
            
            if (this.input.startsWith(token, this.offset)) {
                // TODO: hackish way to make sure space comes after word tokens
                if (this.isAlphaNum(token.get(token.length() - 1)) && this.isAlphaNum(this.input.get(this.offset + token.length())))
                    return false;
                return true;
            }
            else
                return false;
        }
        
        public bool readToken(string token)
        {
            if (this.peekToken(token)) {
                this.prevTokenOffset = this.offset;
                this.wsOffset = this.offset = this.offset + token.length();
                return true;
            }
            return false;
        }
        
        public string readAnyOf(string[] tokens)
        {
            foreach (var token in tokens) {
                if (this.readToken(token))
                    return token;
            }
            return null;
        }
        
        public void expectToken(string token, string errorMsg = null)
        {
            if (!this.readToken(token))
                this.fail(errorMsg ?? $"expected token '{token}'");
        }
        
        public string expectString(string errorMsg = null)
        {
            var result = this.readString();
            if (result == null)
                this.fail(errorMsg ?? $"expected string");
            return result;
        }
        
        public string expectOneOf(string[] tokens)
        {
            var result = this.readAnyOf(tokens);
            if (result == null)
                this.fail($"expected one of the following tokens: {tokens.join(", ")}");
            return result;
        }
        
        public static string[] matchFromIndex(string pattern, string input, int offset)
        {
            var regex = new RegExp(pattern, "gy");
            regex.lastIndex = offset;
            var matches = regex.exec(input);
            if (matches == null)
                return null;
            else {
                var result = new List<string>();
                for (int i = 0; i < matches.length(); i++)
                    result.push(matches.get(i));
                return result.ToArray();
            }
        }
        
        public string[] peekRegex(string pattern)
        {
            var matches = Reader.matchFromIndex(pattern, this.input, this.offset);
            return matches;
        }
        
        public string[] readRegex(string pattern)
        {
            var matches = Reader.matchFromIndex(pattern, this.input, this.offset);
            if (matches != null) {
                this.prevTokenOffset = this.offset;
                this.wsOffset = this.offset = this.offset + matches.get(0).length();
            }
            return matches;
        }
        
        public void skipWhitespaceAndComment()
        {
            if (this.commentDisabled)
                return;
            
            this.moveWsOffset = false;
            while (true) {
                this.skipWhitespace(true);
                if (this.input.startsWith(this.lineComment, this.offset))
                    this.skipLine();
                else if (this.supportsBlockComment && this.input.startsWith(this.blockCommentStart, this.offset)) {
                    if (!this.skipUntil(this.blockCommentEnd))
                        this.fail($"block comment end (\"{this.blockCommentEnd}\") was not found");
                }
                else
                    break;
            }
            this.moveWsOffset = true;
        }
        
        public string readLeadingTrivia()
        {
            this.skipWhitespaceAndComment();
            var thisLineStart = this.input.lastIndexOf("\n", this.offset);
            if (thisLineStart <= this.wsOffset)
                return "";
            
            var result = this.input.substring(this.wsOffset, thisLineStart + 1);
            result = Utils.deindent(result);
            this.wsOffset = thisLineStart;
            return result;
        }
        
        public string readIdentifier()
        {
            this.skipWhitespace();
            var idMatch = this.readRegex(this.identifierRegex);
            if (idMatch == null)
                return null;
            
            return idMatch.get(0);
        }
        
        public string readNumber()
        {
            this.skipWhitespace();
            var numMatch = this.readRegex(this.numberRegex);
            if (numMatch == null)
                return null;
            
            if (this.readRegex("[0-9a-zA-Z]") != null)
                this.fail("invalid character in number");
            
            return numMatch.get(0);
        }
        
        public string readString()
        {
            this.skipWhitespace();
            
            var sepChar = this.input.get(this.offset);
            if (sepChar != "'" && sepChar != "\"")
                return null;
            
            var str = "";
            this.readExactly(sepChar);
            while (!this.readExactly(sepChar)) {
                var chr = this.readChar();
                if (chr == "\\") {
                    var esc = this.readChar();
                    if (esc == "n")
                        str += "\n";
                    else if (esc == "r")
                        str += "\r";
                    else if (esc == "t")
                        str += "\t";
                    else if (esc == "\\")
                        str += "\\";
                    else if (esc == sepChar)
                        str += sepChar;
                    else
                        this.fail("invalid escape", this.offset - 1);
                }
                else {
                    var chrCode = chr.charCodeAt(0);
                    if (!(32 <= chrCode && chrCode <= 126) || chr == "\\" || chr == sepChar)
                        this.fail($"not allowed character (code={chrCode})", this.offset - 1);
                    str += chr;
                }
            }
            return str;
        }
        
        public string expectIdentifier(string errorMsg = null)
        {
            var id = this.readIdentifier();
            if (id == null)
                this.fail(errorMsg ?? "expected identifier");
            return id;
        }
        
        public string[] readModifiers(string[] modifiers)
        {
            var result = new List<string>();
            while (true) {
                var success = false;
                foreach (var modifier in modifiers) {
                    if (this.readToken(modifier)) {
                        result.push(modifier);
                        success = true;
                    }
                }
                if (!success)
                    break;
            }
            return result.ToArray();
        }
    }
    
    public class CursorPositionSearch {
        public List<int> lineOffsets;
        public string input;
        
        public CursorPositionSearch(string input)
        {
            this.input = input;
            this.lineOffsets = new List<int> { 0 };
            for (int i = 0; i < input.length(); i++) {
                if (input.get(i) == "\n")
                    this.lineOffsets.push(i + 1);
            }
            this.lineOffsets.push(input.length());
        }
        
        public int getLineIdxForOffset(int offset)
        {
            var low = 0;
            var high = this.lineOffsets.length() - 1;
            
            while (low <= high) {
                // @java var middle = (int)Math.floor((low + high) / 2);
                var middle = Math.floor((low + high) / 2);
                var middleOffset = this.lineOffsets.get(middle);
                if (offset == middleOffset)
                    return middle;
                else if (offset <= middleOffset)
                    high = middle - 1;
                else
                    low = middle + 1;
            }
            
            return low - 1;
        }
        
        public Cursor getCursorForOffset(int offset)
        {
            var lineIdx = this.getLineIdxForOffset(offset);
            var lineStart = this.lineOffsets.get(lineIdx);
            var lineEnd = this.lineOffsets.get(lineIdx + 1);
            var column = offset - lineStart + 1;
            if (column < 1)
                throw new Error("Column should not be < 1");
            return new Cursor(offset, lineIdx + 1, offset - lineStart + 1, lineStart, lineEnd);
        }
    }
}