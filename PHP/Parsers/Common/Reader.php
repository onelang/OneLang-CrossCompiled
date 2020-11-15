<?php

namespace Parsers\Common\Reader;

use Parsers\Common\Utils\Utils;

interface IReaderHooks {
    function errorCallback($error);
}

class Cursor {
    public $offset;
    public $line;
    public $column;
    public $lineStart;
    public $lineEnd;
    
    function __construct($offset, $line, $column, $lineStart, $lineEnd) {
        $this->offset = $offset;
        $this->line = $line;
        $this->column = $column;
        $this->lineStart = $lineStart;
        $this->lineEnd = $lineEnd;
    }
}

class ParseError {
    public $message;
    public $cursor;
    public $reader;
    
    function __construct($message, $cursor = null, $reader = null) {
        $this->message = $message;
        $this->cursor = $cursor;
        $this->reader = $reader;
    }
}

class Reader {
    public $wsOffset = 0;
    public $offset = 0;
    public $cursorSearch;
    public $lineComment = "//";
    public $supportsBlockComment = true;
    public $blockCommentStart = "/*";
    public $blockCommentEnd = "*/";
    public $commentDisabled = false;
    public $identifierRegex = "[A-Za-z_][A-Za-z0-9_]*";
    public $numberRegex = "[+-]?(\\d*\\.\\d+|\\d+\\.\\d+|0x[0-9a-fA-F_]+|0b[01_]+|[0-9_]+)";
    public $errors;
    public $hooks;
    public $wsLineCounter = 0;
    public $moveWsOffset = true;
    public $prevTokenOffset;
    public $input;
    
    function get_eof() {
        return $this->offset >= strlen($this->input);
    }
    
    function get_cursor() {
        return $this->cursorSearch->getCursorForOffset($this->offset);
    }
    
    function get_preview() {
        $preview = preg_replace("/\\n/", "\\n", substr($this->input, $this->offset, 30));
        if (strlen($preview) === 30)
            $preview .= "...";
        return $preview;
    }
    
    function __construct($input) {
        $this->input = $input;
        $this->errors = array();
        $this->hooks = null;
        $this->prevTokenOffset = -1;
        $this->cursorSearch = new CursorPositionSearch($input);
    }
    
    function linePreview($cursor) {
        $line = substr($this->input, $cursor->lineStart, $cursor->lineEnd - 1 - ($cursor->lineStart));
        return $line . "\n" . str_repeat(" ", $cursor->column - 1) . "^^^";
    }
    
    function fail($message, $offset = -1) {
        $error = new ParseError($message, $this->cursorSearch->getCursorForOffset($offset === -1 ? $this->offset : $offset), $this);
        $this->errors[] = $error;
        
        if ($this->hooks !== null)
            $this->hooks->errorCallback($error);
        else
            throw new \OneLang\Error($message . " at " . $error->cursor->line . ":" . $error->cursor->column . "\n" . $this->linePreview($error->cursor));
    }
    
    function skipWhitespace($includeInTrivia = false) {
        for (; $this->offset < strlen($this->input); $this->offset++) {
            $c = $this->input[$this->offset];
            
            if ($c === "\n")
                $this->wsLineCounter++;
            
            if (!($c === "\n" || $c === "\r" || $c === "\t" || $c === " "))
                break;
        }
        
        if (!$includeInTrivia)
            $this->wsOffset = $this->offset;
    }
    
    function skipUntil($token) {
        $index = strpos($this->input, $token, $this->offset);
        if ($index === -1)
            return false;
        $this->offset = $index + strlen($token);
        if ($this->moveWsOffset)
            $this->wsOffset = $this->offset;
        return true;
    }
    
    function skipLine() {
        if (!$this->skipUntil("\n"))
            $this->offset = strlen($this->input);
    }
    
    function isAlphaNum($c) {
        $n = ord($c[0]);
        return (97 <= $n && $n <= 122) || (65 <= $n && $n <= 90) || (48 <= $n && $n <= 57) || $n === 95;
    }
    
    function readExactly($what) {
        if (substr_compare($this->input, $what, $this->offset, strlen($what)) === 0) {
            $this->wsOffset = $this->offset = $this->offset + strlen($what);
            return true;
        }
        return false;
    }
    
    function readChar() {
        // TODO: should we move wsOffset?
        $this->offset++;
        return $this->input[$this->offset - 1];
    }
    
    function peekToken($token) {
        $this->skipWhitespaceAndComment();
        
        if (substr_compare($this->input, $token, $this->offset, strlen($token)) === 0) {
            // TODO: hackish way to make sure space comes after word tokens
            if ($this->isAlphaNum($token[strlen($token) - 1]) && $this->isAlphaNum($this->input[$this->offset + strlen($token)]))
                return false;
            return true;
        }
        else
            return false;
    }
    
    function readToken($token) {
        if ($this->peekToken($token)) {
            $this->prevTokenOffset = $this->offset;
            $this->wsOffset = $this->offset = $this->offset + strlen($token);
            return true;
        }
        return false;
    }
    
    function readAnyOf($tokens) {
        foreach ($tokens as $token) {
            if ($this->readToken($token))
                return $token;
        }
        return null;
    }
    
    function expectToken($token, $errorMsg = null) {
        if (!$this->readToken($token))
            $this->fail($errorMsg ?? "expected token '" . $token . "'");
    }
    
    function expectString($errorMsg = null) {
        $result = $this->readString();
        if ($result === null)
            $this->fail($errorMsg ?? "expected string");
        return $result;
    }
    
    function expectOneOf($tokens) {
        $result = $this->readAnyOf($tokens);
        if ($result === null)
            $this->fail("expected one of the following tokens: " . implode(", ", $tokens));
        return $result;
    }
    
