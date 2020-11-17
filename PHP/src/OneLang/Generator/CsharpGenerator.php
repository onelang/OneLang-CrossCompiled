<?php

namespace OneLang\Generator\CsharpGenerator;

use OneLang\One\Ast\Expressions\NewExpression;
use OneLang\One\Ast\Expressions\Identifier;
use OneLang\One\Ast\Expressions\TemplateString;
use OneLang\One\Ast\Expressions\ArrayLiteral;
use OneLang\One\Ast\Expressions\CastExpression;
use OneLang\One\Ast\Expressions\BooleanLiteral;
use OneLang\One\Ast\Expressions\StringLiteral;
use OneLang\One\Ast\Expressions\NumericLiteral;
use OneLang\One\Ast\Expressions\CharacterLiteral;
use OneLang\One\Ast\Expressions\PropertyAccessExpression;
use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\Ast\Expressions\ElementAccessExpression;
use OneLang\One\Ast\Expressions\BinaryExpression;
use OneLang\One\Ast\Expressions\UnresolvedCallExpression;
use OneLang\One\Ast\Expressions\ConditionalExpression;
use OneLang\One\Ast\Expressions\InstanceOfExpression;
use OneLang\One\Ast\Expressions\ParenthesizedExpression;
use OneLang\One\Ast\Expressions\RegexLiteral;
use OneLang\One\Ast\Expressions\UnaryExpression;
use OneLang\One\Ast\Expressions\UnaryType;
use OneLang\One\Ast\Expressions\MapLiteral;
use OneLang\One\Ast\Expressions\NullLiteral;
use OneLang\One\Ast\Expressions\AwaitExpression;
use OneLang\One\Ast\Expressions\UnresolvedNewExpression;
use OneLang\One\Ast\Expressions\UnresolvedMethodCallExpression;
use OneLang\One\Ast\Expressions\InstanceMethodCallExpression;
use OneLang\One\Ast\Expressions\NullCoalesceExpression;
use OneLang\One\Ast\Expressions\GlobalFunctionCallExpression;
use OneLang\One\Ast\Expressions\StaticMethodCallExpression;
use OneLang\One\Ast\Expressions\LambdaCallExpression;
use OneLang\One\Ast\Expressions\IMethodCallExpression;
use OneLang\One\Ast\Statements\Statement;
use OneLang\One\Ast\Statements\ReturnStatement;
use OneLang\One\Ast\Statements\UnsetStatement;
use OneLang\One\Ast\Statements\ThrowStatement;
use OneLang\One\Ast\Statements\ExpressionStatement;
use OneLang\One\Ast\Statements\VariableDeclaration;
use OneLang\One\Ast\Statements\BreakStatement;
use OneLang\One\Ast\Statements\ForeachStatement;
use OneLang\One\Ast\Statements\IfStatement;
use OneLang\One\Ast\Statements\WhileStatement;
use OneLang\One\Ast\Statements\ForStatement;
use OneLang\One\Ast\Statements\DoStatement;
use OneLang\One\Ast\Statements\ContinueStatement;
use OneLang\One\Ast\Statements\TryStatement;
use OneLang\One\Ast\Statements\Block;
use OneLang\One\Ast\Types\Class_;
use OneLang\One\Ast\Types\SourceFile;
use OneLang\One\Ast\Types\IVariable;
use OneLang\One\Ast\Types\Lambda;
use OneLang\One\Ast\Types\Interface_;
use OneLang\One\Ast\Types\IInterface;
use OneLang\One\Ast\Types\MethodParameter;
use OneLang\One\Ast\Types\IVariableWithInitializer;
use OneLang\One\Ast\Types\Visibility;
use OneLang\One\Ast\Types\Package;
use OneLang\One\Ast\Types\IHasAttributesAndTrivia;
use OneLang\One\Ast\AstTypes\VoidType;
use OneLang\One\Ast\AstTypes\ClassType;
use OneLang\One\Ast\AstTypes\InterfaceType;
use OneLang\One\Ast\AstTypes\EnumType;
use OneLang\One\Ast\AstTypes\AnyType;
use OneLang\One\Ast\AstTypes\LambdaType;
use OneLang\One\Ast\AstTypes\NullType;
use OneLang\One\Ast\AstTypes\GenericsType;
use OneLang\One\Ast\References\ThisReference;
use OneLang\One\Ast\References\EnumReference;
use OneLang\One\Ast\References\ClassReference;
use OneLang\One\Ast\References\MethodParameterReference;
use OneLang\One\Ast\References\VariableDeclarationReference;
use OneLang\One\Ast\References\ForVariableReference;
use OneLang\One\Ast\References\ForeachVariableReference;
use OneLang\One\Ast\References\SuperReference;
use OneLang\One\Ast\References\StaticFieldReference;
use OneLang\One\Ast\References\StaticPropertyReference;
use OneLang\One\Ast\References\InstanceFieldReference;
use OneLang\One\Ast\References\InstancePropertyReference;
use OneLang\One\Ast\References\EnumMemberReference;
use OneLang\One\Ast\References\CatchVariableReference;
use OneLang\One\Ast\References\GlobalFunctionReference;
use OneLang\One\Ast\References\StaticThisReference;
use OneLang\One\Ast\References\VariableReference;
use OneLang\Generator\GeneratedFile\GeneratedFile;
use OneLang\Generator\NameUtils\NameUtils;
use OneLang\Generator\IGenerator\IGenerator;
use OneLang\One\Ast\Interfaces\IExpression;
use OneLang\One\Ast\Interfaces\IType;
use OneLang\One\ITransformer\ITransformer;

