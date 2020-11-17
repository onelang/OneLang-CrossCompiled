<?php

namespace OneLang\One\Transforms\FillAttributesFromTrivia;

use OneLang\One\Ast\Types\SourceFile;
use OneLang\One\Ast\Types\IMethodBase;
use OneLang\One\Ast\Types\IHasAttributesAndTrivia;
use OneLang\One\Ast\Types\Package;
use OneLang\One\Ast\Types\IMethodBaseWithTrivia;
use OneLang\One\Ast\Statements\ForeachStatement;
use OneLang\One\Ast\Statements\ForStatement;
use OneLang\One\Ast\Statements\IfStatement;
use OneLang\One\Ast\Statements\Block;
use OneLang\One\Ast\Statements\WhileStatement;
use OneLang\One\Ast\Statements\DoStatement;
use OneLang\One\AstTransformer\AstTransformer;
use OneLang\One\Ast\Expressions\Expression;

class FillAttributesFromTrivia extends AstTransformer {
    function __construct() {
        parent::__construct("FillAttributesFromTrivia");
        
    }
    
    protected function visitAttributesAndTrivia($node) {
        $node->attributes = FillAttributesFromTrivia::processTrivia($node->leadingTrivia);
    }
    
    protected function visitExpression($expr) {
        return $expr;
    }
    
    static function processTrivia($trivia) {
        $result = Array();
        if ($trivia !== null && $trivia !== "") {
            $regex = new \OneCore\RegExp("(?:\\n|^)\\s*(?://|#|/\\*\\*?)\\s*@([A-Za-z0-9_.-]+) ?((?!\\n|\\*/|$).+)?");
            while (true) {
                $match = $regex->exec($trivia);
                if ($match === null)
                    break;
                if (array_key_exists($match[1], $result))
                    // @php $result[$match[1]] .= "\n" . $match[2];
                    // @python result[match[1]] += "\n" + match[2]
                    // @csharp result[match[1]] += "\n" + match[2];
                    // @java result.put(match[1], result.get(match[1]) + "\n" + match[2]);
                    $result[$match[1]] .= "\n" . $match[2];
                else
                    $result[$match[1]] = ($match[2] ?? "") === "" ? "true" : $match[2] ?? "";
            }
        }
        return $result;
    }
}
