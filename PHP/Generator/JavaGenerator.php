<?php

namespace Generator\JavaGenerator;

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
use One\Ast\Types\Method;
use One\Ast\Types\ExportScopeRef;
use One\Ast\AstTypes\VoidType;
use One\Ast\AstTypes\ClassType;
use One\Ast\AstTypes\InterfaceType;
use One\Ast\AstTypes\EnumType;
use One\Ast\AstTypes\AnyType;
use One\Ast\AstTypes\LambdaType;
use One\Ast\AstTypes\NullType;
use One\Ast\AstTypes\GenericsType;
use One\Ast\AstTypes\TypeHelper;
use One\Ast\AstTypes\IInterfaceType;
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
use Generator\JavaPlugins\JsToJava\JsToJava;
use One\ITransformer\ITransformer;
use One\Transforms\ConvertNullCoalesce\ConvertNullCoalesce;
use One\Transforms\UseDefaultCallArgsExplicitly\UseDefaultCallArgsExplicitly;

class JavaGenerator implements IGenerator {
    public $imports;
    public $currentClass;
    public $reservedWords;
    public $fieldToMethodHack;
    public $plugins;
    
    function __construct() {
        $this->imports = new \OneLang\Set();
        $this->reservedWords = array("class", "interface", "throws", "package", "throw", "boolean");
        $this->fieldToMethodHack = array();
        $this->plugins = array();
        $this->plugins[] = new JsToJava($this);
    }
    
    function getLangName() {
        return "Java";
    }
    
    function getExtension() {
        return "java";
    }
    
