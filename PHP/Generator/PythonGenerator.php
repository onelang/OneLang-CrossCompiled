<?php

namespace Generator\PythonGenerator;

use One\Ast\Expressions\NewExpression;
use One\Ast\Expressions\Identifier;
use One\Ast\Expressions\TemplateString;
use One\Ast\Expressions\ArrayLiteral;
use One\Ast\Expressions\CastExpression;
use One\Ast\Expressions\BooleanLiteral;
use One\Ast\Expressions\StringLiteral;
use One\Ast\Expressions\NumericLiteral;
use One\Ast\Expressions\CharacterLiteral;
use One\Ast\Expressions\PropertyAccessExpression;
use One\Ast\Expressions\Expression;
use One\Ast\Expressions\ElementAccessExpression;
use One\Ast\Expressions\BinaryExpression;
use One\Ast\Expressions\UnresolvedCallExpression;
use One\Ast\Expressions\ConditionalExpression;
use One\Ast\Expressions\InstanceOfExpression;
use One\Ast\Expressions\ParenthesizedExpression;
use One\Ast\Expressions\RegexLiteral;
use One\Ast\Expressions\UnaryExpression;
use One\Ast\Expressions\UnaryType;
use One\Ast\Expressions\MapLiteral;
use One\Ast\Expressions\NullLiteral;
use One\Ast\Expressions\AwaitExpression;
use One\Ast\Expressions\UnresolvedNewExpression;
use One\Ast\Expressions\UnresolvedMethodCallExpression;
use One\Ast\Expressions\InstanceMethodCallExpression;
use One\Ast\Expressions\NullCoalesceExpression;
use One\Ast\Expressions\GlobalFunctionCallExpression;
use One\Ast\Expressions\StaticMethodCallExpression;
use One\Ast\Expressions\LambdaCallExpression;
use One\Ast\Expressions\IMethodCallExpression;
use One\Ast\Statements\Statement;
use One\Ast\Statements\ReturnStatement;
use One\Ast\Statements\UnsetStatement;
use One\Ast\Statements\ThrowStatement;
use One\Ast\Statements\ExpressionStatement;
use One\Ast\Statements\VariableDeclaration;
use One\Ast\Statements\BreakStatement;
use One\Ast\Statements\ForeachStatement;
use One\Ast\Statements\IfStatement;
use One\Ast\Statements\WhileStatement;
use One\Ast\Statements\ForStatement;
use One\Ast\Statements\DoStatement;
use One\Ast\Statements\ContinueStatement;
use One\Ast\Statements\ForVariable;
use One\Ast\Statements\TryStatement;
use One\Ast\Statements\Block;
use One\Ast\Types\Class_;
use One\Ast\Types\IClassMember;
use One\Ast\Types\SourceFile;
use One\Ast\Types\IMethodBase;
use One\Ast\Types\Constructor;
use One\Ast\Types\IVariable;
use One\Ast\Types\Lambda;
use One\Ast\Types\IImportable;
use One\Ast\Types\UnresolvedImport;
use One\Ast\Types\Interface_;
use One\Ast\Types\Enum;
use One\Ast\Types\IInterface;
use One\Ast\Types\Field;
use One\Ast\Types\Property;
use One\Ast\Types\MethodParameter;
use One\Ast\Types\IVariableWithInitializer;
use One\Ast\Types\Visibility;
use One\Ast\Types\IAstNode;
use One\Ast\Types\GlobalFunction;
use One\Ast\Types\Package;
use One\Ast\Types\SourcePath;
use One\Ast\Types\IHasAttributesAndTrivia;
use One\Ast\Types\ExportedScope;
use One\Ast\Types\ExportScopeRef;
use One\Ast\AstTypes\VoidType;
use One\Ast\AstTypes\ClassType;
use One\Ast\AstTypes\InterfaceType;
use One\Ast\AstTypes\EnumType;
use One\Ast\AstTypes\AnyType;
use One\Ast\AstTypes\LambdaType;
use One\Ast\AstTypes\NullType;
use One\Ast\AstTypes\GenericsType;
use One\Ast\References\ThisReference;
use One\Ast\References\EnumReference;
use One\Ast\References\ClassReference;
use One\Ast\References\MethodParameterReference;
use One\Ast\References\VariableDeclarationReference;
use One\Ast\References\ForVariableReference;
use One\Ast\References\ForeachVariableReference;
use One\Ast\References\SuperReference;
use One\Ast\References\StaticFieldReference;
use One\Ast\References\StaticPropertyReference;
use One\Ast\References\InstanceFieldReference;
use One\Ast\References\InstancePropertyReference;
use One\Ast\References\EnumMemberReference;
use One\Ast\References\CatchVariableReference;
use One\Ast\References\GlobalFunctionReference;
use One\Ast\References\StaticThisReference;
use One\Ast\References\Reference;
use One\Ast\References\VariableReference;
use Generator\GeneratedFile\GeneratedFile;
use Utils\TSOverviewGenerator\TSOverviewGenerator;
use Generator\IGeneratorPlugin\IGeneratorPlugin;
use Generator\PythonPlugins\JsToPython\JsToPython;
use Generator\NameUtils\NameUtils;
use One\Ast\Interfaces\IExpression;
use One\Ast\Interfaces\IType;
use Generator\IGenerator\IGenerator;
use One\ITransformer\ITransformer;

