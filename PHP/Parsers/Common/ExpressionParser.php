<?php

namespace Parsers\Common\ExpressionParser;

use Parsers\Common\Reader\Reader;
use Parsers\Common\NodeManager\NodeManager;
use One\Ast\AstTypes\UnresolvedType;
use One\Ast\Expressions\Expression;
use One\Ast\Expressions\MapLiteral;
use One\Ast\Expressions\ArrayLiteral;
use One\Ast\Expressions\UnaryType;
use One\Ast\Expressions\Identifier;
use One\Ast\Expressions\NumericLiteral;
use One\Ast\Expressions\StringLiteral;
use One\Ast\Expressions\UnresolvedCallExpression;
use One\Ast\Expressions\CastExpression;
use One\Ast\Expressions\BinaryExpression;
use One\Ast\Expressions\UnaryExpression;
use One\Ast\Expressions\ParenthesizedExpression;
use One\Ast\Expressions\ConditionalExpression;
use One\Ast\Expressions\ElementAccessExpression;
use One\Ast\Expressions\PropertyAccessExpression;
use One\Ast\Expressions\MapLiteralItem;
use Utils\ArrayHelper\ArrayHelper;
use One\Ast\Interfaces\IType;

interface IExpressionParserHooks {
    function unaryPrehook();
    
    function infixPrehook($left);
}

class Operator {
    public $text;
    public $precedence;
    public $isBinary;
    public $isRightAssoc;
    public $isPostfix;
    
    function __construct($text, $precedence, $isBinary, $isRightAssoc, $isPostfix) {
        $this->text = $text;
        $this->precedence = $precedence;
        $this->isBinary = $isBinary;
        $this->isRightAssoc = $isRightAssoc;
        $this->isPostfix = $isPostfix;
    }
}

class PrecedenceLevel {
    public $name;
    public $operators;
    public $binary;
    
    function __construct($name, $operators, $binary) {
        $this->name = $name;
        $this->operators = $operators;
        $this->binary = $binary;
    }
}

class ExpressionParserConfig {
    public $unary;
    public $precedenceLevels;
    public $rightAssoc;
    public $aliases;
    public $propertyAccessOps;
}

class ExpressionParser {
    public $operatorMap;
    public $operators;
    public $prefixPrecedence;
    public $stringLiteralType;
    public $numericLiteralType;
    public $reader;
    public $hooks;
    public $nodeManager;
    public $config;
    
    function __construct($reader, $hooks = null, $nodeManager = null, $config = null) {
        $this->reader = $reader;
        $this->hooks = $hooks;
        $this->nodeManager = $nodeManager;
        $this->config = $config;
        $this->stringLiteralType = null;
        $this->numericLiteralType = null;
        if ($this->config === null)
            $this->config = ExpressionParser::defaultConfig();
        $this->reconfigure();
    }
    
    static function defaultConfig() {
        $config = new ExpressionParserConfig();
        $config->unary = array("++", "--", "!", "not", "+", "-", "~");
        $config->precedenceLevels = array(new PrecedenceLevel("assignment", array("=", "+=", "-=", "*=", "/=", "<<=", ">>="), true), new PrecedenceLevel("conditional", array("?"), false), new PrecedenceLevel("or", array("||", "or"), true), new PrecedenceLevel("and", array("&&", "and"), true), new PrecedenceLevel("comparison", array(">=", "!=", "===", "!==", "==", "<=", ">", "<"), true), new PrecedenceLevel("sum", array("+", "-"), true), new PrecedenceLevel("product", array("*", "/", "%"), true), new PrecedenceLevel("bitwise", array("|", "&", "^"), true), new PrecedenceLevel("exponent", array("**"), true), new PrecedenceLevel("shift", array("<<", ">>"), true), new PrecedenceLevel("range", array("..."), true), new PrecedenceLevel("in", array("in"), true), new PrecedenceLevel("prefix", array(), false), new PrecedenceLevel("postfix", array("++", "--"), false), new PrecedenceLevel("call", array("("), false), new PrecedenceLevel("propertyAccess", array(), false), new PrecedenceLevel("elementAccess", array("["), false));
        $config->rightAssoc = array("**");
        $config->aliases = Array(
            "===" => "==",
            "!==" => "!=",
            "not" => "!",
            "and" => "&&",
            "or" => "||"
        );
        $config->propertyAccessOps = array(".", "::");
        return $config;
    }
    
    function reconfigure() {
        \OneLang\ArrayHelper::find($this->config->precedenceLevels, function ($x) { return $x->name === "propertyAccess"; })->operators = $this->config->propertyAccessOps;
        
        $this->operatorMap = Array();
        
        for ($i = 0; $i < count($this->config->precedenceLevels); $i++) {
            $level = $this->config->precedenceLevels[$i];
            $precedence = $i + 1;
            if ($level->name === "prefix")
                $this->prefixPrecedence = $precedence;
            
            if ($level->operators === null)
                continue;
            
            foreach ($level->operators as $opText) {
                $op = new Operator($opText, $precedence, $level->binary, in_array($opText, $this->config->rightAssoc), $level->name === "postfix");
                
                $this->operatorMap[$opText] = $op;
            }
        }
        
        $this->operators = \OneLang\ArrayHelper::sortBy(array_keys($this->operatorMap), function ($x) { return -strlen($x); });
    }
    
