<?php

namespace Generator\PhpPlugins\JsToPhp;

use Generator\IGeneratorPlugin\IGeneratorPlugin;
use One\Ast\Expressions\InstanceMethodCallExpression;
use One\Ast\Expressions\Expression;
use One\Ast\Expressions\StaticMethodCallExpression;
use One\Ast\Expressions\RegexLiteral;
use One\Ast\Statements\Statement;
use One\Ast\AstTypes\ClassType;
use One\Ast\AstTypes\InterfaceType;
use One\Ast\Types\Class_;
use One\Ast\Types\Method;
use One\Ast\References\InstanceFieldReference;
use One\Ast\References\InstancePropertyReference;
use One\Ast\Interfaces\IExpression;
use Generator\PhpGenerator\PhpGenerator;

class JsToPhp implements IGeneratorPlugin {
    public $unhandledMethods;
    public $main;
    
    function __construct($main) {
        $this->main = $main;
        $this->unhandledMethods = new \OneLang\Set();
    }
    
    function convertMethod($cls, $obj, $method, $args) {
        if ($cls->name === "TsArray") {
            $objR = $this->main->expr($obj);
            $argsR = array_map(function ($x) { return $this->main->expr($x); }, $args);
            if ($method->name === "includes")
                return "in_array(" . $argsR[0] . ", " . $objR . ")";
            else if ($method->name === "set")
                return $objR . "[" . $argsR[0] . "] = " . $argsR[1];
            else if ($method->name === "get")
                return $objR . "[" . $argsR[0] . "]";
            else if ($method->name === "join")
                return "implode(" . $argsR[0] . ", " . $objR . ")";
            else if ($method->name === "map")
                return "array_map(" . $argsR[0] . ", " . $objR . ")";
            else if ($method->name === "push")
                return $objR . "[] = " . $argsR[0];
            else if ($method->name === "pop")
                return "array_pop(" . $objR . ")";
            else if ($method->name === "filter")
                return "array_values(array_filter(" . $objR . ", " . $argsR[0] . "))";
            else if ($method->name === "every")
                return "\\OneLang\\ArrayHelper::every(" . $objR . ", " . $argsR[0] . ")";
            else if ($method->name === "some")
                return "\\OneLang\\ArrayHelper::some(" . $objR . ", " . $argsR[0] . ")";
            else if ($method->name === "concat")
                return "array_merge(" . $objR . ", " . $argsR[0] . ")";
            else if ($method->name === "shift")
                return "array_shift(" . $objR . ")";
            else if ($method->name === "find")
                return "\\OneLang\\ArrayHelper::find(" . $objR . ", " . $argsR[0] . ")";
            else if ($method->name === "sort")
                return "sort(" . $objR . ")";
        }
        else if ($cls->name === "TsString") {
            $objR = $this->main->expr($obj);
            $argsR = array_map(function ($x) { return $this->main->expr($x); }, $args);
            if ($method->name === "split") {
                if ($args[0] instanceof RegexLiteral) {
                    $pattern = ($args[0])->pattern;
                    $modPattern = "/" . preg_replace("///", "\\/", $pattern) . "/";
                    return "preg_split(" . json_encode($modPattern, JSON_UNESCAPED_SLASHES) . ", " . $objR . ")";
                }
                
                return "explode(" . $argsR[0] . ", " . $objR . ")";
            }
            else if ($method->name === "replace") {
                if ($args[0] instanceof RegexLiteral)
                    return "preg_replace(" . json_encode("/" . ($args[0])->pattern . "/", JSON_UNESCAPED_SLASHES) . ", " . $argsR[1] . ", " . $objR . ")";
                
                return $argsR[0] . ".replace(" . $objR . ", " . $argsR[1] . ")";
            }
            else if ($method->name === "includes")
                return "strpos(" . $objR . ", " . $argsR[0] . ") !== false";
            else if ($method->name === "startsWith") {
                if (count($argsR) > 1)
                    return "substr_compare(" . $objR . ", " . $argsR[0] . ", " . $argsR[1] . ", strlen(" . $argsR[0] . ")) === 0";
                else
                    return "substr_compare(" . $objR . ", " . $argsR[0] . ", 0, strlen(" . $argsR[0] . ")) === 0";
            }
            else if ($method->name === "endsWith") {
                if (count($argsR) > 1)
                    return "substr_compare(" . $objR . ", " . $argsR[0] . ", " . $argsR[1] . " - strlen(" . $argsR[0] . "), strlen(" . $argsR[0] . ")) === 0";
                else
                    return "substr_compare(" . $objR . ", " . $argsR[0] . ", strlen(" . $objR . ") - strlen(" . $argsR[0] . "), strlen(" . $argsR[0] . ")) === 0";
            }
            else if ($method->name === "indexOf")
                return "strpos(" . $objR . ", " . $argsR[0] . ", " . $argsR[1] . ")";
            else if ($method->name === "lastIndexOf")
                return "strrpos(" . $objR . ", " . $argsR[0] . ", " . $argsR[1] . " - strlen(" . $objR . "))";
            else if ($method->name === "substr") {
                if (count($argsR) > 1)
                    return "substr(" . $objR . ", " . $argsR[0] . ", " . $argsR[1] . ")";
                else
                    return "substr(" . $objR . ", " . $argsR[0] . ")";
            }
            else if ($method->name === "substring")
                return "substr(" . $objR . ", " . $argsR[0] . ", " . $argsR[1] . " - (" . $argsR[0] . "))";
            else if ($method->name === "repeat")
                return "str_repeat(" . $objR . ", " . $argsR[0] . ")";
            else if ($method->name === "toUpperCase")
                return "strtoupper(" . $objR . ")";
            else if ($method->name === "toLowerCase")
                return "strtolower(" . $objR . ")";
            else if ($method->name === "get")
                return $objR . "[" . $argsR[0] . "]";
            else if ($method->name === "charCodeAt")
                return "ord(" . $objR . "[" . $argsR[0] . "])";
        }
        else if ($cls->name === "TsMap") {
            $objR = $this->main->expr($obj);
            $argsR = array_map(function ($x) { return $this->main->expr($x); }, $args);
            if ($method->name === "set")
                return $objR . "[" . $argsR[0] . "] = " . $argsR[1];
            else if ($method->name === "get")
                return "@" . $objR . "[" . $argsR[0] . "] ?? null";
            else if ($method->name === "hasKey")
                return "array_key_exists(" . $argsR[0] . ", " . $objR . ")";
        }
        else if ($cls->name === "Object") {
            $argsR = array_map(function ($x) { return $this->main->expr($x); }, $args);
            if ($method->name === "keys")
                return "array_keys(" . $argsR[0] . ")";
            else if ($method->name === "values")
                return "array_values(" . $argsR[0] . ")";
        }
        else if ($cls->name === "ArrayHelper") {
            $argsR = array_map(function ($x) { return $this->main->expr($x); }, $args);
            if ($method->name === "sortBy")
                return "\\OneLang\\ArrayHelper::sortBy(" . $argsR[0] . ", " . $argsR[1] . ")";
            else if ($method->name === "removeLastN")
                return "array_splice(" . $argsR[0] . ", -" . $argsR[1] . ")";
        }
        else if ($cls->name === "Math") {
            $argsR = array_map(function ($x) { return $this->main->expr($x); }, $args);
            if ($method->name === "floor")
                return "floor(" . $argsR[0] . ")";
        }
        else if ($cls->name === "JSON") {
            $argsR = array_map(function ($x) { return $this->main->expr($x); }, $args);
            if ($method->name === "stringify")
                return "json_encode(" . $argsR[0] . ", JSON_UNESCAPED_SLASHES)";
        }
        else if ($cls->name === "RegExpExecArray") {
            $objR = $this->main->expr($obj);
            $argsR = array_map(function ($x) { return $this->main->expr($x); }, $args);
            return $objR . "[" . $argsR[0] . "]";
        }
        else
            return null;
        
        $methodName = $cls->name . "." . $method->name;
        if (!$this->unhandledMethods->has($methodName)) {
            \OneLang\console::error("[JsToPython] Method was not handled: " . $cls->name . "." . $method->name);
            $this->unhandledMethods->add($methodName);
        }
        //debugger;
        return null;
    }
    
    function expr($expr) {
        if ($expr instanceof InstanceMethodCallExpression && $expr->object->actualType instanceof ClassType)
            return $this->convertMethod($expr->object->actualType->decl, $expr->object, $expr->method, $expr->args);
        else if ($expr instanceof InstancePropertyReference && $expr->object->actualType instanceof ClassType) {
            if ($expr->property->parentClass->name === "TsString" && $expr->property->name === "length")
                return "strlen(" . $this->main->expr($expr->object) . ")";
            if ($expr->property->parentClass->name === "TsArray" && $expr->property->name === "length")
                return "count(" . $this->main->expr($expr->object) . ")";
        }
        else if ($expr instanceof InstanceFieldReference && $expr->object->actualType instanceof ClassType) {
            if ($expr->field->parentInterface->name === "RegExpExecArray" && $expr->field->name === "length")
                return "count(" . $this->main->expr($expr->object) . ")";
        }
        else if ($expr instanceof StaticMethodCallExpression && $expr->method->parentInterface instanceof Class_)
            return $this->convertMethod($expr->method->parentInterface, null, $expr->method, $expr->args);
        return null;
    }
    
    function stmt($stmt) {
        return null;
    }
}
