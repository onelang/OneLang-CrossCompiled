<?php

namespace One\Transforms\InferTypesPlugins\ArrayAndMapLiteralTypeInfer;

use One\Transforms\InferTypesPlugins\Helpers\InferTypesPlugin\InferTypesPlugin;
use One\Ast\Expressions\Expression;
use One\Ast\Expressions\ArrayLiteral;
use One\Ast\Expressions\MapLiteral;
use One\Ast\Expressions\CastExpression;
use One\Ast\Expressions\BinaryExpression;
use One\Ast\Expressions\ConditionalExpression;
use One\Ast\AstTypes\ClassType;
use One\Ast\AstTypes\AnyType;
use One\Ast\AstTypes\TypeHelper;
use One\Ast\Interfaces\IType;

class ArrayAndMapLiteralTypeInfer extends InferTypesPlugin {
    function __construct() {
        parent::__construct("ArrayAndMapLiteralTypeInfer");
        
    }
    
    protected function inferArrayOrMapItemType($items, $expectedType, $isMap) {
        $itemTypes = array();
        foreach ($items as $item) {
            if (!\OneLang\ArrayHelper::some($itemTypes, function ($t) use ($item) { return TypeHelper::equals($t, $item->getType()); }))
                $itemTypes[] = $item->getType();
        }
        
        $literalType = $isMap ? $this->main->currentFile->literalTypes->map : $this->main->currentFile->literalTypes->array;
        
        $itemType = null;
        if (count($itemTypes) === 0) {
            if ($expectedType === null) {
                $this->errorMan->warn("Could not determine the type of an empty " . ($isMap ? "MapLiteral" : "ArrayLiteral") . ", using AnyType instead");
                $itemType = AnyType::$instance;
            }
            else if ($expectedType instanceof ClassType && $expectedType->decl === $literalType->decl)
                $itemType = $expectedType->typeArguments[0];
            else
                $itemType = AnyType::$instance;
        }
        else if (count($itemTypes) === 1)
            $itemType = $itemTypes[0];
        else if (!($expectedType instanceof AnyType)) {
            $this->errorMan->warn("Could not determine the type of " . ($isMap ? "a MapLiteral" : "an ArrayLiteral") . "! Multiple types were found: " . implode(", ", array_map(function ($x) { return $x->repr(); }, $itemTypes)) . ", using AnyType instead");
            $itemType = AnyType::$instance;
        }
        return $itemType;
    }
    
    function canDetectType($expr) {
        return $expr instanceof ArrayLiteral || $expr instanceof MapLiteral;
    }
    
    function detectType($expr) {
        // make this work: `<{ [name: string]: SomeObject }> {}`
        if ($expr->parentNode instanceof CastExpression)
            $expr->setExpectedType($expr->parentNode->newType);
        else if ($expr->parentNode instanceof BinaryExpression && $expr->parentNode->operator === "=" && $expr->parentNode->right === $expr)
            $expr->setExpectedType($expr->parentNode->left->actualType);
        else if ($expr->parentNode instanceof ConditionalExpression && ($expr->parentNode->whenTrue === $expr || $expr->parentNode->whenFalse === $expr))
            $expr->setExpectedType($expr->parentNode->whenTrue === $expr ? $expr->parentNode->whenFalse->actualType : $expr->parentNode->whenTrue->actualType);
        
        if ($expr instanceof ArrayLiteral) {
            $itemType = $this->inferArrayOrMapItemType($expr->items, $expr->expectedType, false);
            $expr->setActualType(new ClassType($this->main->currentFile->literalTypes->array->decl, array($itemType)));
        }
        else if ($expr instanceof MapLiteral) {
            $itemType = $this->inferArrayOrMapItemType(array_map(function ($x) { return $x->value; }, $expr->items), $expr->expectedType, true);
            $expr->setActualType(new ClassType($this->main->currentFile->literalTypes->map->decl, array($itemType)));
        }
        
        return true;
    }
}