class PythonGenerator implements IGenerator {
    public $tmplStrLevel = 0;
    public $package;
    public $currentFile;
    public $imports;
    public $importAllScopes;
    public $currentClass;
    public $reservedWords;
    public $fieldToMethodHack;
    public $plugins;
    
    function __construct() {
        $this->reservedWords = array("from", "async", "global", "lambda", "cls", "import", "pass");
        $this->fieldToMethodHack = array();
        $this->plugins = array();
        $this->plugins[] = new JsToPython($this);
    }
    
    function getLangName() {
        return "Python";
    }
    
    function getExtension() {
        return "py";
    }
    
    function getTransforms() {
        return array();
    }
    
    function type($type) {
        if ($type instanceof ClassType) {
            if ($type->decl->name === "TsString")
                return "str";
            else if ($type->decl->name === "TsBoolean")
                return "bool";
            else if ($type->decl->name === "TsNumber")
                return "int";
            else
                return $this->clsName($type->decl);
        }
        else
            return "NOT-HANDLED-TYPE";
    }
    
    function splitName($name) {
        $nameParts = array();
        $partStartIdx = 0;
        for ($i = 1; $i < strlen($name); $i++) {
            $prevChrCode = ord($name[$i - 1]);
            $chrCode = ord($name[$i]);
            if (65 <= $chrCode && $chrCode <= 90 && !(65 <= $prevChrCode && $prevChrCode <= 90)) {
                // 'A' .. 'Z'
                $nameParts[] = strtolower(substr($name, $partStartIdx, $i - ($partStartIdx)));
                $partStartIdx = $i;
            }
            else if ($chrCode === 95) {
                // '-'
                $nameParts[] = substr($name, $partStartIdx, $i - ($partStartIdx));
                $partStartIdx = $i + 1;
            }
        }
        $nameParts[] = strtolower(substr($name, $partStartIdx));
        return $nameParts;
    }
    
    function name_($name) {
        if (in_array($name, $this->reservedWords))
            $name .= "_";
        if (in_array($name, $this->fieldToMethodHack))
            $name .= "()";
        return implode("_", $this->splitName($name));
    }
    
    function calcImportedName($exportScope, $name) {
        if ($this->importAllScopes->has($exportScope->getId()))
            return $name;
        else
            return $this->calcImportAlias($exportScope) . "." . $name;
    }
    
    function enumName($enum_, $isDecl = false) {
        $name = strtoupper($this->name_($enum_->name));
        if ($isDecl || $enum_->parentFile->exportScope === null || $enum_->parentFile === $this->currentFile)
            return $name;
        return $this->calcImportedName($enum_->parentFile->exportScope, $name);
    }
    
    function enumMemberName($name) {
        return strtoupper($this->name_($name));
    }
    
    function clsName($cls, $isDecl = false) {
        // TODO: hack
        if ($cls->name === "Set")
            return "dict";
        if ($isDecl || $cls->parentFile->exportScope === null || $cls->parentFile === $this->currentFile)
            return $cls->name;
        return $this->calcImportedName($cls->parentFile->exportScope, $cls->name);
    }
    
