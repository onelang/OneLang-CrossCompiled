<?php

namespace OneLang\Generator\JavaPlugins\JsToJava;

use OneLang\Generator\IGeneratorPlugin\IGeneratorPlugin;
use OneLang\One\Ast\Expressions\InstanceMethodCallExpression;
use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\Ast\Expressions\StaticMethodCallExpression;
use OneLang\One\Ast\Expressions\RegexLiteral;
use OneLang\One\Ast\Expressions\ElementAccessExpression;
use OneLang\One\Ast\Expressions\ArrayLiteral;
use OneLang\One\Ast\Statements\Statement;
use OneLang\One\Ast\AstTypes\ClassType;
use OneLang\One\Ast\AstTypes\InterfaceType;
use OneLang\One\Ast\AstTypes\LambdaType;
use OneLang\One\Ast\AstTypes\TypeHelper;
use OneLang\One\Ast\Types\Class_;
use OneLang\One\Ast\Types\Lambda;
use OneLang\One\Ast\Types\Method;
use OneLang\One\Ast\References\InstanceFieldReference;
use OneLang\One\Ast\References\InstancePropertyReference;
use OneLang\One\Ast\References\VariableDeclarationReference;
use OneLang\One\Ast\References\VariableReference;
use OneLang\One\Ast\Interfaces\IExpression;
use OneLang\One\Ast\Interfaces\IType;
use OneLang\Generator\JavaGenerator\JavaGenerator;

class JsToJava implements IGeneratorPlugin {
    public $unhandledMethods;
    public $main;
    
    function __construct($main) {
        $this->main = $main;
        $this->unhandledMethods = new \OneLang\Core\Set();
    }
    
    function isArray($arrayExpr) {
        // TODO: InstanceMethodCallExpression is a hack, we should introduce real stream handling
        return $arrayExpr instanceof VariableReference && !$arrayExpr->getVariable()->mutability->mutated || $arrayExpr instanceof StaticMethodCallExpression || $arrayExpr instanceof InstanceMethodCallExpression;
    }
    
    function arrayStream($arrayExpr) {
        $isArray = $this->isArray($arrayExpr);
        $objR = $this->main->expr($arrayExpr);
        if ($isArray)
            $this->main->imports->add("java.util.Arrays");
        return $isArray ? "Arrays.stream(" . $objR . ")" : $objR . ".stream()";
    }
    
    function toArray($arrayType, $typeArgIdx = 0) {
        $type = ($arrayType)->typeArguments[$typeArgIdx];
        return "toArray(" . $this->main->type($type) . "[]::new)";
    }
    
