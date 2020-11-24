<?php

namespace OneLang\Template\TemplateParser;

use OneLang\Parsers\Common\Reader\Reader;
use OneLang\Parsers\Common\ExpressionParser\ExpressionParser;
use OneLang\Template\Nodes\ExpressionNode;
use OneLang\Template\Nodes\ForNode;
use OneLang\Template\Nodes\ITemplateNode;
use OneLang\Template\Nodes\LiteralNode;
use OneLang\Template\Nodes\TemplateBlock;
use OneLang\One\Ast\Expressions\Identifier;

class TemplateParser {
    public $reader;
    public $exprParser;
    public $template;
    
    function __construct($template) {
        $this->template = $template;
        $this->reader = new Reader($template);
        $this->exprParser = new ExpressionParser($this->reader);
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
    
    function parseBlock() {
        $items = array();
        while (!$this->reader->get_eof()) {
            if ($this->reader->peekToken("{{/"))
                break;
            if ($this->reader->readToken("\${")) {
                $expr = $this->exprParser->parse();
                $items[] = new ExpressionNode($expr);
                $this->reader->expectToken("}");
            }
            else if ($this->reader->readToken("\$")) {
                $id = $this->reader->readIdentifier();
                $items[] = new ExpressionNode(new Identifier($id));
            }
            else if ($this->reader->readToken("{{")) {
                if ($this->reader->readToken("for")) {
                    $varName = $this->reader->readIdentifier();
                    $this->reader->expectToken("of");
                    $itemsExpr = $this->exprParser->parse();
                    $attrs = $this->parseAttributes();
                    $this->reader->expectToken("}}");
                    $body = $this->parseBlock();
                    $this->reader->expectToken("{{/for}}");
                    $items[] = new ForNode($varName, $itemsExpr, $body, @$attrs["joiner"] ?? null);
                }
                else {
                    $expr = $this->exprParser->parse();
                    $items[] = new ExpressionNode($expr);
                    $this->reader->expectToken("}}");
                }
            }
            else {
                $literal = $this->reader->readRegex("([^\\\\]\\\\(\\{\\{|\\\$)|\r|\n|(?!\\{\\{|\\$\\{|\\$).)*")[0];
                if ($literal === "")
                    throw new \OneLang\Core\Error("This should not happen!");
                $items[] = new LiteralNode($literal);
            }
        }
        return new TemplateBlock($items);
    }
    
    function parse() {
        return $this->parseBlock();
    }
}
