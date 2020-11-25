<?php

namespace OneLang\Template\Nodes;

use OneLang\One\Ast\Expressions\Expression;
use OneLang\Utils\TSOverviewGenerator\TSOverviewGenerator;
use OneLang\VM\ExprVM\ExprVM;
use OneLang\VM\ExprVM\VMContext;
use OneLang\VM\Values\ArrayValue;
use OneLang\VM\Values\StringValue;

interface ITemplateNode {
    function format($context);
}

class TemplateBlock implements ITemplateNode {
    public $items;
    
    function __construct($items) {
        $this->items = $items;
    }
    
    function format($context) {
        return implode("", array_map(function ($x) use ($context) { return $x->format($context); }, $this->items));
    }
}

class LiteralNode implements ITemplateNode {
    public $value;
    
    function __construct($value) {
        $this->value = $value;
    }
    
    function format($context) {
        return $this->value;
    }
}

class ExpressionNode implements ITemplateNode {
    public $expr;
    
    function __construct($expr) {
        $this->expr = $expr;
    }
    
    function format($context) {
        $value = (new ExprVM($context))->evaluate($this->expr);
        if ($value instanceof StringValue)
            return $value->value;
        
        if ($context->hooks !== null) {
            $result = $context->hooks->stringifyValue($value);
            if ($result !== null)
                return $result;
        }
        
        throw new \OneLang\Core\Error("ExpressionNode (" . TSOverviewGenerator::$preview->expr($this->expr) . ") return a non-string result!");
    }
}

class ForNode implements ITemplateNode {
    public $variableName;
    public $itemsExpr;
    public $body;
    public $joiner;
    
    function __construct($variableName, $itemsExpr, $body, $joiner) {
        $this->variableName = $variableName;
        $this->itemsExpr = $itemsExpr;
        $this->body = $body;
        $this->joiner = $joiner;
    }
    
    function format($context) {
        $items = (new ExprVM($context))->evaluate($this->itemsExpr);
        if (!($items instanceof ArrayValue))
            throw new \OneLang\Core\Error("ForNode items (" . TSOverviewGenerator::$preview->expr($this->itemsExpr) . ") return a non-array result!");
        
        $result = "";
        foreach (($items)->items as $item) {
            if ($this->joiner !== null && $result !== "")
                $result .= $this->joiner;
            
            $context->model->props[$this->variableName] = $item;
            $result .= $this->body->format($context);
        }
        /* unset @$context->model->props[$this->variableName] ?? null; */
        return $result;
    }
}