    function convertMethod($cls, $obj, $method, $args, $returnType) {
        $objR = $obj === null ? null : $this->main->expr($obj);
        $argsR = array_map(function ($x) { return $this->main->expr($x); }, $args);
        if ($cls->name === "TsArray") {
            if ($method->name === "includes")
                // TsArray.includes(value): ${toStream(this)}.anyMatch($value}::equals)
                return $this->arrayStream($obj) . ".anyMatch(" . $argsR[0] . "::equals)";
            else if ($method->name === "set") {
                // TsArray.set(key, value): $this[$key] = $value
                if ($this->isArray($obj))
                    return $objR . "[" . $argsR[0] . "] = " . $argsR[1];
                else
                    return $objR . ".set(" . $argsR[0] . ", " . $argsR[1] . ")";
            }
            else if ($method->name === "pop")
                return $objR . ".remove(" . $objR . ".size() - 1)";
            else if ($method->name === "filter")
                return $this->arrayStream($obj) . ".filter(" . $argsR[0] . ")." . $this->toArray($returnType);
            else if ($method->name === "every") {
                $this->main->imports->add("io.onelang.std.core.StdArrayHelper");
                return "StdArrayHelper.allMatch(" . $objR . ", " . $argsR[0] . ")";
            }
            else if ($method->name === "some")
                return $this->arrayStream($obj) . ".anyMatch(" . $argsR[0] . ")";
            else if ($method->name === "concat") {
                $this->main->imports->add("java.util.stream.Stream");
                return "Stream.of(" . $objR . ", " . $argsR[0] . ").flatMap(Stream::of)." . $this->toArray($obj->getType());
            }
            else if ($method->name === "shift")
                return $objR . ".remove(0)";
            else if ($method->name === "find")
                return $this->arrayStream($obj) . ".filter(" . $argsR[0] . ").findFirst().orElse(null)";
            else if ($method->name === "sort") {
                $this->main->imports->add("java.util.Collections");
                return "Collections.sort(" . $objR . ")";
            }
        }
        else if ($cls->name === "TsString") {
            if ($method->name === "replace") {
                if ($args[0] instanceof RegexLiteral) {
                    $this->main->imports->add("java.util.regex.Pattern");
                    return $objR . ".replaceAll(" . json_encode(($args[0])->pattern, JSON_UNESCAPED_SLASHES) . ", " . $argsR[1] . ")";
                }
                
                return $argsR[0] . ".replace(" . $objR . ", " . $argsR[1] . ")";
            }
            else if ($method->name === "charCodeAt")
                return "(int)" . $objR . ".charAt(" . $argsR[0] . ")";
            else if ($method->name === "includes")
                return $objR . ".contains(" . $argsR[0] . ")";
            else if ($method->name === "get")
                return $objR . ".substring(" . $argsR[0] . ", " . $argsR[0] . " + 1)";
            else if ($method->name === "substr")
                return count($argsR) === 1 ? $objR . ".substring(" . $argsR[0] . ")" : $objR . ".substring(" . $argsR[0] . ", " . $argsR[0] . " + " . $argsR[1] . ")";
            else if ($method->name === "substring")
                return $objR . ".substring(" . $argsR[0] . ", " . $argsR[1] . ")";
            
            if ($method->name === "split" && $args[0] instanceof RegexLiteral) {
                $pattern = ($args[0])->pattern;
                return $objR . ".split(" . json_encode($pattern, JSON_UNESCAPED_SLASHES) . ", -1)";
            }
        }
        else if ($cls->name === "TsMap" || $cls->name === "Map") {
            if ($method->name === "set")
                return $objR . ".put(" . $argsR[0] . ", " . $argsR[1] . ")";
            else if ($method->name === "get")
                return $objR . ".get(" . $argsR[0] . ")";
            else if ($method->name === "hasKey" || $method->name === "has")
                return $objR . ".containsKey(" . $argsR[0] . ")";
            else if ($method->name === "delete")
                return $objR . ".remove(" . $argsR[0] . ")";
            else if ($method->name === "values")
                return $objR . ".values()." . $this->toArray($obj->getType(), 1);
        }
        else if ($cls->name === "Object") {
            if ($method->name === "keys")
                return $argsR[0] . ".keySet().toArray(String[]::new)";
            else if ($method->name === "values")
                return $argsR[0] . ".values()." . $this->toArray($args[0]->getType());
        }
        else if ($cls->name === "Set") {
            if ($method->name === "values")
                return $objR . "." . $this->toArray($obj->getType());
            else if ($method->name === "has")
                return $objR . ".contains(" . $argsR[0] . ")";
            else if ($method->name === "add")
                return $objR . ".add(" . $argsR[0] . ")";
        }
        else if ($cls->name === "ArrayHelper") { }
        else if ($cls->name === "Array") {
            if ($method->name === "from")
                return $argsR[0];
        }
        else if ($cls->name === "Promise") {
            if ($method->name === "resolve")
                return $argsR[0];
        }
        else if ($cls->name === "RegExpExecArray") {
            if ($method->name === "get")
                return $objR . "[" . $argsR[0] . "]";
        }
        else if (in_array($cls->name, array("console", "RegExp"))) {
            $this->main->imports->add("io.onelang.std.core." . $cls->name);
            return null;
        }
        else if (in_array($cls->name, array("JSON"))) {
            $this->main->imports->add("io.onelang.std.json." . $cls->name);
            return null;
        }
        else
            return null;
        
        $methodName = $cls->name . "." . $method->name;
        if (!$this->unhandledMethods->has($methodName)) {
            \OneLang\Core\console::error("[JsToJava] Method was not handled: " . $cls->name . "." . $method->name);
            $this->unhandledMethods->add($methodName);
        }
        //debugger;
        return null;
    }
    
    function expr($expr) {
        if ($expr instanceof InstanceMethodCallExpression && $expr->object->actualType instanceof ClassType)
            return $this->convertMethod($expr->object->actualType->decl, $expr->object, $expr->method, $expr->args, $expr->actualType);
        else if ($expr instanceof InstancePropertyReference && $expr->object->actualType instanceof ClassType) {
            if ($expr->property->parentClass->name === "TsString" && $expr->property->name === "length")
                return $this->main->expr($expr->object) . ".length()";
            if ($expr->property->parentClass->name === "TsArray" && $expr->property->name === "length")
                return $this->main->expr($expr->object) . "." . ($this->isArray($expr->object) ? "length" : "size()");
        }
        else if ($expr instanceof InstanceFieldReference && $expr->object->actualType instanceof ClassType) {
            if ($expr->field->parentInterface->name === "RegExpExecArray" && $expr->field->name === "length")
                return $this->main->expr($expr->object) . ".length";
            if ($expr->field->parentInterface->name === "Map" && $expr->field->name === "size")
                return $this->main->expr($expr->object) . ".size()";
        }
        else if ($expr instanceof StaticMethodCallExpression && $expr->method->parentInterface instanceof Class_)
            return $this->convertMethod($expr->method->parentInterface, null, $expr->method, $expr->args, $expr->actualType);
        return null;
    }
    
    function stmt($stmt) {
        return null;
    }
}
