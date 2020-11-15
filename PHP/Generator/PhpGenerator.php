<?php

namespace Generator\PhpGenerator;

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
use One\Ast\Statements\TryStatement;
use One\Ast\Statements\Block;
use One\Ast\Types\Class_;
use One\Ast\Types\SourceFile;
use One\Ast\Types\IVariable;
use One\Ast\Types\Lambda;
use One\Ast\Types\Interface_;
use One\Ast\Types\IInterface;
use One\Ast\Types\MethodParameter;
use One\Ast\Types\IVariableWithInitializer;
use One\Ast\Types\Visibility;
use One\Ast\Types\Package;
use One\Ast\Types\IHasAttributesAndTrivia;
use One\Ast\Types\Enum;
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
use One\Ast\References\VariableReference;
use Generator\GeneratedFile\GeneratedFile;
use Generator\NameUtils\NameUtils;
use Generator\IGenerator\IGenerator;
use One\Ast\Interfaces\IExpression;
use One\Ast\Interfaces\IType;
use Generator\IGeneratorPlugin\IGeneratorPlugin;
use Generator\PhpPlugins\JsToPhp\JsToPhp;
use One\ITransformer\ITransformer;

class PhpGenerator implements IGenerator {
    public $usings;
    public $currentClass;
    public $reservedWords;
    public $fieldToMethodHack;
    public $plugins;
    
    function __construct() {
        $this->reservedWords = array("Generator", "Array", "List", "Interface", "Class");
        $this->fieldToMethodHack = array("length");
        $this->plugins = array();
        $this->plugins[] = new JsToPhp($this);
    }
    
    function getLangName() {
        return "PHP";
    }
    
