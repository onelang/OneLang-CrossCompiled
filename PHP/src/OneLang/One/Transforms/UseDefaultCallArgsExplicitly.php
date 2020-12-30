<?php

namespace OneLang\One\Transforms\UseDefaultCallArgsExplicitly;

use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\Ast\Expressions\IMethodCallExpression;
use OneLang\One\Ast\Expressions\InstanceMethodCallExpression;
use OneLang\One\Ast\Expressions\NewExpression;
use OneLang\One\Ast\Expressions\StaticMethodCallExpression;
use OneLang\One\Ast\Types\IMethodBase;
use OneLang\One\Ast\Types\IMethodBaseWithTrivia;
use OneLang\One\Ast\Types\MethodParameter;
use OneLang\One\AstTransformer\AstTransformer;

class UseDefaultCallArgsExplicitly extends AstTransformer {
    function __construct() {
        parent::__construct("UseDefaultCallArgsExplicitly");
        
    }
    
    protected function getNewArgs($args, $method) {
        if (array_key_exists("UseDefaultCallArgsExplicitly", $method->attributes) && (@$method->attributes["UseDefaultCallArgsExplicitly"] ?? null) === "disable")
            return $args;
        if (count($args) >= count($method->parameters))
            return $args;
        
        $newArgs = array();
        for ($i = 0; $i < count($method->parameters); $i++) {
            $init = $method->parameters[$i]->initializer;
            if ($i >= count($args) && $init === null) {
                $this->errorMan->throw("Missing default value for parameter #" . ($i + 1) . "!");
                break;
            }
            $newArgs[] = $i < count($args) ? $args[$i] : $init;
        }
        return $newArgs;
    }
    
    protected function visitExpression($expr) {
        parent::visitExpression($expr);
        if ($expr instanceof NewExpression && $expr->cls->decl->constructor_ !== null)
            $expr->args = $this->getNewArgs($expr->args, $expr->cls->decl->constructor_);
        else if ($expr instanceof InstanceMethodCallExpression)
            $expr->args = $this->getNewArgs($expr->args, $expr->method);
        else if ($expr instanceof StaticMethodCallExpression)
            $expr->args = $this->getNewArgs($expr->args, $expr->method);
        return $expr;
    }
}