    function leading($item) {
        $result = "";
        if ($item->leadingTrivia !== null && strlen($item->leadingTrivia) > 0)
            $result .= preg_replace("////", "#", $item->leadingTrivia);
        //if (item.attributes !== null)
        //    result += Object.keys(item.attributes).map(x => `// @${x} ${item.attributes[x]}\n`).join("");
        return $result;
    }
    
    function preArr($prefix, $value) {
        return count($value) > 0 ? $prefix . implode(", ", $value) : "";
    }
    
    function preIf($prefix, $condition) {
        return $condition ? $prefix : "";
    }
    
    function pre($prefix, $value) {
        return $value !== null ? $prefix . $value : "";
    }
    
    function isTsArray($type) {
        return $type instanceof ClassType && $type->decl->name === "TsArray";
    }
    
    function vis($v) {
        return $v === Visibility::PRIVATE ? "__" : ($v === Visibility::PROTECTED ? "_" : ($v === Visibility::PUBLIC ? "" : "/* TODO: not set */public"));
    }
    
    function varWoInit($v, $attr) {
        return $this->name_($v->name);
    }
    
    function var($v, $attrs) {
        return $this->varWoInit($v, $attrs) . ($v->initializer !== null ? " = " . $this->expr($v->initializer) : "");
    }
    
    function exprCall($args) {
        return "(" . implode(", ", array_map(function ($x) { return $this->expr($x); }, $args)) . ")";
    }
    
    function callParams($args) {
        $argReprs = array();
        for ($i = 0; $i < count($args); $i++)
            $argReprs[] = $this->expr($args[$i]);
        return "(" . implode(", ", $argReprs) . ")";
    }
    
    function methodCall($expr) {
        return $this->name_($expr->method->name) . $this->callParams($expr->args);
    }
    