class CsharpGenerator implements IGenerator {
    public $usings;
    public $currentClass;
    public $reservedWords;
    public $fieldToMethodHack;
    public $instanceOfIds;
    
    function __construct()
    {
        $this->reservedWords = array("object", "else", "operator", "class", "enum", "void", "string", "implicit", "Type", "Enum", "params", "using", "throw", "ref", "base", "virtual", "interface", "int", "const");
        $this->fieldToMethodHack = array("length", "size");
        $this->instanceOfIds = Array();
    }
    
    function getLangName() {
        return "CSharp";
    }
    
    function getExtension() {
        return "cs";
    }
    
    function getTransforms() {
        return array();
    }
    
    function name_($name) {
        if (in_array($name, $this->reservedWords))
            $name .= "_";
        if (in_array($name, $this->fieldToMethodHack))
            $name .= "()";
        $nameParts = preg_split("/-/", $name);
        for ($i = 1; $i < count($nameParts); $i++)
            $nameParts[$i] = strtoupper($nameParts[$i][0]) . substr($nameParts[$i], 1);
        $name = implode("", $nameParts);
        return $name;
    }
    
    function leading($item) {
        $result = "";
        if ($item->leadingTrivia !== null && strlen($item->leadingTrivia) > 0)
            $result .= $item->leadingTrivia;
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
    
    function typeArgs($args) {
        return $args !== null && count($args) > 0 ? "<" . implode(", ", $args) . ">" : "";
    }
    
    function typeArgs2($args) {
        return $this->typeArgs(array_map(function ($x) { return $this->type($x); }, $args));
    }
    
    function type($t, $mutates = true) {
        if ($t instanceof ClassType) {
            $typeArgs = $this->typeArgs(array_map(function ($x) { return $this->type($x); }, $t->typeArguments));
            if ($t->decl->name === "TsString")
                return "string";
            else if ($t->decl->name === "TsBoolean")
                return "bool";
            else if ($t->decl->name === "TsNumber")
                return "int";
            else if ($t->decl->name === "TsArray") {
                if ($mutates) {
                    $this->usings->add("System.Collections.Generic");
                    return "List<" . $this->type($t->typeArguments[0]) . ">";
                }
                else
                    return $this->type($t->typeArguments[0]) . "[]";
            }
            else if ($t->decl->name === "Promise") {
                $this->usings->add("System.Threading.Tasks");
                return $t->typeArguments[0] instanceof VoidType ? "Task" : "Task" . $typeArgs;
            }
            else if ($t->decl->name === "Object") {
                $this->usings->add("System");
                return "object";
            }
            else if ($t->decl->name === "TsMap") {
                $this->usings->add("System.Collections.Generic");
                return "Dictionary<string, " . $this->type($t->typeArguments[0]) . ">";
            }
            return $this->name_($t->decl->name) . $typeArgs;
        }
        else if ($t instanceof InterfaceType)
            return $this->name_($t->decl->name) . $this->typeArgs(array_map(function ($x) { return $this->type($x); }, $t->typeArguments));
        else if ($t instanceof VoidType)
            return "void";
        else if ($t instanceof EnumType)
            return $this->name_($t->decl->name);
        else if ($t instanceof AnyType)
            return "object";
        else if ($t instanceof NullType)
            return "null";
        else if ($t instanceof GenericsType)
            return $t->typeVarName;
        else if ($t instanceof LambdaType) {
            $isFunc = !($t->returnType instanceof VoidType);
            $paramTypes = array_map(function ($x) { return $this->type($x->type); }, $t->parameters);
            if ($isFunc)
                $paramTypes[] = $this->type($t->returnType);
            $this->usings->add("System");
            return ($isFunc ? "Func" : "Action") . "<" . implode(", ", $paramTypes) . ">";
        }
        else if ($t === null)
            return "/* TODO */ object";
        else
            return "/* MISSING */";
    }
    
    function isTsArray($type) {
        return $type instanceof ClassType && $type->decl->name === "TsArray";
    }
    
    function vis($v) {
        return $v === Visibility::PRIVATE ? "private" : ($v === Visibility::PROTECTED ? "protected" : ($v === Visibility::PUBLIC ? "public" : "/* TODO: not set */public"));
    }
    
    function varWoInit($v, $attr) {
        /* @var $type */
        if ($attr !== null && $attr->attributes !== null && array_key_exists("csharp-type", $attr->attributes))
            $type = @$attr->attributes["csharp-type"] ?? null;
        else if ($v->type instanceof ClassType && $v->type->decl->name === "TsArray") {
            if ($v->mutability->mutated) {
                $this->usings->add("System.Collections.Generic");
                $type = "List<" . $this->type($v->type->typeArguments[0]) . ">";
            }
            else
                $type = $this->type($v->type->typeArguments[0]) . "[]";
        }
        else
            $type = $this->type($v->type);
        return $type . " " . $this->name_($v->name);
    }
    
    function var($v, $attrs) {
        return $this->varWoInit($v, $attrs) . ($v->initializer !== null ? " = " . $this->expr($v->initializer) : "");
    }
    
    function exprCall($typeArgs, $args) {
        return $this->typeArgs2($typeArgs) . "(" . implode(", ", array_map(function ($x) { return $this->expr($x); }, $args)) . ")";
    }
    
    function mutateArg($arg, $shouldBeMutable) {
        if ($this->isTsArray($arg->actualType)) {
            if ($arg instanceof ArrayLiteral && !$shouldBeMutable) {
                $itemType = ($arg->actualType)->typeArguments[0];
                return count($arg->items) === 0 && !$this->isTsArray($itemType) ? "new " . $this->type($itemType) . "[0]" : "new " . $this->type($itemType) . "[] { " . implode(", ", array_map(function ($x) { return $this->expr($x); }, $arg->items)) . " }";
            }
            
            $currentlyMutable = $shouldBeMutable;
            if ($arg instanceof VariableReference)
                $currentlyMutable = $arg->getVariable()->mutability->mutated;
            else if ($arg instanceof InstanceMethodCallExpression || $arg instanceof StaticMethodCallExpression)
                $currentlyMutable = false;
            
            if ($currentlyMutable && !$shouldBeMutable)
                return $this->expr($arg) . ".ToArray()";
            else if (!$currentlyMutable && $shouldBeMutable) {
                $this->usings->add("System.Linq");
                return $this->expr($arg) . ".ToList()";
            }
        }
        return $this->expr($arg);
    }
    
    function mutatedExpr($expr, $toWhere) {
        if ($toWhere instanceof VariableReference) {
            $v = $toWhere->getVariable();
            if ($this->isTsArray($v->type))
                return $this->mutateArg($expr, $v->mutability->mutated);
        }
        return $this->expr($expr);
    }
    
    function callParams($args, $params) {
        $argReprs = array();
        for ($i = 0; $i < count($args); $i++)
            $argReprs[] = $this->isTsArray($params[$i]->type) ? $this->mutateArg($args[$i], $params[$i]->mutability->mutated) : $this->expr($args[$i]);
        return "(" . implode(", ", $argReprs) . ")";
    }
    
    function methodCall($expr) {
        return $this->name_($expr->method->name) . $this->typeArgs2($expr->typeArgs) . $this->callParams($expr->args, $expr->method->parameters);
    }
    
    function inferExprNameForType($type) {
        if ($type instanceof ClassType && \OneCore\ArrayHelper::every($type->typeArguments, function ($x, $_) { return $x instanceof ClassType; })) {
            $fullName = implode("", array_map(function ($x) { return ($x)->decl->name; }, $type->typeArguments)) . $type->decl->name;
            return NameUtils::shortName($fullName);
        }
        return null;
    }
    
    function expr($expr) {
        $res = "UNKNOWN-EXPR";
        if ($expr instanceof NewExpression)
            $res = "new " . $this->type($expr->cls) . $this->callParams($expr->args, $expr->cls->decl->constructor_ !== null ? $expr->cls->decl->constructor_->parameters : array());
        else if ($expr instanceof UnresolvedNewExpression)
            $res = "/* TODO: UnresolvedNewExpression */ new " . $this->type($expr->cls) . "(" . implode(", ", array_map(function ($x) { return $this->expr($x); }, $expr->args)) . ")";
        else if ($expr instanceof Identifier)
            $res = "/* TODO: Identifier */ " . $expr->text;
        else if ($expr instanceof PropertyAccessExpression)
            $res = "/* TODO: PropertyAccessExpression */ " . $this->expr($expr->object) . "." . $expr->propertyName;
        else if ($expr instanceof UnresolvedCallExpression)
            $res = "/* TODO: UnresolvedCallExpression */ " . $this->expr($expr->func) . $this->exprCall($expr->typeArgs, $expr->args);
        else if ($expr instanceof UnresolvedMethodCallExpression)
            $res = "/* TODO: UnresolvedMethodCallExpression */ " . $this->expr($expr->object) . "." . $expr->methodName . $this->exprCall($expr->typeArgs, $expr->args);
        else if ($expr instanceof InstanceMethodCallExpression)
            $res = $this->expr($expr->object) . "." . $this->methodCall($expr);
        else if ($expr instanceof StaticMethodCallExpression)
            $res = $this->name_($expr->method->parentInterface->name) . "." . $this->methodCall($expr);
        else if ($expr instanceof GlobalFunctionCallExpression)
            $res = "Global." . $this->name_($expr->func->name) . $this->exprCall(array(), $expr->args);
        else if ($expr instanceof LambdaCallExpression)
            $res = $this->expr($expr->method) . "(" . implode(", ", array_map(function ($x) { return $this->expr($x); }, $expr->args)) . ")";
        else if ($expr instanceof BooleanLiteral)
            $res = ($expr->boolValue ? "true" : "false");
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
                // parts.push(part.literalText.replace(new RegExp("\\n"), $"\\n").replace(new RegExp("\\r"), $"\\r").replace(new RegExp("\\t"), $"\\t").replace(new RegExp("{"), "{{").replace(new RegExp("}"), "}}").replace(new RegExp("\""), $"\\\""));
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
                        else if ($chr === "\"")
                            $lit .= "\\\"";
                        else if ($chr === "{")
                            $lit .= "{{";
                        else if ($chr === "}")
                            $lit .= "}}";
                        else {
                            $chrCode = ord($chr[0]);
                            if (32 <= $chrCode && $chrCode <= 126)
                                $lit .= $chr;
                            else
                                throw new \OneCore\Error("invalid char in template string (code=" . $chrCode . ")");
                        }
                    }
                    $parts[] = $lit;
                }
                else {
                    $repr = $this->expr($part->expression);
                    $parts[] = $part->expression instanceof ConditionalExpression ? "{(" . $repr . ")}" : "{" . $repr . "}";
                }
            }
            $res = "$\"" . implode("", $parts) . "\"";
        }
        else if ($expr instanceof BinaryExpression)
            $res = $this->expr($expr->left) . " " . $expr->operator . " " . $this->mutatedExpr($expr->right, $expr->operator === "=" ? $expr->left : null);
        else if ($expr instanceof ArrayLiteral) {
            if (count($expr->items) === 0)
                $res = "new " . $this->type($expr->actualType) . "()";
            else
                $res = "new " . $this->type($expr->actualType) . " { " . implode(", ", array_map(function ($x) { return $this->expr($x); }, $expr->items)) . " }";
        }
        else if ($expr instanceof CastExpression) {
            if ($expr->instanceOfCast !== null && $expr->instanceOfCast->alias !== null)
                $res = $this->name_($expr->instanceOfCast->alias);
            else
                $res = "((" . $this->type($expr->newType) . ")" . $this->expr($expr->expression) . ")";
        }
        else if ($expr instanceof ConditionalExpression)
            $res = $this->expr($expr->condition) . " ? " . $this->expr($expr->whenTrue) . " : " . $this->mutatedExpr($expr->whenFalse, $expr->whenTrue);
        else if ($expr instanceof InstanceOfExpression) {
            if ($expr->implicitCasts !== null && count($expr->implicitCasts) > 0) {
                $aliasPrefix = $this->inferExprNameForType($expr->checkType);
                if ($aliasPrefix === null)
                    $aliasPrefix = $expr->expr instanceof VariableReference ? $expr->expr->getVariable()->name : "obj";
                $id = array_key_exists($aliasPrefix, $this->instanceOfIds) ? @$this->instanceOfIds[$aliasPrefix] ?? null : 1;
                $this->instanceOfIds[$aliasPrefix] = $id + 1;
                $expr->alias = $aliasPrefix . ($id === 1 ? "" : $id);
            }
            $res = $this->expr($expr->expr) . " is " . $this->type($expr->checkType) . ($expr->alias !== null ? " " . $this->name_($expr->alias) : "");
        }
        else if ($expr instanceof ParenthesizedExpression)
            $res = "(" . $this->expr($expr->expression) . ")";
        else if ($expr instanceof RegexLiteral)
            $res = "new RegExp(" . json_encode($expr->pattern, JSON_UNESCAPED_SLASHES) . ")";
        else if ($expr instanceof Lambda) {
            /* @var $body */
            if (count($expr->body->statements) === 1 && $expr->body->statements[0] instanceof ReturnStatement)
                $body = $this->expr(($expr->body->statements[0])->expression);
            else
                $body = "{ " . $this->rawBlock($expr->body) . " }";
            
            $params = array_map(function ($x) { return $this->name_($x->name); }, $expr->parameters);
            
            $res = (count($params) === 1 ? $params[0] : "(" . implode(", ", $params) . ")") . " => " . $body;
        }
        else if ($expr instanceof UnaryExpression && $expr->unaryType === UnaryType::PREFIX)
            $res = $expr->operator . $this->expr($expr->operand);
        else if ($expr instanceof UnaryExpression && $expr->unaryType === UnaryType::POSTFIX)
            $res = $this->expr($expr->operand) . $expr->operator;
        else if ($expr instanceof MapLiteral) {
            $repr = implode(",\n", array_map(function ($item) { return "[" . json_encode($item->key, JSON_UNESCAPED_SLASHES) . "] = " . $this->expr($item->value); }, $expr->items));
            $res = "new " . $this->type($expr->actualType) . " " . ($repr === "" ? "{}" : (strpos($repr, "\n") !== false ? "{\n" . $this->pad($repr) . "\n}" : "{ " . $repr . " }"));
        }
        else if ($expr instanceof NullLiteral)
            $res = "null";
        else if ($expr instanceof AwaitExpression)
            $res = "await " . $this->expr($expr->expr);
        else if ($expr instanceof ThisReference)
            $res = "this";
        else if ($expr instanceof StaticThisReference)
            $res = $this->currentClass->name;
        else if ($expr instanceof EnumReference)
            $res = $this->name_($expr->decl->name);
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
            $res = "base";
        else if ($expr instanceof StaticFieldReference)
            $res = $this->name_($expr->decl->parentInterface->name) . "." . $this->name_($expr->decl->name);
        else if ($expr instanceof StaticPropertyReference)
            $res = $this->name_($expr->decl->parentClass->name) . "." . $this->name_($expr->decl->name);
        else if ($expr instanceof InstanceFieldReference)
            $res = $this->expr($expr->object) . "." . $this->name_($expr->field->name);
        else if ($expr instanceof InstancePropertyReference)
            $res = $this->expr($expr->object) . "." . $this->name_($expr->property->name);
        else if ($expr instanceof EnumMemberReference)
            $res = $this->name_($expr->decl->parentEnum->name) . "." . $this->name_($expr->decl->name);
        else if ($expr instanceof NullCoalesceExpression)
            $res = $this->expr($expr->defaultExpr) . " ?? " . $this->mutatedExpr($expr->exprIfNull, $expr->defaultExpr);
        else { }
        return $res;
    }
    
    function block($block, $allowOneLiner = true) {
        $stmtLen = count($block->statements);
        return $stmtLen === 0 ? " { }" : ($allowOneLiner && $stmtLen === 1 && !($block->statements[0] instanceof IfStatement) ? "\n" . $this->pad($this->rawBlock($block)) : " {\n" . $this->pad($this->rawBlock($block)) . "\n}");
    }
    
    function stmt($stmt) {
        $res = "UNKNOWN-STATEMENT";
        if ($stmt->attributes !== null && array_key_exists("csharp", $stmt->attributes))
            $res = @$stmt->attributes["csharp"] ?? null;
        else if ($stmt instanceof BreakStatement)
            $res = "break;";
        else if ($stmt instanceof ReturnStatement)
            $res = $stmt->expression === null ? "return;" : "return " . $this->mutateArg($stmt->expression, false) . ";";
        else if ($stmt instanceof UnsetStatement)
            $res = "/* unset " . $this->expr($stmt->expression) . "; */";
        else if ($stmt instanceof ThrowStatement)
            $res = "throw " . $this->expr($stmt->expression) . ";";
        else if ($stmt instanceof ExpressionStatement)
            $res = $this->expr($stmt->expression) . ";";
        else if ($stmt instanceof VariableDeclaration) {
            if ($stmt->initializer instanceof NullLiteral)
                $res = $this->type($stmt->type, $stmt->mutability->mutated) . " " . $this->name_($stmt->name) . " = null;";
            else if ($stmt->initializer !== null)
                $res = "var " . $this->name_($stmt->name) . " = " . $this->mutateArg($stmt->initializer, $stmt->mutability->mutated) . ";";
            else
                $res = $this->type($stmt->type) . " " . $this->name_($stmt->name) . ";";
        }
        else if ($stmt instanceof ForeachStatement)
            $res = "foreach (var " . $this->name_($stmt->itemVar->name) . " in " . $this->expr($stmt->items) . ")" . $this->block($stmt->body);
        else if ($stmt instanceof IfStatement) {
            $elseIf = $stmt->else_ !== null && count($stmt->else_->statements) === 1 && $stmt->else_->statements[0] instanceof IfStatement;
            $res = "if (" . $this->expr($stmt->condition) . ")" . $this->block($stmt->then);
            $res .= ($elseIf ? "\nelse " . $this->stmt($stmt->else_->statements[0]) : "") . (!$elseIf && $stmt->else_ !== null ? "\nelse" . $this->block($stmt->else_) : "");
        }
        else if ($stmt instanceof WhileStatement)
            $res = "while (" . $this->expr($stmt->condition) . ")" . $this->block($stmt->body);
        else if ($stmt instanceof ForStatement)
            $res = "for (" . ($stmt->itemVar !== null ? $this->var($stmt->itemVar, null) : "") . "; " . $this->expr($stmt->condition) . "; " . $this->expr($stmt->incrementor) . ")" . $this->block($stmt->body);
        else if ($stmt instanceof DoStatement)
            $res = "do" . $this->block($stmt->body) . " while (" . $this->expr($stmt->condition) . ");";
        else if ($stmt instanceof TryStatement) {
            $res = "try" . $this->block($stmt->tryBody, false);
            if ($stmt->catchBody !== null) {
                $this->usings->add("System");
                $res .= " catch (Exception " . $this->name_($stmt->catchVar->name) . ") " . $this->block($stmt->catchBody, false);
            }
            if ($stmt->finallyBody !== null)
                $res .= "finally" . $this->block($stmt->finallyBody);
        }
        else if ($stmt instanceof ContinueStatement)
            $res = "continue;";
        else { }
        return $this->leading($stmt) . $res;
    }
    
    function stmts($stmts) {
        return implode("\n", array_map(function ($stmt) { return $this->stmt($stmt); }, $stmts));
    }
    
    function rawBlock($block) {
        return $this->stmts($block->statements);
    }
    
    function classLike($cls) {
        $this->currentClass = $cls;
        $resList = array();
        
        $staticConstructorStmts = array();
        $complexFieldInits = array();
        if ($cls instanceof Class_) {
            $fieldReprs = array();
            foreach ($cls->fields as $field) {
                $isInitializerComplex = $field->initializer !== null && !($field->initializer instanceof StringLiteral) && !($field->initializer instanceof BooleanLiteral) && !($field->initializer instanceof NumericLiteral);
                
                $prefix = $this->vis($field->visibility) . " " . $this->preIf("static ", $field->isStatic);
                if (count($field->interfaceDeclarations) > 0)
                    $fieldReprs[] = $prefix . $this->varWoInit($field, $field) . " { get; set; }";
                else if ($isInitializerComplex) {
                    if ($field->isStatic)
                        $staticConstructorStmts[] = new ExpressionStatement(new BinaryExpression(new StaticFieldReference($field), "=", $field->initializer));
                    else
                        $complexFieldInits[] = new ExpressionStatement(new BinaryExpression(new InstanceFieldReference(new ThisReference($cls), $field), "=", $field->initializer));
                    
                    $fieldReprs[] = $prefix . $this->varWoInit($field, $field) . ";";
                }
                else
                    $fieldReprs[] = $prefix . $this->var($field, $field) . ";";
            }
            $resList[] = implode("\n", $fieldReprs);
            
            $resList[] = implode("\n", array_map(function ($prop) { return $this->vis($prop->visibility) . " " . $this->preIf("static ", $prop->isStatic) . $this->varWoInit($prop, $prop) . ($prop->getter !== null ? " {\n    get {\n" . $this->pad($this->block($prop->getter)) . "\n    }\n}" : "") . ($prop->setter !== null ? " {\n    set {\n" . $this->pad($this->block($prop->setter)) . "\n    }\n}" : ""); }, $cls->properties));
            
            if (count($staticConstructorStmts) > 0)
                $resList[] = "static " . $this->name_($cls->name) . "()\n{\n" . $this->pad($this->stmts($staticConstructorStmts)) . "\n}";
            
            if ($cls->constructor_ !== null) {
                $constrFieldInits = array();
                foreach (array_values(array_filter($cls->fields, function ($x) { return $x->constructorParam !== null; })) as $field) {
                    $fieldRef = new InstanceFieldReference(new ThisReference($cls), $field);
                    $mpRef = new MethodParameterReference($field->constructorParam);
                    // TODO: decide what to do with "after-TypeEngine" transformations
                    $mpRef->setActualType($field->type, false, false);
                    $constrFieldInits[] = new ExpressionStatement(new BinaryExpression($fieldRef, "=", $mpRef));
                }
                
                // @java var stmts = Stream.concat(Stream.concat(constrFieldInits.stream(), complexFieldInits.stream()), ((Class)cls).constructor_.getBody().statements.stream()).toArray(Statement[]::new);
                // @java-import java.util.stream.Stream
                $stmts = array_merge(array_merge($constrFieldInits, $complexFieldInits), $cls->constructor_->body->statements);
                $resList[] = "public " . $this->preIf("/* throws */ ", $cls->constructor_->throws) . $this->name_($cls->name) . "(" . implode(", ", array_map(function ($p) { return $this->var($p, $p); }, $cls->constructor_->parameters)) . ")" . ($cls->constructor_->superCallArgs !== null ? ": base(" . implode(", ", array_map(function ($x) { return $this->expr($x); }, $cls->constructor_->superCallArgs)) . ")" : "") . "\n{\n" . $this->pad($this->stmts($stmts)) . "\n}";
            }
            else if (count($complexFieldInits) > 0)
                $resList[] = "public " . $this->name_($cls->name) . "()\n{\n" . $this->pad($this->stmts($complexFieldInits)) . "\n}";
        }
        else if ($cls instanceof Interface_)
            $resList[] = implode("\n", array_map(function ($field) { return $this->varWoInit($field, $field) . " { get; set; }"; }, $cls->fields));
        
        $methods = array();
        foreach ($cls->methods as $method) {
            if ($cls instanceof Class_ && $method->body === null)
                continue;
            // declaration only
            $methods[] = ($method->parentInterface instanceof Interface_ ? "" : $this->vis($method->visibility) . " ") . $this->preIf("static ", $method->isStatic) . $this->preIf("virtual ", $method->overrides === null && count($method->overriddenBy) > 0) . $this->preIf("override ", $method->overrides !== null) . $this->preIf("async ", $method->async) . $this->preIf("/* throws */ ", $method->throws) . $this->type($method->returns, false) . " " . $this->name_($method->name) . $this->typeArgs($method->typeArguments) . "(" . implode(", ", array_map(function ($p) { return $this->var($p, null); }, $method->parameters)) . ")" . ($method->body !== null ? "\n{\n" . $this->pad($this->stmts($method->body->statements)) . "\n}" : ";");
        }
        $resList[] = implode("\n\n", $methods);
        return $this->pad(implode("\n\n", array_values(array_filter($resList, function ($x) { return $x !== ""; }))));
    }
    
    function pad($str) {
        return implode("\n", array_map(function ($x) { return "    " . $x; }, preg_split("/\\n/", $str)));
    }
    
    function pathToNs($path) {
        // Generator/ExprLang/ExprLangAst.ts -> Generator.ExprLang
        $parts = preg_split("/\\//", $path);
        array_pop($parts);
        return implode(".", $parts);
    }
    
    function genFile($sourceFile) {
        $this->instanceOfIds = Array();
        $this->usings = new \OneCore\Set();
        $enums = array_map(function ($enum_) { return "public enum " . $this->name_($enum_->name) . " { " . implode(", ", array_map(function ($x) { return $this->name_($x->name); }, $enum_->values)) . " }"; }, $sourceFile->enums);
        
        $intfs = array_map(function ($intf) { return "public interface " . $this->name_($intf->name) . $this->typeArgs($intf->typeArguments) . $this->preArr(" : ", array_map(function ($x) { return $this->type($x); }, $intf->baseInterfaces)) . " {\n" . $this->classLike($intf) . "\n}"; }, $sourceFile->interfaces);
        
        $classes = array();
        foreach ($sourceFile->classes as $cls) {
            $baseClasses = array();
            if ($cls->baseClass !== null)
                $baseClasses[] = $cls->baseClass;
            foreach ($cls->baseInterfaces as $intf)
                $baseClasses[] = $intf;
            $classes[] = "public class " . $this->name_($cls->name) . $this->typeArgs($cls->typeArguments) . $this->preArr(" : ", array_map(function ($x) { return $this->type($x); }, $baseClasses)) . " {\n" . $this->classLike($cls) . "\n}";
        }
        
        $main = count($sourceFile->mainBlock->statements) > 0 ? "public class Program\n{\n    static void Main(string[] args)\n    {\n" . $this->pad($this->rawBlock($sourceFile->mainBlock)) . "\n    }\n}" : "";
        
        // @java var usingsSet = new LinkedHashSet<String>(Arrays.stream(sourceFile.imports).map(x -> this.pathToNs(x.exportScope.scopeName)).filter(x -> x != "").collect(Collectors.toList()));
        // @java-import java.util.LinkedHashSet
        $usingsSet = new \OneCore\Set(array_values(array_filter(array_map(function ($x) { return $this->pathToNs($x->exportScope->scopeName); }, $sourceFile->imports), function ($x) { return $x !== ""; })));
        foreach ($this->usings->values() as $using)
            $usingsSet->add($using);
        
        $usings = array();
        foreach ($usingsSet->values() as $using)
            $usings[] = "using " . $using . ";";
        sort($usings);
        
        $result = implode("\n\n", array_values(array_filter(array(implode("\n", $enums), implode("\n\n", $intfs), implode("\n\n", $classes), $main), function ($x) { return $x !== ""; })));
        $nl = "\n";
        // Python fix
        $result = implode($nl, $usings) . "\n\nnamespace " . $this->pathToNs($sourceFile->sourcePath->path) . "\n{\n" . $this->pad($result) . "\n}";
        return $result;
    }
    
    function generate($pkg) {
        $result = array();
        foreach (array_keys($pkg->files) as $path)
            $result[] = new GeneratedFile($path . ".cs", $this->genFile(@$pkg->files[$path] ?? null));
        return $result;
    }
}
