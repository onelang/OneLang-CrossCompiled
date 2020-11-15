<?php

namespace One\Transforms\InferTypesPlugins\Helpers\InferTypesPlugin;

use One\ErrorManager\ErrorManager;
use One\Ast\Expressions\Expression;
use One\Transforms\InferTypes\InferTypes;
use One\Ast\Statements\Statement;
use One\Ast\Types\Property;
use One\Ast\Types\Lambda;
use One\Ast\Types\Method;
use One\Ast\Types\IMethodBase;

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