    static function matchFromIndex($pattern, $input, $offset) {
        $regex = new \OneLang\RegExp($pattern, "gy");
        $regex->lastIndex = $offset;
        $matches = $regex->exec($input);
        if ($matches === null)
            return null;
        else {
            $result = array();
            for ($i = 0; $i < count($matches); $i++)
                $result[] = $matches[$i];
            return $result;
        }
    }
    
    function peekRegex($pattern) {
        $matches = Reader::matchFromIndex($pattern, $this->input, $this->offset);
        return $matches;
    }
    
    function readRegex($pattern) {
        $matches = Reader::matchFromIndex($pattern, $this->input, $this->offset);
        if ($matches !== null) {
            $this->prevTokenOffset = $this->offset;
            $this->wsOffset = $this->offset = $this->offset + strlen($matches[0]);
        }
        return $matches;
    }
    
    function skipWhitespaceAndComment() {
        if ($this->commentDisabled)
            return;
        
        $this->moveWsOffset = false;
        while (true) {
            $this->skipWhitespace(true);
            if (substr_compare($this->input, $this->lineComment, $this->offset, strlen($this->lineComment)) === 0)
                $this->skipLine();
            else if ($this->supportsBlockComment && substr_compare($this->input, $this->blockCommentStart, $this->offset, strlen($this->blockCommentStart)) === 0) {
                if (!$this->skipUntil($this->blockCommentEnd))
                    $this->fail("block comment end (\"" . $this->blockCommentEnd . "\") was not found");
            }
            else
                break;
        }
        $this->moveWsOffset = true;
    }
    
    function readLeadingTrivia() {
        $this->skipWhitespaceAndComment();
        $thisLineStart = strrpos($this->input, "\n", $this->offset - strlen($this->input));
        if ($thisLineStart <= $this->wsOffset)
            return "";
        
        $result = substr($this->input, $this->wsOffset, $thisLineStart + 1 - ($this->wsOffset));
        $result = Utils::deindent($result);
        $this->wsOffset = $thisLineStart;
        return $result;
    }
    
    function readIdentifier() {
        $this->skipWhitespace();
        $idMatch = $this->readRegex($this->identifierRegex);
        if ($idMatch === null)
            return null;
        
        return $idMatch[0];
    }
    
    function readNumber() {
        $this->skipWhitespace();
        $numMatch = $this->readRegex($this->numberRegex);
        if ($numMatch === null)
            return null;
        
        if ($this->readRegex("[0-9a-zA-Z]") !== null)
            $this->fail("invalid character in number");
        
        return $numMatch[0];
    }
    
    function readString() {
        $this->skipWhitespace();
        
        $sepChar = $this->input[$this->offset];
        if ($sepChar !== "'" && $sepChar !== "\"")
            return null;
        
        $str = "";
        $this->readExactly($sepChar);
        while (!$this->readExactly($sepChar)) {
            $chr = $this->readChar();
            if ($chr === "\\") {
                $esc = $this->readChar();
                if ($esc === "n")
                    $str .= "\n";
                else if ($esc === "r")
                    $str .= "\r";
                else if ($esc === "t")
                    $str .= "\t";
                else if ($esc === "\\")
                    $str .= "\\";
                else if ($esc === $sepChar)
                    $str .= $sepChar;
                else
                    $this->fail("invalid escape", $this->offset - 1);
            }
            else {
                $chrCode = ord($chr[0]);
                if (!(32 <= $chrCode && $chrCode <= 126) || $chr === "\\" || $chr === $sepChar)
                    $this->fail("not allowed character (code=" . $chrCode . ")", $this->offset - 1);
                $str .= $chr;
            }
        }
        return $str;
    }
    
    function expectIdentifier($errorMsg = null) {
        $id = $this->readIdentifier();
        if ($id === null)
            $this->fail($errorMsg ?? "expected identifier");
        return $id;
    }
    
    function readModifiers($modifiers) {
        $result = array();
        while (true) {
            $success = false;
            foreach ($modifiers as $modifier) {
                if ($this->readToken($modifier)) {
                    $result[] = $modifier;
                    $success = true;
                }
            }
            if (!$success)
                break;
        }
        return $result;
    }
}

class CursorPositionSearch {
    public $lineOffsets;
    public $input;
    
    function __construct($input) {
        $this->input = $input;
        $this->lineOffsets = array(0);
        for ($i = 0; $i < strlen($input); $i++) {
            if ($input[$i] === "\n")
                $this->lineOffsets[] = $i + 1;
        }
        $this->lineOffsets[] = strlen($input);
    }
    
    function getLineIdxForOffset($offset) {
        $low = 0;
        $high = count($this->lineOffsets) - 1;
        
        while ($low <= $high) {
            // @java var middle = (int)Math.floor((low + high) / 2);
            $middle = floor(($low + $high) / 2);
            $middleOffset = $this->lineOffsets[$middle];
            if ($offset === $middleOffset)
                return $middle;
            else if ($offset <= $middleOffset)
                $high = $middle - 1;
            else
                $low = $middle + 1;
        }
        
        return $low - 1;
    }
    
    function getCursorForOffset($offset) {
        $lineIdx = $this->getLineIdxForOffset($offset);
        $lineStart = $this->lineOffsets[$lineIdx];
        $lineEnd = $this->lineOffsets[$lineIdx + 1];
        $column = $offset - $lineStart + 1;
        if ($column < 1)
            throw new \OneLang\Error("Column should not be < 1");
        return new Cursor($offset, $lineIdx + 1, $offset - $lineStart + 1, $lineStart, $lineEnd);
    }
}