    function expr($expr) {
        foreach ($this->plugins as $plugin) {
            $result = $plugin->expr($expr);
            if ($result !== null)
                return $result;
        }
        
        $res = "UNKNOWN-EXPR";
        if ($expr instanceof NewExpression) {
            // TODO: hack
            if ($expr->cls->decl->name === "Set")
                $res = count($expr->args) === 0 ? "dict()" : "dict.fromkeys" . $this->callParams($expr->args);
            else
                $res = $this->clsName($expr->cls->decl) . $this->callParams($expr->args);
        }
        else if ($expr instanceof UnresolvedNewExpression)
            $res = "/* TODO: UnresolvedNewExpression */ " . $expr->cls->typeName . "(" . implode(", ", array_map(function ($x) { return $this->expr($x); }, $expr->args)) . ")";
        else if ($expr instanceof Identifier)
            $res = "/* TODO: Identifier */ " . $expr->text;
        else if ($expr instanceof PropertyAccessExpression)
            $res = "/* TODO: PropertyAccessExpression */ " . $this->expr($expr->object) . "." . $expr->propertyName;
        else if ($expr instanceof UnresolvedCallExpression)
            $res = "/* TODO: UnresolvedCallExpression */ " . $this->expr($expr->func) . $this->exprCall($expr->args);
        else if ($expr instanceof UnresolvedMethodCallExpression)
            $res = "/* TODO: UnresolvedMethodCallExpression */ " . $this->expr($expr->object) . "." . $expr->methodName . $this->exprCall($expr->args);
        else if ($expr instanceof InstanceMethodCallExpression)
            $res = $this->expr($expr->object) . "." . $this->methodCall($expr);
        else if ($expr instanceof StaticMethodCallExpression) {
            //const parent = expr.method.parentInterface === this.currentClass ? "cls" : this.clsName(expr.method.parentInterface);
            $parent = $this->clsName($expr->method->parentInterface);
            $res = $parent . "." . $this->methodCall($expr);
        }
        else if ($expr instanceof GlobalFunctionCallExpression) {
            $this->imports->add("from OneLangStdLib import *");
            $res = $this->name_($expr->func->name) . $this->exprCall($expr->args);
        }
        else if ($expr instanceof LambdaCallExpression)
            $res = $this->expr($expr->method) . "(" . implode(", ", array_map(function ($x) { return $this->expr($x); }, $expr->args)) . ")";
        else if ($expr instanceof BooleanLiteral)
            $res = ($expr->boolValue ? "True" : "False");
        else if ($expr instanceof StringLiteral)
            $res = json_encode($expr->stringValue, JSON_UNESCAPED_SLASHES);
        else if ($expr instanceof NumericLiteral)
            $res = $expr->valueAsText;
        else if ($expr instanceof CharacterLiteral)
            $res = "'" . $expr->charValue . "'";
        else if ($expr instanceof ElementAccessExpression)
            $res = $this->expr($expr->object) . "[" . $this->expr($expr->elementExpr) . "]";
        else if ($expr instanceof TemplateString) {
            $parts = array();
            foreach ($expr->parts as $part) {
                if ($part->isLiteral) {
                    $lit = "";
                    for ($i = 0; $i < strlen($part->literalText); $i++) {
                        $chr = $part->literalText[$i];
                        if ($chr === "\n")
                            $lit .= "\\n";
                        else if ($chr === "\r")
                            $lit .= "\\r";
                        else if ($chr === "\t")
                            $lit .= "\\t";
                        else if ($chr === "\\")
                            $lit .= "\\\\";
                        else if ($chr === "'")
                            $lit .= "\\'";
                        else if ($chr === "{")
                            $lit .= "{{";
                        else if ($chr === "}")
                            $lit .= "}}";
                        else {
                            $chrCode = ord($chr[0]);
                            if (32 <= $chrCode && $chrCode <= 126)
                                $lit .= $chr;
                            else
                                throw new \OneLang\Error("invalid char in template string (code=" . $chrCode . ")");
                        }
                    }
                    $parts[] = $lit;
                }
                else {
                    $this->tmplStrLevel++;
                    $repr = $this->expr($part->expression);
                    $this->tmplStrLevel--;
                    $parts[] = $part->expression instanceof ConditionalExpression ? "{(" . $repr . ")}" : "{" . $repr . "}";
                }
            }
            $res = $this->tmplStrLevel === 1 ? "f'" . implode("", $parts) . "'" : "f'''" . implode("", $parts) . "'''";
        }
        else if ($expr instanceof BinaryExpression) {
            $op = $expr->operator === "&&" ? "and" : ($expr->operator === "||" ? "or" : $expr->operator);
            $res = $this->expr($expr->left) . " " . $op . " " . $this->expr($expr->right);
        }
        else if ($expr instanceof ArrayLiteral)
            $res = "[" . implode(", ", array_map(function ($x) { return $this->expr($x); }, $expr->items)) . "]";
        else if ($expr instanceof CastExpression)
            $res = $this->expr($expr->expression);
        else if ($expr instanceof ConditionalExpression)
            $res = $this->expr($expr->whenTrue) . " if " . $this->expr($expr->condition) . " else " . $this->expr($expr->whenFalse);
        else if ($expr instanceof InstanceOfExpression)
            $res = "isinstance(" . $this->expr($expr->expr) . ", " . $this->type($expr->checkType) . ")";
        else if ($expr instanceof ParenthesizedExpression)
            $res = "(" . $this->expr($expr->expression) . ")";
        else if ($expr instanceof RegexLiteral)
            $res = "RegExp(" . json_encode($expr->pattern, JSON_UNESCAPED_SLASHES) . ")";
        else if ($expr instanceof Lambda) {
            $body = "INVALID-BODY";
            if (count($expr->body->statements) === 1 && $expr->body->statements[0] instanceof ReturnStatement)
                $body = $this->expr(($expr->body->statements[0])->expression);
            else
                \OneLang\console::error("Multi-line lambda is not yet supported for Python: " . TSOverviewGenerator::$preview->nodeRepr($expr));
            
            $params = array_map(function ($x) { return $this->name_($x->name); }, $expr->parameters);
            
            $res = "lambda " . implode(", ", $params) . ": " . $body;
        }
        else if ($expr instanceof UnaryExpression && $expr->unaryType === UnaryType::PREFIX) {
            $op = $expr->operator === "!" ? "not " : $expr->operator;
            if ($op === "++")
                $res = $this->expr($expr->operand) . " = " . $this->expr($expr->operand) . " + 1";
            else if ($op === "--")
                $res = $this->expr($expr->operand) . " = " . $this->expr($expr->operand) . " - 1";
            else
                $res = $op . $this->expr($expr->operand);
        }
        else if ($expr instanceof UnaryExpression && $expr->unaryType === UnaryType::POSTFIX) {
            if ($expr->operator === "++")
                $res = $this->expr($expr->operand) . " = " . $this->expr($expr->operand) . " + 1";
            else if ($expr->operator === "--")
                $res = $this->expr($expr->operand) . " = " . $this->expr($expr->operand) . " - 1";
            else
                $res = $this->expr($expr->operand) . $expr->operator;
        }
        else if ($expr instanceof MapLiteral) {
            $repr = implode(",\n", array_map(function ($item) { return json_encode($item->key, JSON_UNESCAPED_SLASHES) . ": " . $this->expr($item->value); }, $expr->items));
            $res = count($expr->items) === 0 ? "{}" : "{\n" . $this->pad($repr) . "\n}";
        }
        else if ($expr instanceof NullLiteral)
            $res = "None";
        else if ($expr instanceof AwaitExpression)
            $res = $this->expr($expr->expr);
        else if ($expr instanceof ThisReference)
            $res = "self";
        else if ($expr instanceof StaticThisReference)
            $res = "cls";
        else if ($expr instanceof EnumReference)
            $res = $this->enumName($expr->decl);
        else if ($expr instanceof ClassReference)
            $res = $this->name_($expr->decl->name);
        else if ($expr instanceof MethodParameterReference)
            $res = $this->name_($expr->decl->name);
        else if ($expr instanceof VariableDeclarationReference)
            $res = $this->name_($expr->decl->name);
        else if ($expr instanceof ForVariableReference)
            $res = $this->name_($expr->decl->name);
        else if ($expr instanceof ForeachVariableReference)
            $res = $this->name_($expr->decl->name);
        else if ($expr instanceof CatchVariableReference)
            $res = $this->name_($expr->decl->name);
        else if ($expr instanceof GlobalFunctionReference)
            $res = $this->name_($expr->decl->name);
        else if ($expr instanceof SuperReference)
            $res = "super()";
        else if ($expr instanceof StaticFieldReference)
            $res = $this->clsName($expr->decl->parentInterface) . "." . $this->name_($expr->decl->name);
        else if ($expr instanceof StaticPropertyReference)
            $res = $this->clsName($expr->decl->parentClass) . ".get_" . $this->name_($expr->decl->name) . "()";
        else if ($expr instanceof InstanceFieldReference)
            $res = $this->expr($expr->object) . "." . $this->name_($expr->field->name);
        else if ($expr instanceof InstancePropertyReference)
            $res = $this->expr($expr->object) . ".get_" . $this->name_($expr->property->name) . "()";
        else if ($expr instanceof EnumMemberReference)
            $res = $this->enumName($expr->decl->parentEnum) . "." . $this->enumMemberName($expr->decl->name);
        else if ($expr instanceof NullCoalesceExpression)
            $res = $this->expr($expr->defaultExpr) . " or " . $this->expr($expr->exprIfNull);
        else { }
        return $res;
    }
    