    function getExtension() {
        return "php";
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
            //const typeArgs = this.typeArgs(t.typeArguments.map(x => this.type(x)));
            if ($t->decl->name === "TsString")
                return "string";
            else if ($t->decl->name === "TsBoolean")
                return "bool";
            else if ($t->decl->name === "TsNumber")
                return "int";
            else if ($t->decl->name === "TsArray") {
                if ($mutates)
                    return "List_";
                else
                    return $this->type($t->typeArguments[0]) . "[]";
            }
            else if ($t->decl->name === "Promise")
                return $this->type($t->typeArguments[0]);
            else if ($t->decl->name === "Object")
                //this.usings.add("System");
                return "object";
            else if ($t->decl->name === "TsMap")
                return "Dictionary";
            
            if ($t->decl->parentFile->exportScope === null)
                return "\\OneLang\\" . $this->name_($t->decl->name);
            else
                return $this->name_($t->decl->name);
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
    
    function vis($v, $isProperty) {
        return $v === Visibility::PRIVATE ? "private " : ($v === Visibility::PROTECTED ? "protected " : ($v === Visibility::PUBLIC ? ($isProperty ? "public " : "") : "/* TODO: not set */" . ($isProperty ? "public " : "")));
    }
    
    function varWoInit($v, $attr) {
        // let type: string;
        // if (attr !== null && attr.attributes !== null && "php-type" in attr.attributes)
        //     type = attr.attributes["php-type"];
        // else if (v.type instanceof ClassType && v.type.decl.name === "TsArray") {
        //     if (v.mutability.mutated) {
        //         type = `List<${this.type(v.type.typeArguments[0])}>`;
        //     } else {
        //         type = `${this.type(v.type.typeArguments[0])}[]`;
        //     }
        // } else {
        //     type = this.type(v.type);
        // }
        return "$" . $this->name_($v->name);
    }
    
    function var($v, $attrs) {
        return $this->varWoInit($v, $attrs) . ($v->initializer !== null ? " = " . $this->expr($v->initializer) : "");
    }
    
    function exprCall($typeArgs, $args) {
        return $this->typeArgs2($typeArgs) . "(" . implode(", ", array_map(function ($x) { return $this->expr($x); }, $args)) . ")";
    }
    
    function mutateArg($arg, $shouldBeMutable) {
        // if (this.isTsArray(arg.actualType)) {
        //     if (arg instanceof ArrayLiteral && !shouldBeMutable) {
        //         return `Array(${arg.items.map(x => this.expr(x)).join(', ')})`;
        //     }
        
        //     let currentlyMutable = shouldBeMutable;
        //     if (arg instanceof VariableReference)
        //         currentlyMutable = arg.getVariable().mutability.mutated;
        //     else if (arg instanceof InstanceMethodCallExpression || arg instanceof StaticMethodCallExpression)
        //         currentlyMutable = false;
            
        //     if (currentlyMutable && !shouldBeMutable)
        //         return `${this.expr(arg)}.ToArray()`;
        //     else if (!currentlyMutable && shouldBeMutable) {
        //         return `${this.expr(arg)}.ToList()`;
        //     }
        // }
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
        if ($type instanceof ClassType && \OneLang\ArrayHelper::every($type->typeArguments, function ($x, $_) { return $x instanceof ClassType; })) {
            $fullName = implode("", array_map(function ($x) { return ($x)->decl->name; }, $type->typeArguments)) . $type->decl->name;
            return NameUtils::shortName($fullName);
        }
        return null;
    }
    
    function expr($expr) {
        foreach ($this->plugins as $plugin) {
            $result = $plugin->expr($expr);
            if ($result !== null)
                return $result;
        }
        
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
            $res = "/* TODO: UnresolvedMethodCallExpression */ " . $this->expr($expr->object) . "->" . $expr->methodName . $this->exprCall($expr->typeArgs, $expr->args);
        else if ($expr instanceof InstanceMethodCallExpression) {
            if ($expr->object instanceof SuperReference)
                $res = "parent::" . $this->methodCall($expr);
            else if ($expr->object instanceof NewExpression)
                $res = "(" . $this->expr($expr->object) . ")->" . $this->methodCall($expr);
            else
                $res = $this->expr($expr->object) . "->" . $this->methodCall($expr);
        }
        else if ($expr instanceof StaticMethodCallExpression) {
            $res = $this->name_($expr->method->parentInterface->name) . "::" . $this->methodCall($expr);
            if ($expr->method->parentInterface->parentFile->exportScope === null)
                $res = "\\OneLang\\" . $res;
        }
        else if ($expr instanceof GlobalFunctionCallExpression)
            $res = "Global." . $this->name_($expr->func->name) . $this->exprCall(array(), $expr->args);
        else if ($expr instanceof LambdaCallExpression)
            $res = $this->expr($expr->method) . "(" . implode(", ", array_map(function ($x) { return $this->expr($x); }, $expr->args)) . ")";
        else if ($expr instanceof BooleanLiteral)
            $res = ($expr->boolValue ? "true" : "false");
        else if ($expr instanceof StringLiteral)
            $res = preg_replace("/\\$/", "\\\$", json_encode($expr->stringValue, JSON_UNESCAPED_SLASHES));
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
                        else if ($chr === "\"")
                            $lit .= "\\\"";
                        else {
                            $chrCode = ord($chr[0]);
                            if (32 <= $chrCode && $chrCode <= 126)
                                $lit .= $chr;
                            else
                                throw new \OneLang\Error("invalid char in template string (code=" . $chrCode . ")");
                        }
                    }
                    $parts[] = "\"" . $lit . "\"";
                }
                else {
                    $repr = $this->expr($part->expression);
                    $parts[] = $part->expression instanceof ConditionalExpression ? "(" . $repr . ")" : $repr;
                }
            }
            $res = implode(" . ", $parts);
        }
        else if ($expr instanceof BinaryExpression) {
            $op = $expr->operator;
            if ($op === "==")
                $op = "===";
            else if ($op === "!=")
                $op = "!==";
            
            if ($expr->left->actualType !== null && $expr->left->actualType->repr() === "C:TsString") {
                if ($op === "+")
                    $op = ".";
                else if ($op === "+=")
                    $op = ".=";
            }
            
            // const useParen = expr.left instanceof BinaryExpression && expr.left.operator !== expr.operator;
            // const leftExpr = this.expr(expr.left);
            
            $res = $this->expr($expr->left) . " " . $op . " " . $this->mutatedExpr($expr->right, $expr->operator === "=" ? $expr->left : null);
        }
        else if ($expr instanceof ArrayLiteral)
            $res = "array(" . implode(", ", array_map(function ($x) { return $this->expr($x); }, $expr->items)) . ")";
        else if ($expr instanceof CastExpression)
            $res = $this->expr($expr->expression);
        else if ($expr instanceof ConditionalExpression) {
            $whenFalseExpr = $this->expr($expr->whenFalse);
            if ($expr->whenFalse instanceof ConditionalExpression)
                $whenFalseExpr = "(" . $whenFalseExpr . ")";
            $res = $this->expr($expr->condition) . " ? " . $this->expr($expr->whenTrue) . " : " . $whenFalseExpr;
        }
        else if ($expr instanceof InstanceOfExpression)
            $res = $this->expr($expr->expr) . " instanceof " . $this->type($expr->checkType);
        else if ($expr instanceof ParenthesizedExpression)
            $res = "(" . $this->expr($expr->expression) . ")";
        else if ($expr instanceof RegexLiteral)
            $res = "new \\OneLang\\RegExp(" . json_encode($expr->pattern, JSON_UNESCAPED_SLASHES) . ")";
        else if ($expr instanceof Lambda) {
            $params = array_map(function ($x) { return "$" . $this->name_($x->name); }, $expr->parameters);
            // TODO: captures should not be null
            $uses = $expr->captures !== null && count($expr->captures) > 0 ? " use (" . implode(", ", array_map(function ($x) { return "$" . $x->name; }, $expr->captures)) . ")" : "";
            $res = "function (" . implode(", ", $params) . ")" . $uses . " { " . $this->rawBlock($expr->body) . " }";
        }
        else if ($expr instanceof UnaryExpression && $expr->unaryType === UnaryType::PREFIX)
            $res = $expr->operator . $this->expr($expr->operand);
        else if ($expr instanceof UnaryExpression && $expr->unaryType === UnaryType::POSTFIX)
            $res = $this->expr($expr->operand) . $expr->operator;
        else if ($expr instanceof MapLiteral) {
            $repr = implode(",\n", array_map(function ($item) { return json_encode($item->key, JSON_UNESCAPED_SLASHES) . " => " . $this->expr($item->value); }, $expr->items));
            $res = "Array(" . ($repr === "" ? "" : (strpos($repr, "\n") !== false ? "\n" . $this->pad($repr) . "\n" : "(" . $repr)) . ")";
        }
        else if ($expr instanceof NullLiteral)
            $res = "null";
        else if ($expr instanceof AwaitExpression)
            $res = $this->expr($expr->expr);
        else if ($expr instanceof ThisReference)
            $res = "$this";
        else if ($expr instanceof StaticThisReference)
            $res = $this->currentClass->name;
        else if ($expr instanceof EnumReference)
            $res = $this->name_($expr->decl->name);
        else if ($expr instanceof ClassReference)
            $res = $this->name_($expr->decl->name);
        else if ($expr instanceof MethodParameterReference)
            $res = "$" . $this->name_($expr->decl->name);
        else if ($expr instanceof VariableDeclarationReference)
            $res = "$" . $this->name_($expr->decl->name);
        else if ($expr instanceof ForVariableReference)
            $res = "$" . $this->name_($expr->decl->name);
        else if ($expr instanceof ForeachVariableReference)
            $res = "$" . $this->name_($expr->decl->name);
        else if ($expr instanceof CatchVariableReference)
            $res = "$" . $this->name_($expr->decl->name);
        else if ($expr instanceof GlobalFunctionReference)
            $res = $this->name_($expr->decl->name);
        else if ($expr instanceof SuperReference)
            $res = "parent";
        else if ($expr instanceof StaticFieldReference)
            $res = $this->name_($expr->decl->parentInterface->name) . "::$" . $this->name_($expr->decl->name);
        else if ($expr instanceof StaticPropertyReference)
            $res = $this->name_($expr->decl->parentClass->name) . "::get_" . $this->name_($expr->decl->name) . "()";
        else if ($expr instanceof InstanceFieldReference)
            $res = $this->expr($expr->object) . "->" . $this->name_($expr->field->name);
        else if ($expr instanceof InstancePropertyReference)
            $res = $this->expr($expr->object) . "->get_" . $this->name_($expr->property->name) . "()";
        else if ($expr instanceof EnumMemberReference)
            $res = $this->name_($expr->decl->parentEnum->name) . "::" . $this->enumMemberName($expr->decl->name);
        else if ($expr instanceof NullCoalesceExpression)
            $res = $this->expr($expr->defaultExpr) . " ?? " . $this->mutatedExpr($expr->exprIfNull, $expr->defaultExpr);
        else { }
        return $res;
    }
    
    function block($block, $allowOneLiner = true) {
        $stmtLen = count($block->statements);
        return $stmtLen === 0 ? " { }" : ($allowOneLiner && $stmtLen === 1 && !($block->statements[0] instanceof IfStatement) ? "\n" . $this->pad($this->rawBlock($block)) : " {\n" . $this->pad($this->rawBlock($block)) . "\n}");
    }
    
    function stmtDefault($stmt) {
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
                $res = "$" . $this->name_($stmt->name) . " = null;";
            else if ($stmt->initializer !== null)
                $res = "$" . $this->name_($stmt->name) . " = " . $this->mutateArg($stmt->initializer, $stmt->mutability->mutated) . ";";
            else
                $res = "/* @var $" . $this->name_($stmt->name) . " */";
        }
        else if ($stmt instanceof ForeachStatement)
            $res = "foreach (" . $this->expr($stmt->items) . " as $" . $this->name_($stmt->itemVar->name) . ")" . $this->block($stmt->body);
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
            if ($stmt->catchBody !== null)
                //                this.usings.add("System");
                $res .= " catch (Exception $" . $this->name_($stmt->catchVar->name) . ")" . $this->block($stmt->catchBody, false);
            if ($stmt->finallyBody !== null)
                $res .= "finally" . $this->block($stmt->finallyBody);
        }
        else if ($stmt instanceof ContinueStatement)
            $res = "continue;";
        else { }
        return $res;
    }
    
    function stmt($stmt) {
        $res = null;
        
        if ($stmt->attributes !== null && array_key_exists("php", $stmt->attributes))
            $res = @$stmt->attributes["php"] ?? null;
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
                
                $prefix = $this->vis($field->visibility, true) . $this->preIf("static ", $field->isStatic);
                if (count($field->interfaceDeclarations) > 0)
                    $fieldReprs[] = $prefix . $this->varWoInit($field, $field) . ";";
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
            
            foreach ($cls->properties as $prop) {
                if ($prop->getter !== null)
                    $resList[] = $this->vis($prop->visibility, false) . $this->preIf("static ", $prop->isStatic) . "function get_" . $this->name_($prop->name) . "()" . $this->block($prop->getter, false);
                if ($prop->setter !== null)
                    $resList[] = $this->vis($prop->visibility, false) . $this->preIf("static ", $prop->isStatic) . "function set_" . $this->name_($prop->name) . "($value)" . $this->block($prop->setter, false);
            }
            
            if (count($staticConstructorStmts) > 0)
                $resList[] = "static function StaticInit()\n{\n" . $this->pad($this->stmts($staticConstructorStmts)) . "\n}";
            
            if ($cls->constructor_ !== null) {
                $constrFieldInits = array();
                foreach (array_values(array_filter($cls->fields, function ($x) { return $x->constructorParam !== null; })) as $field) {
                    $fieldRef = new InstanceFieldReference(new ThisReference($cls), $field);
                    $mpRef = new MethodParameterReference($field->constructorParam);
                    // TODO: decide what to do with "after-TypeEngine" transformations
                    $mpRef->setActualType($field->type, false, false);
                    $constrFieldInits[] = new ExpressionStatement(new BinaryExpression($fieldRef, "=", $mpRef));
                }
                
                $parentCall = $cls->constructor_->superCallArgs !== null ? "parent::__construct(" . implode(", ", array_map(function ($x) { return $this->expr($x); }, $cls->constructor_->superCallArgs)) . ");\n" : "";
                
                $resList[] = $this->preIf("/* throws */ ", $cls->constructor_->throws) . "function __construct" . "(" . implode(", ", array_map(function ($p) { return $this->var($p, $p); }, $cls->constructor_->parameters)) . ")" . " {\n" . $this->pad($parentCall . $this->stmts(array_merge(array_merge($constrFieldInits, $complexFieldInits), $cls->constructor_->body->statements))) . "\n}";
            }
            else if (count($complexFieldInits) > 0)
                $resList[] = "function __construct()\n{\n" . $this->pad($this->stmts($complexFieldInits)) . "\n}";
        }
        else if ($cls instanceof Interface_) { }
        
        $methods = array();
        foreach ($cls->methods as $method) {
            if ($cls instanceof Class_ && $method->body === null)
                continue;
            // declaration only
            $methods[] = ($method->parentInterface instanceof Interface_ ? "" : $this->vis($method->visibility, false)) . $this->preIf("static ", $method->isStatic) . $this->preIf("/* throws */ ", $method->throws) . "function " . $this->name_($method->name) . $this->typeArgs($method->typeArguments) . "(" . implode(", ", array_map(function ($p) { return $this->var($p, null); }, $method->parameters)) . ")" . ($method->body !== null ? " {\n" . $this->pad($this->stmts($method->body->statements)) . "\n}" : ";");
        }
        $resList[] = implode("\n\n", $methods);
        return " {\n" . $this->pad(implode("\n\n", array_values(array_filter($resList, function ($x) { return $x !== ""; })))) . "\n}" . (count($staticConstructorStmts) > 0 ? "\n" . $this->name_($cls->name) . "::StaticInit();" : "");
    }
    
    function pad($str) {
        return implode("\n", array_map(function ($x) { return "    " . $x; }, preg_split("/\\n/", $str)));
    }
    
    function pathToNs($path) {
        // Generator/ExprLang/ExprLangAst.ts -> Generator\ExprLang\ExprLangAst
        $parts = preg_split("/\\//", preg_replace("/\\.ts/", "", $path));
        //parts.pop();
        return implode("\\", $parts);
    }
    
    function enumName($enum_, $isDecl = false) {
        return $enum_->name;
    }
    
    function enumMemberName($name) {
        return strtoupper($this->name_($name));
    }
    
    function genFile($sourceFile) {
        $this->usings = new \OneLang\Set();
        
        $enums = array();
        foreach ($sourceFile->enums as $enum_) {
            $values = array();
            for ($i = 0; $i < count($enum_->values); $i++)
                $values[] = "const " . $this->enumMemberName($enum_->values[$i]->name) . " = " . $i + 1 . ";";
            $enums[] = "class " . $this->enumName($enum_, true) . " {\n" . $this->pad(implode("\n", $values)) . "\n}";
        }
        
        $intfs = array_map(function ($intf) { return "interface " . $this->name_($intf->name) . $this->typeArgs($intf->typeArguments) . $this->preArr(" extends ", array_map(function ($x) { return $this->type($x); }, $intf->baseInterfaces)) . $this->classLike($intf); }, $sourceFile->interfaces);
        
        $classes = array();
        foreach ($sourceFile->classes as $cls)
            $classes[] = "class " . $this->name_($cls->name) . ($cls->baseClass !== null ? " extends " . $this->type($cls->baseClass) : "") . $this->preArr(" implements ", array_map(function ($x) { return $this->type($x); }, $cls->baseInterfaces)) . $this->classLike($cls);
        
        $main = $this->rawBlock($sourceFile->mainBlock);
        
        $usingsSet = new \OneLang\Set();
        foreach ($sourceFile->imports as $imp) {
            if (array_key_exists("php-use", $imp->attributes))
                $usingsSet->add(@$imp->attributes["php-use"] ?? null);
            else {
                $fileNs = $this->pathToNs($imp->exportScope->scopeName);
                if ($fileNs === "index")
                    continue;
                foreach ($imp->imports as $impItem)
                    $usingsSet->add($fileNs . "\\" . $this->name_($impItem->name));
            }
        }
        
        foreach ($this->usings as $using)
            $usingsSet->add($using);
        
        $usings = array();
        foreach ($usingsSet as $using)
            $usings[] = "use " . $using . ";";
        
        $result = implode("\n\n", array_values(array_filter(array(implode("\n", $usings), implode("\n", $enums), implode("\n\n", $intfs), implode("\n\n", $classes), $main), function ($x) { return $x !== ""; })));
        $nl = "\n";
        $result = "<?php\n\nnamespace " . $this->pathToNs($sourceFile->sourcePath->path) . ";\n\n" . $result . "\n";
        return $result;
    }
    
    function generate($pkg) {
        $result = array();
        foreach (array_keys($pkg->files) as $path)
            $result[] = new GeneratedFile($path, $this->genFile(@$pkg->files[$path] ?? null));
        return $result;
    }
}
