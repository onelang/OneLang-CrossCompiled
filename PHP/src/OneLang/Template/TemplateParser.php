<?php

namespace OneLang\Template\TemplateParser;

use OneLang\Parsers\Common\Reader\Reader;
use OneLang\Template\Nodes\ExpressionNode;
use OneLang\Template\Nodes\ForNode;
use OneLang\Template\Nodes\ITemplateNode;
use OneLang\Template\Nodes\LiteralNode;
use OneLang\Template\Nodes\TemplateBlock;
use OneLang\One\Ast\Expressions\Identifier;
use OneLang\Parsers\TypeScriptParser\TypeScriptParser2;

class BlockIndentManager {
    public $deindentLen;
    public $block;
    public $indentLen;
    
    function __construct($block, $indentLen) {
        $this->block = $block;
        $this->indentLen = $indentLen;
        $this->deindentLen = -1;
    }
    
    function removePrevIndent() {
        if (count($this->block->items) === 0)
            return 0;
        $lastItem = $this->block->items[count($this->block->items) - 1];
        
        if (!($lastItem instanceof LiteralNode))
            return 0;
        $lit = $lastItem;
        
        for ($pos = strlen($lit->value) - 1; $pos >= 0; $pos--) {
            if ($lit->value[$pos] === "\n") {
                $indent = strlen($lit->value) - $pos - 1;
                $lit->value = substr($lit->value, 0, $pos - (0));
                if ($indent < 0) { }
                return $indent;
            }
            
            if ($lit->value[$pos] !== " ")
                break;
        }
        
        return 0;
    }
    
    function deindent($lit) {
        if ($this->indentLen === 0)
            return $lit;
        // do not deindent root nodes
               
        $lines = preg_split("/\\r?\\n/", $lit->value);
        if (count($lines) === 1)
            return $lit;
        
        $newLines = array($lines[0]);
        for ($iLine = 1; $iLine < count($lines); $iLine++) {
            $line = $lines[$iLine];
            
            if ($this->deindentLen === -1)
                for ($i = 0; $i < strlen($line); $i++) {
                    if ($line[$i] !== " ") {
                        $this->deindentLen = $i;
                        if ($this->deindentLen - $this->indentLen < 0) { }
                        break;
                    }
                }
            
            if ($this->deindentLen === -1)
                $newLines[] = $line;
            else {
                $spaceLen = strlen($line) < $this->deindentLen ? strlen($line) : $this->deindentLen;
                for ($i = 0; $i < $spaceLen; $i++) {
                    if ($line[$i] !== " ")
                        throw new \OneLang\Core\Error("invalid indent");
                }
                $newLines[] = substr($line, $this->deindentLen - $this->indentLen);
            }
        }
        $lit->value = implode("\n", $newLines);
        return $lit;
    }
}

class TemplateParser {
    public $reader;
    public $parser;
    public $template;
    
    function __construct($template) {
        $this->template = $template;
        $this->parser = new TypeScriptParser2($template);
        $this->parser->allowDollarIds = true;
        $this->reader = $this->parser->reader;
    }
    
    function parseAttributes() {
        $result = Array();
        while ($this->reader->readToken(",")) {
            $key = $this->reader->expectIdentifier();
            $value = $this->reader->readToken("=") ? $this->reader->expectString() : null;
            $result[$key] = $value;
        }
        return $result;
    }
    
    function parseBlock($indentLen = 0) {
        $block = new TemplateBlock(array());
        $indentMan = new BlockIndentManager($block, $indentLen);
        while (!$this->reader->get_eof()) {
            if ($this->reader->peekExactly("{{/"))
                break;
            if ($this->reader->readExactly("\${")) {
                $expr = $this->parser->parseExpression();
                $block->items[] = new ExpressionNode($expr);
                $this->reader->expectToken("}");
            }
            else if ($this->reader->readExactly("\$")) {
                $id = $this->reader->expectIdentifier();
                $block->items[] = new ExpressionNode(new Identifier($id));
            }
            else if ($this->reader->readExactly("{{")) {
                $blockIndentLen = $indentMan->removePrevIndent();
                
                if ($this->reader->readToken("for")) {
                    $varName = $this->reader->expectIdentifier();
                    $this->reader->expectToken("of");
                    $itemsExpr = $this->parser->parseExpression();
                    $attrs = $this->parseAttributes();
                    $this->reader->expectToken("}}");
                    $body = $this->parseBlock($blockIndentLen);
                    $this->reader->expectToken("{{/for}}");
                    $block->items[] = new ForNode($varName, $itemsExpr, $body, (@$attrs["joiner"] ?? null));
                }
                else {
                    $expr = $this->parser->parseExpression();
                    $block->items[] = new ExpressionNode($expr);
                    $this->reader->expectToken("}}");
                }
            }
            else {
                $literal = $this->reader->readRegex("([^\\\\]\\\\(\\{\\{|\\\$)|\r|\n|(?!\\{\\{|\\\$\\{|\\\$).)*")[0];
                if ($literal === "")
                    throw new \OneLang\Core\Error("This should not happen!");
                $block->items[] = $indentMan->deindent(new LiteralNode($literal));
            }
        }
        
        if ($indentLen !== 0)
            $indentMan->removePrevIndent();
        
        return $block;
    }
    
    function parse() {
        return $this->parseBlock();
    }
}
