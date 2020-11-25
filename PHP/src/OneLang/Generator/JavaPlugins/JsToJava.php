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
    
    function convertMethod($cls, $obj, $method, $args, $returnType) {
        $objR = $obj === null ? null : $this->main->expr($obj);
        $argsR = array_map(function ($x) { return $this->main->expr($x); }, $args);
        if ($cls->name === "TsString") {
            if ($method->name === "replace") {
                if ($args[0] instanceof RegexLiteral) {
                    $this->main->imports->add("java.util.regex.Pattern");
                    return $objR . ".replaceAll(" . json_encode(($args[0])->pattern, JSON_UNESCAPED_SLASHES) . ", " . $argsR[1] . ")";
                }
                
                return $argsR[0] . ".replace(" . $objR . ", " . $argsR[1] . ")";
            }
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
        
        return null;
    }
    
    function expr($expr) {
        if ($expr instanceof InstanceMethodCallExpression && $expr->object->actualType instanceof ClassType)
            return $this->convertMethod($expr->object->actualType->decl, $expr->object, $expr->method, $expr->args, $expr->actualType);
        else if ($expr instanceof StaticMethodCallExpression && $expr->method->parentInterface instanceof Class_)
            return $this->convertMethod($expr->method->parentInterface, null, $expr->method, $expr->args, $expr->actualType);
        return null;
    }
    
    function stmt($stmt) {
        return null;
    }
}
