<?php

namespace OneLang\One\Transforms\InferTypesPlugins\Helpers\InferTypesPlugin;

use OneLang\One\ErrorManager\ErrorManager;
use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\Transforms\InferTypes\InferTypes;
use OneLang\One\Ast\Statements\Statement;
use OneLang\One\Ast\Types\Property;
use OneLang\One\Ast\Types\Lambda;
use OneLang\One\Ast\Types\Method;
use OneLang\One\Ast\Types\IMethodBase;

class InferTypesPlugin {
    public $main;
    public $errorMan;
    public $name;
    
    function __construct($name) {
        $this->name = $name;
        $this->errorMan = null;
    }
    
    function canTransform($expr) {
        return false;
    }
    
    function canDetectType($expr) {
        return false;
    }
    
    function transform($expr) {
        return $expr;
    }
    
    function detectType($expr) {
        return false;
    }
    
    function handleProperty($prop) {
        return false;
    }
    
    function handleLambda($lambda) {
        return false;
    }
    
    function handleMethod($method) {
        return false;
    }
    
    function handleStatement($stmt) {
        return false;
    }
}