    function parseMapLiteral($keySeparator = ":", $startToken = "{", $endToken = "}") {
        if (!$this->reader->readToken($startToken))
            return null;
        
        $items = array();
        do {
            if ($this->reader->peekToken($endToken))
                break;
            
            $name = $this->reader->readString();
            if ($name === null)
                $name = $this->reader->expectIdentifier("expected string or identifier as map key");
            
            $this->reader->expectToken($keySeparator);
            $initializer = $this->parse();
            $items[] = new MapLiteralItem($name, $initializer);
        } while ($this->reader->readToken(","));
        
        $this->reader->expectToken($endToken);
        return new MapLiteral($items);
    }
    
    function parseArrayLiteral($startToken = "[", $endToken = "]") {
        if (!$this->reader->readToken($startToken))
            return null;
        
        $items = array();
        if (!$this->reader->readToken($endToken)) {
            do {
                $item = $this->parse();
                $items[] = $item;
            } while ($this->reader->readToken(","));
            
            $this->reader->expectToken($endToken);
        }
        return new ArrayLiteral($items);
    }
    
    function parseLeft($required = true) {
        $result = $this->hooks !== null ? $this->hooks->unaryPrehook() : null;
        if ($result !== null)
            return $result;
        
        $unary = $this->reader->readAnyOf($this->config->unary);
        if ($unary !== null) {
            $right = $this->parse($this->prefixPrecedence);
            return new UnaryExpression(UnaryType::PREFIX, $unary, $right);
        }
        
        $id = $this->reader->readIdentifier();
        if ($id !== null)
            return new Identifier($id);
        
        $num = $this->reader->readNumber();
        if ($num !== null)
            return new NumericLiteral($num);
        
        $str = $this->reader->readString();
        if ($str !== null)
            return new StringLiteral($str);
        
        if ($this->reader->readToken("(")) {
            $expr = $this->parse();
            $this->reader->expectToken(")");
            return new ParenthesizedExpression($expr);
        }
        
        if ($required)
            $this->reader->fail("unknown (literal / unary) token in expression");
        
        return null;
    }
    
    function parseOperator() {
        foreach ($this->operators as $opText) {
            if ($this->reader->peekToken($opText))
                return @$this->operatorMap[$opText] ?? null;
        }
        
        return null;
    }
    
    function parseCallArguments() {
        $args = array();
        
        if (!$this->reader->readToken(")")) {
            do {
                $arg = $this->parse();
                $args[] = $arg;
            } while ($this->reader->readToken(","));
            
            $this->reader->expectToken(")");
        }
        
        return $args;
    }
    
    function addNode($node, $start) {
        if ($this->nodeManager !== null)
            $this->nodeManager->addNode($node, $start);
    }
    
    function parse($precedence = 0, $required = true) {
        $this->reader->skipWhitespace();
        $leftStart = $this->reader->offset;
        $left = $this->parseLeft($required);
        if ($left === null)
            return null;
        $this->addNode($left, $leftStart);
        
        while (true) {
            if ($this->hooks !== null) {
                $parsed = $this->hooks->infixPrehook($left);
                if ($parsed !== null) {
                    $left = $parsed;
                    $this->addNode($left, $leftStart);
                    continue;
                }
            }
            
            $op = $this->parseOperator();
            if ($op === null || $op->precedence <= $precedence)
                break;
            $this->reader->expectToken($op->text);
            $opText = array_key_exists($op->text, $this->config->aliases) ? @$this->config->aliases[$op->text] ?? null : $op->text;
            
            if ($op->isBinary) {
                $right = $this->parse($op->isRightAssoc ? $op->precedence - 1 : $op->precedence);
                $left = new BinaryExpression($left, $opText, $right);
            }
            else if ($op->isPostfix)
                $left = new UnaryExpression(UnaryType::POSTFIX, $opText, $left);
            else if ($op->text === "?") {
                $whenTrue = $this->parse();
                $this->reader->expectToken(":");
                $whenFalse = $this->parse($op->precedence - 1);
                $left = new ConditionalExpression($left, $whenTrue, $whenFalse);
            }
            else if ($op->text === "(") {
                $args = $this->parseCallArguments();
                $left = new UnresolvedCallExpression($left, array(), $args);
            }
            else if ($op->text === "[") {
                $elementExpr = $this->parse();
                $this->reader->expectToken("]");
                $left = new ElementAccessExpression($left, $elementExpr);
            }
            else if (in_array($op->text, $this->config->propertyAccessOps)) {
                $prop = $this->reader->expectIdentifier("expected identifier as property name");
                $left = new PropertyAccessExpression($left, $prop);
            }
            else
                $this->reader->fail("parsing '" . $op->text . "' is not yet implemented");
            
            $this->addNode($left, $leftStart);
        }
        
        if ($left instanceof ParenthesizedExpression && $left->expression instanceof Identifier) {
            $expr = $this->parse(0, false);
            if ($expr !== null)
                return new CastExpression(new UnresolvedType($left->expression->text, array()), $expr);
        }
        
        return $left;
    }
}