    function getTransforms() {
        return array(new ConvertNullCoalesce(), new UseDefaultCallArgsExplicitly());
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
        if ($name === "_")
            $name = "unused";
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
    
    function type($t, $mutates = true, $isNew = false) {
        if ($t instanceof ClassType || $t instanceof InterfaceType) {
            $decl = ($t)->getDecl();
            if ($decl->parentFile->exportScope !== null)
                $this->imports->add($this->toImport($decl->parentFile->exportScope) . "." . $decl->name);
        }
        
        if ($t instanceof ClassType) {
            $typeArgs = $this->typeArgs(array_map(function ($x) { return $this->type($x); }, $t->typeArguments));
            if ($t->decl->name === "TsString")
                return "String";
            else if ($t->decl->name === "TsBoolean")
                return "Boolean";
            else if ($t->decl->name === "TsNumber")
                return "Integer";
            else if ($t->decl->name === "TsArray") {
                $realType = $isNew ? "ArrayList" : "List";
                if ($mutates) {
                    $this->imports->add("java.util." . $realType);
                    return $realType . "<" . $this->type($t->typeArguments[0]) . ">";
                }
                else
                    return $this->type($t->typeArguments[0]) . "[]";
            }
            else if ($t->decl->name === "Map") {
                $realType = $isNew ? "LinkedHashMap" : "Map";
                $this->imports->add("java.util." . $realType);
                return $realType . "<" . $this->type($t->typeArguments[0]) . ", " . $this->type($t->typeArguments[1]) . ">";
            }
            else if ($t->decl->name === "Set") {
                $realType = $isNew ? "LinkedHashSet" : "Set";
                $this->imports->add("java.util." . $realType);
                return $realType . "<" . $this->type($t->typeArguments[0]) . ">";
            }
            else if ($t->decl->name === "Promise")
                return $t->typeArguments[0] instanceof VoidType ? "void" : $this->type($t->typeArguments[0]);
            else if ($t->decl->name === "Object")
                //this.imports.add("System");
                return "Object";
            else if ($t->decl->name === "TsMap") {
                $realType = $isNew ? "LinkedHashMap" : "Map";
                $this->imports->add("java.util." . $realType);
                return $realType . "<String, " . $this->type($t->typeArguments[0]) . ">";
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
            return "Object";
        else if ($t instanceof NullType)
            return "null";
        else if ($t instanceof GenericsType)
            return $t->typeVarName;
        else if ($t instanceof LambdaType) {
            $isFunc = !($t->returnType instanceof VoidType);
            $paramTypes = array_map(function ($x) { return $this->type($x->type); }, $t->parameters);
            if ($isFunc)
                $paramTypes[] = $this->type($t->returnType);
            $this->imports->add("java.util.function." . ($isFunc ? "Function" : "Consumer"));
            return ($isFunc ? "Function" : "Consumer") . "<" . implode(", ", $paramTypes) . ">";
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
    
    function varType($v, $attr) {
        /* @var $type */
        if ($attr !== null && $attr->attributes !== null && array_key_exists("java-type", $attr->attributes))
            $type = @$attr->attributes["java-type"] ?? null;
        else if ($v->type instanceof ClassType && $v->type->decl->name === "TsArray") {
            if ($v->mutability->mutated) {
                $this->imports->add("java.util.List");
                $type = "List<" . $this->type($v->type->typeArguments[0]) . ">";
            }
            else
                $type = $this->type($v->type->typeArguments[0]) . "[]";
        }
        else
            $type = $this->type($v->type);
        return $type;
    }
    
    function varWoInit($v, $attr) {
        return $this->varType($v, $attr) . " " . $this->name_($v->name);
    }
    
    function var($v, $attrs) {
        return $this->varWoInit($v, $attrs) . ($v->initializer !== null ? " = " . $this->expr($v->initializer) : "");
    }
    
    function exprCall($typeArgs, $args) {
        return $this->typeArgs2($typeArgs) . "(" . implode(", ", array_map(function ($x) { return $this->expr($x); }, $args)) . ")";
    }
    
    function mutateArg($arg, $shouldBeMutable) {
        if ($this->isTsArray($arg->actualType)) {
            $itemType = ($arg->actualType)->typeArguments[0];
            if ($arg instanceof ArrayLiteral && !$shouldBeMutable)
                return count($arg->items) === 0 && !$this->isTsArray($itemType) ? "new " . $this->type($itemType) . "[0]" : "new " . $this->type($itemType) . "[] { " . implode(", ", array_map(function ($x) { return $this->expr($x); }, $arg->items)) . " }";
            
            $currentlyMutable = $shouldBeMutable;
            if ($arg instanceof VariableReference)
                $currentlyMutable = $arg->getVariable()->mutability->mutated;
            else if ($arg instanceof InstanceMethodCallExpression || $arg instanceof StaticMethodCallExpression)
                $currentlyMutable = false;
            
            if ($currentlyMutable && !$shouldBeMutable)
                return $this->expr($arg) . ".toArray(" . $this->type($itemType) . "[]::new)";
            else if (!$currentlyMutable && $shouldBeMutable) {
                $this->imports->add("java.util.Arrays");
                $this->imports->add("java.util.ArrayList");
                return "new ArrayList<>(Arrays.asList(" . $this->expr($arg) . "))";
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
        if ($type instanceof ClassType && \OneLang\ArrayHelper::every($type->typeArguments, function ($x, $_) { return $x instanceof ClassType; })) {
            $fullName = implode("", array_map(function ($x) { return ($x)->decl->name; }, $type->typeArguments)) . $type->decl->name;
            return NameUtils::shortName($fullName);
        }
        return null;
    }
    
    function isSetExpr($varRef) {
        return $varRef->parentNode instanceof BinaryExpression && $varRef->parentNode->left === $varRef && in_array($varRef->parentNode->operator, array("=", "+=", "-="));
    }
    
    function expr($expr) {
        foreach ($this->plugins as $plugin) {
            $result = $plugin->expr($expr);
            if ($result !== null)
                return $result;
        }
        
        $res = "UNKNOWN-EXPR";
        if ($expr instanceof NewExpression)
            $res = "new " . $this->type($expr->cls, true, true) . $this->callParams($expr->args, $expr->cls->decl->constructor_ !== null ? $expr->cls->decl->constructor_->parameters : array());
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
            $res = $this->expr($expr->method) . ".apply(" . implode(", ", array_map(function ($x) { return $this->expr($x); }, $expr->args)) . ")";
        else if ($expr instanceof BooleanLiteral)
            $res = ($expr->boolValue ? "true" : "false");
        else if ($expr instanceof StringLiteral)
            $res = json_encode($expr->stringValue, JSON_UNESCAPED_SLASHES);
        else if ($expr instanceof NumericLiteral)
            $res = $expr->valueAsText;
        else if ($expr instanceof CharacterLiteral)
            $res = "'" . $expr->charValue . "'";
        else if ($expr instanceof ElementAccessExpression)
            $res = $this->expr($expr->object) . ".get(" . $this->expr($expr->elementExpr) . ")";
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
            $res = implode(" + ", $parts);
        }
        else if ($expr instanceof BinaryExpression) {
            $modifies = in_array($expr->operator, array("=", "+=", "-="));
            if ($modifies && $expr->left instanceof InstanceFieldReference && $this->useGetterSetter($expr->left))
                $res = $this->expr($expr->left->object) . ".set" . $this->ucFirst($expr->left->field->name) . "(" . $this->mutatedExpr($expr->right, $expr->operator === "=" ? $expr->left : null) . ")";
            else if (in_array($expr->operator, array("==", "!="))) {
                $lit = $this->currentClass->parentFile->literalTypes;
                $leftType = $expr->left->getType();
                $rightType = $expr->right->getType();
                $useEquals = TypeHelper::equals($leftType, $lit->string) && $rightType !== null && TypeHelper::equals($rightType, $lit->string);
                if ($useEquals) {
                    $this->imports->add("OneStd.Objects");
                    $res = ($expr->operator === "!=" ? "!" : "") . "Objects.equals(" . $this->expr($expr->left) . ", " . $this->expr($expr->right) . ")";
                }
                else
                    $res = $this->expr($expr->left) . " " . $expr->operator . " " . $this->expr($expr->right);
            }
            else
                $res = $this->expr($expr->left) . " " . $expr->operator . " " . $this->mutatedExpr($expr->right, $expr->operator === "=" ? $expr->left : null);
        }
        else if ($expr instanceof ArrayLiteral) {
            if (count($expr->items) === 0)
                $res = "new " . $this->type($expr->actualType, true, true) . "()";
            else {
                $this->imports->add("java.util.List");
                $this->imports->add("java.util.ArrayList");
                $res = "new ArrayList<>(List.of(" . implode(", ", array_map(function ($x) { return $this->expr($x); }, $expr->items)) . "))";
            }
        }
        else if ($expr instanceof CastExpression)
            $res = "((" . $this->type($expr->newType) . ")" . $this->expr($expr->expression) . ")";
        else if ($expr instanceof ConditionalExpression)
            $res = $this->expr($expr->condition) . " ? " . $this->expr($expr->whenTrue) . " : " . $this->mutatedExpr($expr->whenFalse, $expr->whenTrue);
        else if ($expr instanceof InstanceOfExpression)
            $res = $this->expr($expr->expr) . " instanceof " . $this->type($expr->checkType);
        else if ($expr instanceof ParenthesizedExpression)
            $res = "(" . $this->expr($expr->expression) . ")";
        else if ($expr instanceof RegexLiteral) {
            $this->imports->add("OneStd.RegExp");
            $res = "new RegExp(" . json_encode($expr->pattern, JSON_UNESCAPED_SLASHES) . ")";
        }
        else if ($expr instanceof Lambda) {
            /* @var $body */
            if (count($expr->body->statements) === 1 && $expr->body->statements[0] instanceof ReturnStatement)
                $body = " " . $this->expr(($expr->body->statements[0])->expression);
            else
                $body = $this->block($expr->body, false);
            
            $params = array_map(function ($x) { return $this->name_($x->name); }, $expr->parameters);
            
            $res = (count($params) === 1 ? $params[0] : "(" . implode(", ", $params) . ")") . " ->" . $body;
        }
        else if ($expr instanceof UnaryExpression && $expr->unaryType === UnaryType::PREFIX)
            $res = $expr->operator . $this->expr($expr->operand);
        else if ($expr instanceof UnaryExpression && $expr->unaryType === UnaryType::POSTFIX)
            $res = $this->expr($expr->operand) . $expr->operator;
        else if ($expr instanceof MapLiteral) {
            if (count($expr->items) > 10)
                throw new \OneLang\Error("MapLiteral is only supported with maximum of 10 items");
            if (count($expr->items) === 0)
                $res = "new " . $this->type($expr->actualType, true, true) . "()";
            else {
                $this->imports->add("java.util.Map");
                $repr = implode(", ", array_map(function ($item) { return json_encode($item->key, JSON_UNESCAPED_SLASHES) . ", " . $this->expr($item->value); }, $expr->items));
                $res = "Map.of(" . $repr . ")";
            }
        }
        else if ($expr instanceof NullLiteral)
            $res = "null";
        else if ($expr instanceof AwaitExpression)
            $res = $this->expr($expr->expr);
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
            $res = "super";
        else if ($expr instanceof StaticFieldReference)
            $res = $this->name_($expr->decl->parentInterface->name) . "." . $this->name_($expr->decl->name);
        else if ($expr instanceof StaticPropertyReference)
            $res = $this->name_($expr->decl->parentClass->name) . "." . $this->name_($expr->decl->name);
        else if ($expr instanceof InstanceFieldReference) {
            // TODO: unified handling of field -> property conversion?
            if ($this->useGetterSetter($expr))
                $res = $this->expr($expr->object) . ".get" . $this->ucFirst($expr->field->name) . "()";
            else
                $res = $this->expr($expr->object) . "." . $this->name_($expr->field->name);
        }
        else if ($expr instanceof InstancePropertyReference)
            $res = $this->expr($expr->object) . "." . ($this->isSetExpr($expr) ? "set" : "get") . $this->ucFirst($expr->property->name) . "()";
        else if ($expr instanceof EnumMemberReference)
            $res = $this->name_($expr->decl->parentEnum->name) . "." . $this->name_($expr->decl->name);
        else if ($expr instanceof NullCoalesceExpression)
            $res = $this->expr($expr->defaultExpr) . " != null ? " . $this->expr($expr->defaultExpr) . " : " . $this->mutatedExpr($expr->exprIfNull, $expr->defaultExpr);
        else { }
        return $res;
    }
    
    function useGetterSetter($fieldRef) {
        return $fieldRef->object->actualType instanceof InterfaceType || ($fieldRef->field->interfaceDeclarations !== null && count($fieldRef->field->interfaceDeclarations) > 0);
    }
    
    function block($block, $allowOneLiner = true) {
        $stmtLen = count($block->statements);
        return $stmtLen === 0 ? " { }" : ($allowOneLiner && $stmtLen === 1 && !($block->statements[0] instanceof IfStatement) && !($block->statements[0] instanceof VariableDeclaration) ? "\n" . $this->pad($this->rawBlock($block)) : " {\n" . $this->pad($this->rawBlock($block)) . "\n}");
    }
    
    function stmtDefault($stmt) {
        $res = "UNKNOWN-STATEMENT";
        if ($stmt instanceof BreakStatement)
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
            $res = "for (var " . $this->name_($stmt->itemVar->name) . " : " . $this->expr($stmt->items) . ")" . $this->block($stmt->body);
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
                //this.imports.add("System");
                $res .= " catch (Exception " . $this->name_($stmt->catchVar->name) . ") " . $this->block($stmt->catchBody, false);
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
        
        if ($stmt->attributes !== null && array_key_exists("java-import", $stmt->attributes))
            $this->imports->add(@$stmt->attributes["java-import"] ?? null);
        
        if ($stmt->attributes !== null && array_key_exists("java", $stmt->attributes))
            $res = @$stmt->attributes["java"] ?? null;
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
    
    function methodGen($prefix, $params, $body) {
        return $prefix . "(" . implode(", ", array_map(function ($p) { return $this->varWoInit($p, $p); }, $params)) . ")" . $body;
    }
    
    function method($method, $isCls) {
        // TODO: final
        $prefix = ($isCls ? $this->vis($method->visibility) . " " : "") . $this->preIf("static ", $method->isStatic) . $this->preIf("/* throws */ ", $method->throws) . (count($method->typeArguments) > 0 ? "<" . implode(", ", $method->typeArguments) . "> " : "") . $this->type($method->returns, false) . " " . $this->name_($method->name);
        
        return $this->methodGen($prefix, $method->parameters, $method->body === null ? ";" : " {\n" . $this->pad($this->stmts($method->body->statements)) . "\n}");
    }
    
    function class($cls) {
        $this->currentClass = $cls;
        $resList = array();
        
        $staticConstructorStmts = array();
        $complexFieldInits = array();
        $fieldReprs = array();
        $propReprs = array();
        foreach ($cls->fields as $field) {
            $isInitializerComplex = $field->initializer !== null && !($field->initializer instanceof StringLiteral) && !($field->initializer instanceof BooleanLiteral) && !($field->initializer instanceof NumericLiteral);
            
            $prefix = $this->vis($field->visibility) . " " . $this->preIf("static ", $field->isStatic);
            if (count($field->interfaceDeclarations) > 0) {
                $varType = $this->varType($field, $field);
                $name = $this->name_($field->name);
                $pname = $this->ucFirst($field->name);
                $setToFalse = TypeHelper::equals($field->type, $this->currentClass->parentFile->literalTypes->boolean);
                $propReprs[] = $varType . " " . $name . ($setToFalse ? " = false" : ($field->initializer !== null ? " = " . $this->expr($field->initializer) : "")) . ";\n" . $prefix . $varType . " get" . $pname . "() { return this." . $name . "; }\n" . $prefix . "void set" . $pname . "(" . $varType . " value) { this." . $name . " = value; }";
            }
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
        $resList[] = implode("\n\n", $propReprs);
        
        foreach ($cls->properties as $prop) {
            $prefix = $this->vis($prop->visibility) . " " . $this->preIf("static ", $prop->isStatic);
            if ($prop->getter !== null)
                $resList[] = $prefix . $this->type($prop->type) . " get" . $this->ucFirst($prop->name) . "()" . $this->block($prop->getter, false);
            
            if ($prop->setter !== null)
                $resList[] = $prefix . "void set" . $this->ucFirst($prop->name) . "(" . $this->type($prop->type) . " value)" . $this->block($prop->setter, false);
        }
        
        if (count($staticConstructorStmts) > 0)
            $resList[] = "static {\n" . $this->pad($this->stmts($staticConstructorStmts)) . "\n}";
        
        if ($cls->constructor_ !== null) {
            $constrFieldInits = array();
            foreach (array_values(array_filter($cls->fields, function ($x) { return $x->constructorParam !== null; })) as $field) {
                $fieldRef = new InstanceFieldReference(new ThisReference($cls), $field);
                $mpRef = new MethodParameterReference($field->constructorParam);
                // TODO: decide what to do with "after-TypeEngine" transformations
                $mpRef->setActualType($field->type, false, false);
                $constrFieldInits[] = new ExpressionStatement(new BinaryExpression($fieldRef, "=", $mpRef));
            }
            
            $superCall = $cls->constructor_->superCallArgs !== null ? "super(" . implode(", ", array_map(function ($x) { return $this->expr($x); }, $cls->constructor_->superCallArgs)) . ");\n" : "";
            
            // TODO: super calls
            $resList[] = $this->methodGen("public " . $this->preIf("/* throws */ ", $cls->constructor_->throws) . $this->name_($cls->name), $cls->constructor_->parameters, "\n{\n" . $this->pad($superCall . $this->stmts(array_merge(array_merge($constrFieldInits, $complexFieldInits), $cls->constructor_->body->statements))) . "\n}");
        }
        else if (count($complexFieldInits) > 0)
            $resList[] = "public " . $this->name_($cls->name) . "()\n{\n" . $this->pad($this->stmts($complexFieldInits)) . "\n}";
        
        $methods = array();
        foreach ($cls->methods as $method) {
            if ($method->body === null)
                continue;
            // declaration only
            $methods[] = $this->method($method, true);
        }
        $resList[] = implode("\n\n", $methods);
        return $this->pad(implode("\n\n", array_values(array_filter($resList, function ($x) { return $x !== ""; }))));
    }
    
    function ucFirst($str) {
        return strtoupper($str[0]) . substr($str, 1);
    }
    
    function interface($intf) {
        $this->currentClass = $intf;
        
        $resList = array();
        foreach ($intf->fields as $field) {
            $varType = $this->varType($field, $field);
            $name = $this->ucFirst($field->name);
            $resList[] = $varType . " get" . $name . "();\nvoid set" . $name . "(" . $varType . " value);";
        }
        
        $resList[] = implode("\n", array_map(function ($method) { return $this->method($method, false); }, $intf->methods));
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
    
    function importsHead() {
        $imports = array();
        foreach ($this->imports->values() as $imp)
            $imports[] = $imp;
        $this->imports = new \OneLang\Set();
        return count($imports) === 0 ? "" : implode("\n", array_map(function ($x) { return "import " . $x . ";"; }, $imports)) . "\n\n";
    }
    
    function toImport($scope) {
        return $scope->scopeName === "index" ? "OneStd" : $scope->packageName . "." . preg_replace("///", ".", preg_replace("/\\.ts$/", "", $scope->scopeName));
    }
    
    function generate($pkg) {
        $result = array();
        foreach (array_keys($pkg->files) as $path) {
            $file = @$pkg->files[$path] ?? null;
            $packagePath = $pkg->name . "/" . preg_replace("/\\.ts$/", "", $file->sourcePath->path);
            $dstDir = "src/main/java/" . $packagePath;
            $packageName = preg_replace("///", ".", $packagePath);
            
            $imports = new \OneLang\Set();
            foreach ($file->imports as $impList) {
                $impPkg = $this->toImport($impList->exportScope);
                foreach ($impList->imports as $imp)
                    $imports->add($impPkg . "." . $imp->name);
            }
            
            $head = "package " . $packageName . ";\n\n" . implode("\n", array_map(function ($x) { return "import " . $x . ";"; }, \OneLang\Array_::from($imports->values()))) . "\n\n";
            
            foreach ($file->enums as $enum_)
                $result[] = new GeneratedFile($dstDir . "/" . $enum_->name . ".java", $head . "public enum " . $this->name_($enum_->name) . " { " . implode(", ", array_map(function ($x) { return $this->name_($x->name); }, $enum_->values)) . " }");
            
            foreach ($file->interfaces as $intf) {
                $res = "public interface " . $this->name_($intf->name) . $this->typeArgs($intf->typeArguments) . $this->preArr(" extends ", array_map(function ($x) { return $this->type($x); }, $intf->baseInterfaces)) . " {\n" . $this->interface($intf) . "\n}";
                $result[] = new GeneratedFile($dstDir . "/" . $intf->name . ".java", $head . $this->importsHead() . $res);
            }
            
            foreach ($file->classes as $cls) {
                $res = "public class " . $this->name_($cls->name) . $this->typeArgs($cls->typeArguments) . ($cls->baseClass !== null ? " extends " . $this->type($cls->baseClass) : "") . $this->preArr(" implements ", array_map(function ($x) { return $this->type($x); }, $cls->baseInterfaces)) . " {\n" . $this->class($cls) . "\n}";
                $result[] = new GeneratedFile($dstDir . "/" . $cls->name . ".java", $head . $this->importsHead() . $res);
            }
        }
        return $result;
    }
}