    function stmtDefault($stmt) {
        $nl = "\n";
        if ($stmt instanceof BreakStatement)
            return "break";
        else if ($stmt instanceof ReturnStatement)
            return $stmt->expression === null ? "return" : "return " . $this->expr($stmt->expression);
        else if ($stmt instanceof UnsetStatement)
            return "/* unset " . $this->expr($stmt->expression) . "; */";
        else if ($stmt instanceof ThrowStatement)
            return "raise " . $this->expr($stmt->expression);
        else if ($stmt instanceof ExpressionStatement)
            return $this->expr($stmt->expression);
        else if ($stmt instanceof VariableDeclaration)
            return $stmt->initializer !== null ? $this->name_($stmt->name) . " = " . $this->expr($stmt->initializer) : "";
        else if ($stmt instanceof ForeachStatement)
            return "for " . $this->name_($stmt->itemVar->name) . " in " . $this->expr($stmt->items) . ":\n" . $this->block($stmt->body);
        else if ($stmt instanceof IfStatement) {
            $elseIf = $stmt->else_ !== null && count($stmt->else_->statements) === 1 && $stmt->else_->statements[0] instanceof IfStatement;
            return "if " . $this->expr($stmt->condition) . ":\n" . $this->block($stmt->then) . ($elseIf ? "\nel" . $this->stmt($stmt->else_->statements[0]) : "") . (!$elseIf && $stmt->else_ !== null ? "\nelse:\n" . $this->block($stmt->else_) : "");
        }
        else if ($stmt instanceof WhileStatement)
            return "while " . $this->expr($stmt->condition) . ":\n" . $this->block($stmt->body);
        else if ($stmt instanceof ForStatement)
            return ($stmt->itemVar !== null ? $this->var($stmt->itemVar, null) . "\n" : "") . "\nwhile " . $this->expr($stmt->condition) . ":\n" . $this->block($stmt->body) . "\n" . $this->pad($this->expr($stmt->incrementor));
        else if ($stmt instanceof DoStatement)
            return "while True:\n" . $this->block($stmt->body) . "\n" . $this->pad("if not (" . $this->expr($stmt->condition) . "):" . $nl . $this->pad("break"));
        else if ($stmt instanceof TryStatement)
            return "try:\n" . $this->block($stmt->tryBody) . ($stmt->catchBody !== null ? "\nexcept Exception as " . $this->name_($stmt->catchVar->name) . ":\n" . $this->block($stmt->catchBody) : "") . ($stmt->finallyBody !== null ? "\nfinally:\n" . $this->block($stmt->finallyBody) : "");
        else if ($stmt instanceof ContinueStatement)
            return "continue";
        else
            return "UNKNOWN-STATEMENT";
    }
    
