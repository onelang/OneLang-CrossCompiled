<?php

namespace OneLang\One\Transforms\InferTypesPlugins\InferForeachVarType;

use OneLang\One\Transforms\InferTypesPlugins\Helpers\InferTypesPlugin\InferTypesPlugin;
use OneLang\One\Ast\AstTypes\ClassType;
use OneLang\One\Ast\AstTypes\InterfaceType;
use OneLang\One\Ast\AstTypes\IInterfaceType;
use OneLang\One\Ast\AstTypes\AnyType;
use OneLang\One\Ast\Statements\Statement;
use OneLang\One\Ast\Statements\ForeachStatement;

class InferForeachVarType extends InferTypesPlugin {
    function __construct() {
        parent::__construct("InferForeachVarType");
        
    }
    
    function handleStatement($stmt) {
        if ($stmt instanceof ForeachStatement) {
            $stmt->items = $this->main->runPluginsOn($stmt->items);
            $arrayType = $stmt->items->getType();
            $found = false;
            if ($arrayType instanceof ClassType || $arrayType instanceof InterfaceType) {
                $intfType = $arrayType;
                $isArrayType = \OneCore\ArrayHelper::some($this->main->currentFile->arrayTypes, function ($x) use ($intfType) { return $x->decl === $intfType->getDecl(); });
                if ($isArrayType && count($intfType->typeArguments) > 0) {
                    $stmt->itemVar->type = $intfType->typeArguments[0];
                    $found = true;
                }
            }
            
            if (!$found && !($arrayType instanceof AnyType))
                $this->errorMan->throw("Expected array as Foreach items variable, but got " . $arrayType->repr());
            
            $this->main->processBlock($stmt->body);
            return true;
        }
        return false;
    }
}
