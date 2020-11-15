<?php

namespace One\Transforms\InferTypesPlugins\LambdaResolver;

use One\Transforms\InferTypesPlugins\Helpers\InferTypesPlugin\InferTypesPlugin;
use One\Ast\Expressions\Expression;
use One\Ast\Types\Lambda;
use One\Ast\AstTypes\LambdaType;
use One\Ast\AstTypes\TypeHelper;

class LambdaResolver extends InferTypesPlugin {
    function __construct() {
        parent::__construct("LambdaResolver");
        
    }
    
    protected function setupLambdaParameterTypes($lambda) {
        if ($lambda->expectedType === null)
            return;
        
        if ($lambda->expectedType instanceof LambdaType) {
            $declParams = $lambda->expectedType->parameters;
            if (count($declParams) !== count($lambda->parameters))
                $this->errorMan->throw("Expected " . count($lambda->parameters) . " parameters for lambda, but got " . count($declParams) . "!");
            else
                for ($i = 0; $i < count($declParams); $i++) {
                    if ($lambda->parameters[$i]->type === null)
                        $lambda->parameters[$i]->type = $declParams[$i]->type;
                    else if (!TypeHelper::isAssignableTo($lambda->parameters[$i]->type, $declParams[$i]->type))
                        $this->errorMan->throw("Parameter type " . $lambda->parameters[$i]->type->repr() . " cannot be assigned to " . $declParams[$i]->type->repr() . ".");
                }
        }
        else
            $this->errorMan->throw("Expected LambdaType as Lambda's type!");
    }
    
    protected function visitLambda($lambda) {
        $this->setupLambdaParameterTypes($lambda);
    }
    
    function canTransform($expr) {
        return $expr instanceof Lambda;
    }
    
    function transform($expr) {
        $this->visitLambda($expr);
        // does not transform actually
        return $expr;
    }
}
