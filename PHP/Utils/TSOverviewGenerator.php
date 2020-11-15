<?php

namespace Utils\TSOverviewGenerator;

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
use One\Ast\Types\Method;
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
use One\Ast\Types\IHasAttributesAndTrivia;
use One\Ast\AstTypes\VoidType;
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
use One\Ast\Interfaces\IExpression;
use One\Ast\Interfaces\IType;

class TSOverviewGenerator {
    public static $preview;
    public $previewOnly;
    public $showTypes;
    
    static function StaticInit()
    {
        TSOverviewGenerator::$preview = new TSOverviewGenerator(true);
    }
    
    function __construct($previewOnly = false, $showTypes = false) {
        $this->previewOnly = $previewOnly;
        $this->showTypes = $showTypes;
    }
    
    function leading($item) {
        $result = "";
        if ($item->leadingTrivia !== null && strlen($item->leadingTrivia) > 0)
            $result .= $item->leadingTrivia;
        if ($item->attributes !== null)
            $result .= implode("", array_map(function ($x) use ($item) { return "/// {ATTR} name=\"" . $x . "\", value=" . json_encode(@$item->attributes[$x] ?? null, JSON_UNESCAPED_SLASHES) . "\n"; }, array_keys($item->attributes)));
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
    
    function type($t, $raw = false) {
        $repr = $t === null ? "???" : $t->repr();
        if ($repr === "U:UNKNOWN") { }
        return ($raw ? "" : "{T}") . $repr;
    }
    
    function var($v) {
        $result = "";
        $isProp = $v instanceof Property;
        if ($v instanceof Field || $v instanceof Property) {
            $m = $v;
            $result .= $this->preIf("", $m->isStatic);
            $result .= $m->visibility === Visibility::PRIVATE ? "private " : ($m->visibility === Visibility::PROTECTED ? "protected " : ($m->visibility === Visibility::PUBLIC ? "public " : "VISIBILITY-NOT-SET"));
        }
        $result .= ($isProp ? "@prop " : "");
        if ($v->mutability !== null) {
            $result .= ($v->mutability->unused ? "@unused " : "");
            $result .= ($v->mutability->mutated ? "@mutated " : "");
            $result .= ($v->mutability->reassigned ? "@reass " : "");
        }
        $result .= $v->name . ($isProp ? "()" : "") . ": " . $this->type($v->type);
        if ($v instanceof VariableDeclaration || $v instanceof ForVariable || $v instanceof Field || $v instanceof MethodParameter) {
            $init = ($v)->initializer;
            if ($init !== null)
                $result .= $this->pre(" = ", $this->expr($init));
        }
        return $result;
    }
    
    function expr($expr) {
        $res = "UNKNOWN-EXPR";
        if ($expr instanceof NewExpression)
            $res = "new " . $this->type($expr->cls) . "(" . ($this->previewOnly ? "..." : implode(", ", array_map(function ($x) { return $this->expr($x); }, $expr->args))) . ")";
        else if ($expr instanceof UnresolvedNewExpression)
            $res = "new " . $this->type($expr->cls) . "(" . ($this->previewOnly ? "..." : implode(", ", array_map(function ($x) { return $this->expr($x); }, $expr->args))) . ")";
        else if ($expr instanceof Identifier)
            $res = "{ID}" . $expr->text;
        else if ($expr instanceof PropertyAccessExpression)
            $res = $this->expr($expr->object) . ".{PA}" . $expr->propertyName;
        else if ($expr instanceof UnresolvedCallExpression) {
            $typeArgs = count($expr->typeArgs) > 0 ? "<" . implode(", ", array_map(function ($x) { return $this->type($x); }, $expr->typeArgs)) . ">" : "";
            $res = $this->expr($expr->func) . $typeArgs . "(" . ($this->previewOnly ? "..." : implode(", ", array_map(function ($x) { return $this->expr($x); }, $expr->args))) . ")";
        }
        else if ($expr instanceof UnresolvedMethodCallExpression) {
            $typeArgs = count($expr->typeArgs) > 0 ? "<" . implode(", ", array_map(function ($x) { return $this->type($x); }, $expr->typeArgs)) . ">" : "";
            $res = $this->expr($expr->object) . ".{UM}" . $expr->methodName . $typeArgs . "(" . ($this->previewOnly ? "..." : implode(", ", array_map(function ($x) { return $this->expr($x); }, $expr->args))) . ")";
        }
        else if ($expr instanceof InstanceMethodCallExpression) {
            $typeArgs = count($expr->typeArgs) > 0 ? "<" . implode(", ", array_map(function ($x) { return $this->type($x); }, $expr->typeArgs)) . ">" : "";
            $res = $this->expr($expr->object) . ".{M}" . $expr->method->name . $typeArgs . "(" . ($this->previewOnly ? "..." : implode(", ", array_map(function ($x) { return $this->expr($x); }, $expr->args))) . ")";
        }
        else if ($expr instanceof StaticMethodCallExpression) {
            $typeArgs = count($expr->typeArgs) > 0 ? "<" . implode(", ", array_map(function ($x) { return $this->type($x); }, $expr->typeArgs)) . ">" : "";
            $res = $expr->method->parentInterface->name . ".{M}" . $expr->method->name . $typeArgs . "(" . ($this->previewOnly ? "..." : implode(", ", array_map(function ($x) { return $this->expr($x); }, $expr->args))) . ")";
        }
        else if ($expr instanceof GlobalFunctionCallExpression)
            $res = $expr->func->name . "(" . ($this->previewOnly ? "..." : implode(", ", array_map(function ($x) { return $this->expr($x); }, $expr->args))) . ")";
        else if ($expr instanceof LambdaCallExpression)
            $res = $this->expr($expr->method) . "(" . ($this->previewOnly ? "..." : implode(", ", array_map(function ($x) { return $this->expr($x); }, $expr->args))) . ")";
        else if ($expr instanceof BooleanLiteral)
            $res = ($expr->boolValue ? "true" : "false");
        else if ($expr instanceof StringLiteral)
            $res = json_encode($expr->stringValue, JSON_UNESCAPED_SLASHES);
        else if ($expr instanceof NumericLiteral)
            $res = $expr->valueAsText;
        else if ($expr instanceof CharacterLiteral)
            $res = "'" . $expr->charValue . "'";
        else if ($expr instanceof ElementAccessExpression)
            $res = "(" . $this->expr($expr->object) . ")[" . $this->expr($expr->elementExpr) . "]";
        else if ($expr instanceof TemplateString)
            $res = "`" . implode("", array_map(function ($x) { return $x->isLiteral ? $x->literalText : "\${" . $this->expr($x->expression) . "}"; }, $expr->parts)) . "`";
        else if ($expr instanceof BinaryExpression)
            $res = $this->expr($expr->left) . " " . $expr->operator . " " . $this->expr($expr->right);
        else if ($expr instanceof ArrayLiteral)
            $res = "[" . implode(", ", array_map(function ($x) { return $this->expr($x); }, $expr->items)) . "]";
        else if ($expr instanceof CastExpression)
            $res = "<" . $this->type($expr->newType) . ">(" . $this->expr($expr->expression) . ")";
        else if ($expr instanceof ConditionalExpression)
            $res = $this->expr($expr->condition) . " ? " . $this->expr($expr->whenTrue) . " : " . $this->expr($expr->whenFalse);
        else if ($expr instanceof InstanceOfExpression)
            $res = $this->expr($expr->expr) . " instanceof " . $this->type($expr->checkType);
        else if ($expr instanceof ParenthesizedExpression)
            $res = "(" . $this->expr($expr->expression) . ")";
        else if ($expr instanceof RegexLiteral)
            $res = "/" . $expr->pattern . "/" . ($expr->global ? "g" : "") . ($expr->caseInsensitive ? "g" : "");
        else if ($expr instanceof Lambda)
            $res = "(" . implode(", ", array_map(function ($x) { return $x->name . ($x->type !== null ? ": " . $this->type($x->type) : ""); }, $expr->parameters)) . ")" . ($expr->captures !== null && count($expr->captures) > 0 ? " @captures(" . implode(", ", array_map(function ($x) { return $x->name; }, $expr->captures)) . ")" : "") . " => { " . $this->rawBlock($expr->body) . " }";
        else if ($expr instanceof UnaryExpression && $expr->unaryType === UnaryType::PREFIX)
            $res = $expr->operator . $this->expr($expr->operand);
        else if ($expr instanceof UnaryExpression && $expr->unaryType === UnaryType::POSTFIX)
            $res = $this->expr($expr->operand) . $expr->operator;
        else if ($expr instanceof MapLiteral) {
            $repr = implode(",\n", array_map(function ($item) { return $item->key . ": " . $this->expr($item->value); }, $expr->items));
            $res = "{L:M}" . ($repr === "" ? "{}" : (strpos($repr, "\n") !== false ? "{\n" . $this->pad($repr) . "\n}" : "{ " . $repr . " }"));
        }
        else if ($expr instanceof NullLiteral)
            $res = "null";
        else if ($expr instanceof AwaitExpression)
            $res = "await " . $this->expr($expr->expr);
        else if ($expr instanceof ThisReference)
            $res = "{R}this";
        else if ($expr instanceof StaticThisReference)
            $res = "{R:Static}this";
        else if ($expr instanceof EnumReference)
            $res = "{R:Enum}" . $expr->decl->name;
        else if ($expr instanceof ClassReference)
            $res = "{R:Cls}" . $expr->decl->name;
        else if ($expr instanceof MethodParameterReference)
            $res = "{R:MetP}" . $expr->decl->name;
        else if ($expr instanceof VariableDeclarationReference)
            $res = "{V}" . $expr->decl->name;
        else if ($expr instanceof ForVariableReference)
            $res = "{R:ForV}" . $expr->decl->name;
        else if ($expr instanceof ForeachVariableReference)
            $res = "{R:ForEV}" . $expr->decl->name;
        else if ($expr instanceof CatchVariableReference)
            $res = "{R:CatchV}" . $expr->decl->name;
        else if ($expr instanceof GlobalFunctionReference)
            $res = "{R:GFunc}" . $expr->decl->name;
        else if ($expr instanceof SuperReference)
            $res = "{R}super";
        else if ($expr instanceof StaticFieldReference)
            $res = "{R:StFi}" . $expr->decl->parentInterface->name . "::" . $expr->decl->name;
        else if ($expr instanceof StaticPropertyReference)
            $res = "{R:StPr}" . $expr->decl->parentClass->name . "::" . $expr->decl->name;
        else if ($expr instanceof InstanceFieldReference)
            $res = $this->expr($expr->object) . ".{F}" . $expr->field->name;
        else if ($expr instanceof InstancePropertyReference)
            $res = $this->expr($expr->object) . ".{P}" . $expr->property->name;
        else if ($expr instanceof EnumMemberReference)
            $res = "{E}" . $expr->decl->parentEnum->name . "::" . $expr->decl->name;
        else if ($expr instanceof NullCoalesceExpression)
            $res = $this->expr($expr->defaultExpr) . " ?? " . $this->expr($expr->exprIfNull);
        else { }
        
        if ($this->showTypes)
            $res = "<" . $this->type($expr->getType(), true) . ">(" . $res . ")";
        
        return $res;
    }
    
    function block($block, $allowOneLiner = true) {
        if ($this->previewOnly)
            return " { ... }";
        $stmtLen = count($block->statements);
        return $stmtLen === 0 ? " { }" : ($allowOneLiner && $stmtLen === 1 ? "\n" . $this->pad($this->rawBlock($block)) : " {\n" . $this->pad($this->rawBlock($block)) . "\n}");
    }
    
    function stmt($stmt) {
        $res = "UNKNOWN-STATEMENT";
        if ($stmt instanceof BreakStatement)
            $res = "break;";
        else if ($stmt instanceof ReturnStatement)
            $res = $stmt->expression === null ? "return;" : "return " . $this->expr($stmt->expression) . ";";
        else if ($stmt instanceof UnsetStatement)
            $res = "unset " . $this->expr($stmt->expression) . ";";
        else if ($stmt instanceof ThrowStatement)
            $res = "throw " . $this->expr($stmt->expression) . ";";
        else if ($stmt instanceof ExpressionStatement)
            $res = $this->expr($stmt->expression) . ";";
        else if ($stmt instanceof VariableDeclaration)
            $res = "var " . $this->var($stmt) . ";";
        else if ($stmt instanceof ForeachStatement)
            $res = "for (const " . $stmt->itemVar->name . " of " . $this->expr($stmt->items) . ")" . $this->block($stmt->body);
        else if ($stmt instanceof IfStatement) {
            $elseIf = $stmt->else_ !== null && count($stmt->else_->statements) === 1 && $stmt->else_->statements[0] instanceof IfStatement;
            $res = "if (" . $this->expr($stmt->condition) . ")" . $this->block($stmt->then);
            if (!$this->previewOnly)
                $res .= ($elseIf ? "\nelse " . $this->stmt($stmt->else_->statements[0]) : "") . (!$elseIf && $stmt->else_ !== null ? "\nelse" . $this->block($stmt->else_) : "");
        }
        else if ($stmt instanceof WhileStatement)
            $res = "while (" . $this->expr($stmt->condition) . ")" . $this->block($stmt->body);
        else if ($stmt instanceof ForStatement)
            $res = "for (" . ($stmt->itemVar !== null ? $this->var($stmt->itemVar) : "") . "; " . $this->expr($stmt->condition) . "; " . $this->expr($stmt->incrementor) . ")" . $this->block($stmt->body);
        else if ($stmt instanceof DoStatement)
            $res = "do" . $this->block($stmt->body) . " while (" . $this->expr($stmt->condition) . ")";
        else if ($stmt instanceof TryStatement)
            $res = "try" . $this->block($stmt->tryBody, false) . ($stmt->catchBody !== null ? " catch (" . $stmt->catchVar->name . ")" . $this->block($stmt->catchBody) : "") . ($stmt->finallyBody !== null ? "finally" . $this->block($stmt->finallyBody) : "");
        else if ($stmt instanceof ContinueStatement)
            $res = "continue;";
        else { }
        return $this->previewOnly ? $res : $this->leading($stmt) . $res;
    }
    
    function rawBlock($block) {
        return implode("\n", array_map(function ($stmt) { return $this->stmt($stmt); }, $block->statements));
    }
    
    function methodBase($method, $returns) {
        if ($method === null)
            return "";
        $name = $method instanceof Method ? $method->name : ($method instanceof Constructor ? "constructor" : ($method instanceof GlobalFunction ? $method->name : "???"));
        $typeArgs = $method instanceof Method ? $method->typeArguments : null;
        return $this->preIf("/* throws */ ", $method->throws) . $name . $this->typeArgs($typeArgs) . "(" . implode(", ", array_map(function ($p) { return $this->leading($p) . $this->var($p); }, $method->parameters)) . ")" . ($returns instanceof VoidType ? "" : ": " . $this->type($returns)) . ($method->body !== null ? " {\n" . $this->pad($this->rawBlock($method->body)) . "\n}" : ";");
    }
    
    function method($method) {
        return $method === null ? "" : ($method->isStatic ? "static " : "") . ($method->attributes !== null && array_key_exists("mutates", $method->attributes) ? "@mutates " : "") . $this->methodBase($method, $method->returns);
    }
    
    function classLike($cls) {
        $resList = array();
        $resList[] = implode("\n", array_map(function ($field) { return $this->var($field) . ";"; }, $cls->fields));
        if ($cls instanceof Class_) {
            $resList[] = implode("\n", array_map(function ($prop) { return $this->var($prop) . ";"; }, $cls->properties));
            $resList[] = $this->methodBase($cls->constructor_, VoidType::$instance);
        }
        $resList[] = implode("\n\n", array_map(function ($method) { return $this->method($method); }, $cls->methods));
        return $this->pad(implode("\n\n", array_values(array_filter($resList, function ($x) { return $x !== ""; }))));
    }
    
    function pad($str) {
        return implode("\n", array_map(function ($x) { return "    " . $x; }, preg_split("/\\n/", $str)));
    }
    
    function imp($imp) {
        return "" . ($imp instanceof UnresolvedImport ? "X" : ($imp instanceof Class_ ? "C" : ($imp instanceof Interface_ ? "I" : ($imp instanceof Enum ? "E" : "???")))) . ":" . $imp->name;
    }
    
    function nodeRepr($node) {
        if ($node instanceof Statement)
            return $this->stmt($node);
        else if ($node instanceof Expression)
            return $this->expr($node);
        else
            return "/* TODO: missing */";
    }
    
    function generate($sourceFile) {
        $imps = array_map(function ($imp) { return ($imp->importAll ? "import * as " . $imp->importAs : "import { " . implode(", ", array_map(function ($x) { return $this->imp($x); }, $imp->imports)) . " }") . " from \"" . $imp->exportScope->packageName . $this->pre("/", $imp->exportScope->scopeName) . "\";"; }, $sourceFile->imports);
        $enums = array_map(function ($enum_) { return $this->leading($enum_) . "enum " . $enum_->name . " { " . implode(", ", array_map(function ($x) { return $x->name; }, $enum_->values)) . " }"; }, $sourceFile->enums);
        $intfs = array_map(function ($intf) { return $this->leading($intf) . "interface " . $intf->name . $this->typeArgs($intf->typeArguments) . $this->preArr(" extends ", array_map(function ($x) { return $this->type($x); }, $intf->baseInterfaces)) . " {\n" . $this->classLike($intf) . "\n}"; }, $sourceFile->interfaces);
        $classes = array_map(function ($cls) { return $this->leading($cls) . "class " . $cls->name . $this->typeArgs($cls->typeArguments) . $this->pre(" extends ", $cls->baseClass !== null ? $this->type($cls->baseClass) : null) . $this->preArr(" implements ", array_map(function ($x) { return $this->type($x); }, $cls->baseInterfaces)) . " {\n" . $this->classLike($cls) . "\n}"; }, $sourceFile->classes);
        $funcs = array_map(function ($func) { return $this->leading($func) . "function " . $func->name . $this->methodBase($func, $func->returns); }, $sourceFile->funcs);
        $main = $this->rawBlock($sourceFile->mainBlock);
        $result = "// export scope: " . $sourceFile->exportScope->packageName . "/" . $sourceFile->exportScope->scopeName . "\n" . implode("\n\n", array_values(array_filter(array(implode("\n", $imps), implode("\n", $enums), implode("\n\n", $intfs), implode("\n\n", $classes), implode("\n\n", $funcs), $main), function ($x) { return $x !== ""; })));
        return $result;
    }
}
TSOverviewGenerator::StaticInit();