    function stmt($stmt) {
        $res = null;
        
        if ($stmt->attributes !== null && array_key_exists("python", $stmt->attributes))
            $res = @$stmt->attributes["python"] ?? null;
        else {
            foreach ($this->plugins as $plugin) {
                $res = $plugin->stmt($stmt);
                if ($res !== null)
                    break;
            }
            
            if ($res === null)
                $res = $this->stmtDefault($stmt);
        }
        
        return $this->leading($stmt) . $res;
    }
    
    function stmts($stmts, $skipPass = false) {
        return $this->pad(count($stmts) === 0 && !$skipPass ? "pass" : implode("\n", array_map(function ($stmt) { return $this->stmt($stmt); }, $stmts)));
    }
    
    function block($block, $skipPass = false) {
        return $this->stmts($block->statements, $skipPass);
    }
    
    function pass($str) {
        return $str === "" ? "pass" : $str;
    }
    
    function cls($cls) {
        if (@$cls->attributes["external"] ?? null === "true")
            return "";
        $this->currentClass = $cls;
        $resList = array();
        $classAttributes = array();
        
        $staticFields = array_values(array_filter($cls->fields, function ($x) { return $x->isStatic; }));
        
        if (count($staticFields) > 0) {
            $this->imports->add("import OneLangStdLib as one");
            $classAttributes[] = "@one.static_init";
            $fieldInits = array_map(function ($f) use ($cls) { return "cls." . $this->vis($f->visibility) . $cls->name.replace($this->var($f, $f), "cls"); }, $staticFields);
            $resList[] = "@classmethod\ndef static_init(cls):\n" . $this->pad(implode("\n", $fieldInits));
        }
        
        $constrStmts = array();
        
        foreach (array_values(array_filter($cls->fields, function ($x) { return !$x->isStatic; })) as $field) {
            $init = $field->constructorParam !== null ? $this->name_($field->constructorParam->name) : ($field->initializer !== null ? $this->expr($field->initializer) : "None");
            $constrStmts[] = "self." . $this->name_($field->name) . " = " . $init;
        }
        
        if ($cls->baseClass !== null) {
            if ($cls->constructor_ !== null && $cls->constructor_->superCallArgs !== null)
                $constrStmts[] = "super().__init__(" . implode(", ", array_map(function ($x) { return $this->expr($x); }, $cls->constructor_->superCallArgs)) . ")";
            else
                $constrStmts[] = "super().__init__()";
        }
        
        if ($cls->constructor_ !== null)
            foreach ($cls->constructor_->body->statements as $stmt)
                $constrStmts[] = $this->stmt($stmt);
        
        $resList[] = "def __init__(self" . ($cls->constructor_ === null ? "" : implode("", array_map(function ($p) { return ", " . $this->var($p, null); }, $cls->constructor_->parameters))) . "):\n" . $this->pad($this->pass(implode("\n", $constrStmts)));
        
        foreach ($cls->properties as $prop) {
            if ($prop->getter !== null)
                $resList[] = "def get_" . $this->name_($prop->name) . "(self):\n" . $this->block($prop->getter);
        }
        
        $methods = array();
        foreach ($cls->methods as $method) {
            if ($method->body === null)
                continue;
            // declaration only
            $methods[] = ($method->isStatic ? "@classmethod\n" : "") . "def " . $this->name_($method->name) . "(" . ($method->isStatic ? "cls" : "self") . implode("", array_map(function ($p) { return ", " . $this->var($p, null); }, $method->parameters)) . "):" . "\n" . $this->block($method->body);
        }
        $resList[] = implode("\n\n", $methods);
        $resList2 = array_values(array_filter($resList, function ($x) { return $x !== ""; }));
        
        $clsHdr = "class " . $this->clsName($cls, true) . ($cls->baseClass !== null ? "(" . $this->clsName(($cls->baseClass)->decl) . ")" : "") . ":\n";
        return implode("", array_map(function ($x) { return $x . "\n"; }, $classAttributes)) . $clsHdr . $this->pad(count($resList2) > 0 ? implode("\n\n", $resList2) : "pass");
    }
    
