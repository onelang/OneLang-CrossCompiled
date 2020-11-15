<?php

namespace Generator\PythonPlugins\JsToPython;

use Generator\IGeneratorPlugin\IGeneratorPlugin;
use One\Ast\Expressions\InstanceMethodCallExpression;
use One\Ast\Expressions\Expression;
use One\Ast\Expressions\StaticMethodCallExpression;
use One\Ast\Expressions\RegexLiteral;
use One\Ast\Statements\Statement;
use One\Ast\AstTypes\ClassType;
use One\Ast\AstTypes\InterfaceType;
use Generator\PythonGenerator\PythonGenerator;
use One\Ast\Types\Class_;
use One\Ast\Types\Method;
use One\Ast\References\InstanceFieldReference;
use One\Ast\References\InstancePropertyReference;
use One\Ast\Interfaces\IExpression;

class JsToPython implements IGeneratorPlugin {
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
                return $argsR[0] . " in " . $objR;
            else if ($method->name === "set")
                return $objR . "[" . $argsR[0] . "] = " . $argsR[1];
            else if ($method->name === "get")
                return $objR . "[" . $argsR[0] . "]";
            else if ($method->name === "join")
                return $argsR[0] . ".join(" . $objR . ")";
            else if ($method->name === "map")
                return "list(map(" . $argsR[0] . ", " . $objR . "))";
            else if ($method->name === "push")
                return $objR . ".append(" . $argsR[0] . ")";
            else if ($method->name === "pop")
                return $objR . ".pop()";
            else if ($method->name === "filter")
                return "list(filter(" . $argsR[0] . ", " . $objR . "))";
            else if ($method->name === "every")
                return "ArrayHelper.every(" . $argsR[0] . ", " . $objR . ")";
            else if ($method->name === "some")
                return "ArrayHelper.some(" . $argsR[0] . ", " . $objR . ")";
            else if ($method->name === "concat")
                return $objR . " + " . $argsR[0];
            else if ($method->name === "shift")
                return $objR . ".pop(0)";
            else if ($method->name === "find")
                return "next(filter(" . $argsR[0] . ", " . $objR . "), None)";
        }
        else if ($cls->name === "TsString") {
            $objR = $this->main->expr($obj);
            $argsR = array_map(function ($x) { return $this->main->expr($x); }, $args);
            if ($method->name === "split") {
                if ($args[0] instanceof RegexLiteral) {
                    $pattern = ($args[0])->pattern;
                    if (!substr_compare($pattern, "^", 0, strlen("^")) === 0) {
                        //return `${objR}.split(${JSON.stringify(pattern)})`;
                        $this->main->imports->add("import re");
                        return "re.split(" . json_encode($pattern, JSON_UNESCAPED_SLASHES) . ", " . $objR . ")";
                    }
                }
                
                return $argsR[0] . ".split(" . $objR . ")";
            }
            else if ($method->name === "replace") {
                if ($args[0] instanceof RegexLiteral) {
                    $this->main->imports->add("import re");
                    return "re.sub(" . json_encode(($args[0])->pattern, JSON_UNESCAPED_SLASHES) . ", " . $argsR[1] . ", " . $objR . ")";
                }
                
                return $argsR[0] . ".replace(" . $objR . ", " . $argsR[1] . ")";
            }
            else if ($method->name === "includes")
                return $argsR[0] . " in " . $objR;
            else if ($method->name === "startsWith")
                return $objR . ".startswith(" . implode(", ", $argsR) . ")";
            else if ($method->name === "indexOf")
                return $objR . ".find(" . $argsR[0] . ", " . $argsR[1] . ")";
            else if ($method->name === "lastIndexOf")
                return $objR . ".rfind(" . $argsR[0] . ", 0, " . $argsR[1] . ")";
            else if ($method->name === "substr")
                return count($argsR) === 1 ? $objR . "[" . $argsR[0] . ":]" : $objR . "[" . $argsR[0] . ":" . $argsR[0] . " + " . $argsR[1] . "]";
            else if ($method->name === "substring")
                return $objR . "[" . $argsR[0] . ":" . $argsR[1] . "]";
            else if ($method->name === "repeat")
                return $objR . " * (" . $argsR[0] . ")";
            else if ($method->name === "toUpperCase")
                return $objR . ".upper()";
            else if ($method->name === "toLowerCase")
                return $objR . ".lower()";
            else if ($method->name === "endsWith")
                return $objR . ".endswith(" . $argsR[0] . ")";
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
                return $objR . ".get(" . $argsR[0] . ")";
            else if ($method->name === "hasKey")
                return $argsR[0] . " in " . $objR;
        }
        else if ($cls->name === "Object") {
            $argsR = array_map(function ($x) { return $this->main->expr($x); }, $args);
            if ($method->name === "keys")
                return $argsR[0] . ".keys()";
            else if ($method->name === "values")
                return $argsR[0] . ".values()";
        }
        else if ($cls->name === "Set") {
            $objR = $this->main->expr($obj);
            $argsR = array_map(function ($x) { return $this->main->expr($x); }, $args);
            if ($method->name === "values")
                return $objR . ".keys()";
            else if ($method->name === "has")
                return $argsR[0] . " in " . $objR;
            else if ($method->name === "add")
                return $objR . "[" . $argsR[0] . "] = None";
        }
        else if ($cls->name === "ArrayHelper") {
            $argsR = array_map(function ($x) { return $this->main->expr($x); }, $args);
            if ($method->name === "sortBy")
                return "sorted(" . $argsR[0] . ", key=" . $argsR[1] . ")";
            else if ($method->name === "removeLastN")
                return "del " . $argsR[0] . "[-" . $argsR[1] . ":]";
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
                return "len(" . $this->main->expr($expr->object) . ")";
            if ($expr->property->parentClass->name === "TsArray" && $expr->property->name === "length")
                return "len(" . $this->main->expr($expr->object) . ")";
        }
        else if ($expr instanceof InstanceFieldReference && $expr->object->actualType instanceof ClassType) {
            if ($expr->field->parentInterface->name === "RegExpExecArray" && $expr->field->name === "length")
                return "len(" . $this->main->expr($expr->object) . ")";
        }
        else if ($expr instanceof StaticMethodCallExpression && $expr->method->parentInterface instanceof Class_)
            return $this->convertMethod($expr->method->parentInterface, null, $expr->method, $expr->args);
        return null;
    }
    
    function stmt($stmt) {
        return null;
    }
}