    function pad($str) {
        return $str === "" ? "" : implode("\n", array_map(function ($x) { return "    " . $x; }, preg_split("/\\n/", $str)));
    }
    
    function calcRelImport($targetPath, $fromPath) {
        $targetParts = preg_split("/\\//", $targetPath->scopeName);
        $fromParts = preg_split("/\\//", $fromPath->scopeName);
        
        $sameLevel = 0;
        while ($sameLevel < count($targetParts) && $sameLevel < count($fromParts) && $targetParts[$sameLevel] === $fromParts[$sameLevel])
            $sameLevel++;
        
        $result = "";
        for ($i = 1; $i < count($fromParts) - $sameLevel; $i++)
            $result .= ".";
        
        for ($i = $sameLevel; $i < count($targetParts); $i++)
            $result .= "." . $targetParts[$i];
        
        return $result;
    }
    
    function calcImportAlias($targetPath) {
        $parts = preg_split("/\\//", $targetPath->scopeName);
        $filename = $parts[count($parts) - 1];
        return NameUtils::shortName($filename);
    }
    
    function genFile($sourceFile) {
        $this->currentFile = $sourceFile;
        $this->imports = new \OneLang\Set();
        $this->importAllScopes = new \OneLang\Set();
        $this->imports->add("from OneLangStdLib import *");
        // TODO: do not add this globally, just for nativeResolver methods
               
        if (count($sourceFile->enums) > 0)
            $this->imports->add("from enum import Enum");
        
        foreach (array_values(array_filter($sourceFile->imports, function ($x) { return !$x->importAll; })) as $import_) {
            if (@$import_->attributes["python-ignore"] ?? null === "true")
                continue;
            
            if (array_key_exists("python-import-all", $import_->attributes)) {
                $this->imports->add("from " . @$import_->attributes["python-import-all"] ?? null . " import *");
                $this->importAllScopes->add($import_->exportScope->getId());
            }
            else {
                $alias = $this->calcImportAlias($import_->exportScope);
                $this->imports->add("import " . $this->package->name . "." . preg_replace("///", ".", $import_->exportScope->scopeName) . " as " . $alias);
            }
        }
        
        $enums = array();
        foreach ($sourceFile->enums as $enum_) {
            $values = array();
            for ($i = 0; $i < count($enum_->values); $i++)
                $values[] = $this->enumMemberName($enum_->values[$i]->name) . " = " . $i + 1;
            $enums[] = "class " . $this->enumName($enum_, true) . "(Enum):\n" . $this->pad(implode("\n", $values));
        }
        
        $classes = array();
        foreach ($sourceFile->classes as $cls)
            $classes[] = $this->cls($cls);
        
        $main = count($sourceFile->mainBlock->statements) > 0 ? $this->block($sourceFile->mainBlock) : "";
        
        $imports = array();
        foreach ($this->imports as $imp)
            $imports[] = $imp;
        
        return implode("\n\n", array_values(array_filter(array(implode("\n", $imports), implode("\n\n", $enums), implode("\n\n", $classes), $main), function ($x) { return $x !== ""; })));
    }
    
    function generate($pkg) {
        $this->package = $pkg;
        $result = array();
        foreach (array_keys($pkg->files) as $path)
            $result[] = new GeneratedFile($pkg->name . "/" . $path, $this->genFile(@$pkg->files[$path] ?? null));
        return $result;
    }
}
