<?php

namespace Parsers\TypeScriptParser2;

use Parsers\Common\Reader\Reader;
use Parsers\Common\Reader\IReaderHooks;
use Parsers\Common\Reader\ParseError;
use Parsers\Common\ExpressionParser\ExpressionParser;
use Parsers\Common\ExpressionParser\IExpressionParserHooks;
use Parsers\Common\NodeManager\NodeManager;
use Parsers\Common\IParser\IParser;
use One\Ast\AstTypes\AnyType;
use One\Ast\AstTypes\VoidType;
use One\Ast\AstTypes\UnresolvedType;
use One\Ast\AstTypes\LambdaType;
use One\Ast\Expressions\Expression;
use One\Ast\Expressions\TemplateString;
use One\Ast\Expressions\TemplateStringPart;
use One\Ast\Expressions\NewExpression;
use One\Ast\Expressions\Identifier;
use One\Ast\Expressions\CastExpression;
use One\Ast\Expressions\NullLiteral;
use One\Ast\Expressions\BooleanLiteral;
use One\Ast\Expressions\BinaryExpression;
use One\Ast\Expressions\UnaryExpression;
use One\Ast\Expressions\UnresolvedCallExpression;
use One\Ast\Expressions\PropertyAccessExpression;
use One\Ast\Expressions\InstanceOfExpression;
use One\Ast\Expressions\RegexLiteral;
use One\Ast\Expressions\AwaitExpression;
use One\Ast\Expressions\ParenthesizedExpression;
use One\Ast\Expressions\UnresolvedNewExpression;
use One\Ast\Statements\VariableDeclaration;
use One\Ast\Statements\Statement;
use One\Ast\Statements\UnsetStatement;
use One\Ast\Statements\IfStatement;
use One\Ast\Statements\WhileStatement;
use One\Ast\Statements\ForeachStatement;
use One\Ast\Statements\ForStatement;
use One\Ast\Statements\ReturnStatement;
use One\Ast\Statements\ThrowStatement;
use One\Ast\Statements\BreakStatement;
use One\Ast\Statements\ExpressionStatement;
use One\Ast\Statements\ForeachVariable;
use One\Ast\Statements\ForVariable;
use One\Ast\Statements\DoStatement;
use One\Ast\Statements\ContinueStatement;
use One\Ast\Statements\TryStatement;
use One\Ast\Statements\CatchVariable;
use One\Ast\Statements\Block;
use One\Ast\Types\Class_;
use One\Ast\Types\Method;
use One\Ast\Types\MethodParameter;
use One\Ast\Types\Field;
use One\Ast\Types\Visibility;
use One\Ast\Types\SourceFile;
use One\Ast\Types\Property;
use One\Ast\Types\Constructor;
use One\Ast\Types\Interface_;
use One\Ast\Types\EnumMember;
use One\Ast\Types\Enum;
use One\Ast\Types\IMethodBase;
use One\Ast\Types\Import;
use One\Ast\Types\SourcePath;
use One\Ast\Types\ExportScopeRef;
use One\Ast\Types\Package;
use One\Ast\Types\Lambda;
use One\Ast\Types\UnresolvedImport;
use One\Ast\Types\GlobalFunction;
use One\Ast\Interfaces\IType;

class TypeAndInit {
    public $type;
    public $init;
    
    function __construct($type, $init) {
        $this->type = $type;
        $this->init = $init;
    }
}

class MethodSignature {
    public $params;
    public $fields;
    public $body;
    public $returns;
    public $superCallArgs;
    
    function __construct($params, $fields, $body, $returns, $superCallArgs) {
        $this->params = $params;
        $this->fields = $fields;
        $this->body = $body;
        $this->returns = $returns;
        $this->superCallArgs = $superCallArgs;
    }
}

class TypeScriptParser2 implements IParser, IExpressionParserHooks, IReaderHooks {
    public $context;
    public $reader;
    public $expressionParser;
    public $nodeManager;
    public $exportScope;
    public $missingReturnTypeIsVoid = false;
    public $path;
    
    function __construct($source, $path = null) {
        $this->path = $path;
        $this->context = array();
        $this->reader = new Reader($source);
        $this->reader->hooks = $this;
        $this->nodeManager = new NodeManager($this->reader);
        $this->expressionParser = $this->createExpressionParser($this->reader, $this->nodeManager);
        $this->exportScope = $this->path !== null ? new ExportScopeRef($this->path->pkg->name, $this->path->path !== null ? preg_replace("/.ts$/", "", $this->path->path) : null) : null;
    }
    
    function createExpressionParser($reader, $nodeManager = null) {
        $expressionParser = new ExpressionParser($reader, $this, $nodeManager);
        $expressionParser->stringLiteralType = new UnresolvedType("TsString", array());
        $expressionParser->numericLiteralType = new UnresolvedType("TsNumber", array());
        return $expressionParser;
    }
    
    function errorCallback($error) {
        throw new \OneLang\Error("[TypeScriptParser] " . $error->message . " at " . $error->cursor->line . ":" . $error->cursor->column . " (context: " . implode("/", $this->context) . ")\n" . $this->reader->linePreview($error->cursor));
    }
    
    function infixPrehook($left) {
        if ($left instanceof PropertyAccessExpression && $this->reader->peekRegex("<[A-Za-z0-9_<>]*?>\\(") !== null) {
            $typeArgs = $this->parseTypeArgs();
            $this->reader->expectToken("(");
            $args = $this->expressionParser->parseCallArguments();
            return new UnresolvedCallExpression($left, $typeArgs, $args);
        }
        else if ($this->reader->readToken("instanceof")) {
            $type = $this->parseType();
            return new InstanceOfExpression($left, $type);
        }
        else if ($left instanceof Identifier && $this->reader->readToken("=>")) {
            $block = $this->parseLambdaBlock();
            return new Lambda(array(new MethodParameter($left->text, null, null, null)), $block);
        }
        return null;
    }
    
    function parseLambdaParams() {
        if (!$this->reader->readToken("("))
            return null;
        
        $params = array();
        if (!$this->reader->readToken(")")) {
            do {
                $paramName = $this->reader->expectIdentifier();
                $type = $this->reader->readToken(":") ? $this->parseType() : null;
                $params[] = new MethodParameter($paramName, $type, null, null);
            } while ($this->reader->readToken(","));
            $this->reader->expectToken(")");
        }
        return $params;
    }
    
    function parseType() {
        if ($this->reader->readToken("{")) {
            $this->reader->expectToken("[");
            $this->reader->readIdentifier();
            $this->reader->expectToken(":");
            $this->reader->expectToken("string");
            $this->reader->expectToken("]");
            $this->reader->expectToken(":");
            $mapValueType = $this->parseType();
            $this->reader->readToken(";");
            $this->reader->expectToken("}");
            return new UnresolvedType("TsMap", array($mapValueType));
        }
        
        if ($this->reader->peekToken("(")) {
            $params = $this->parseLambdaParams();
            $this->reader->expectToken("=>");
            $returnType = $this->parseType();
            return new LambdaType($params, $returnType);
        }
        
        $typeName = $this->reader->expectIdentifier();
        $startPos = $this->reader->prevTokenOffset;
        
        /* @var $type */
        if ($typeName === "string")
            $type = new UnresolvedType("TsString", array());
        else if ($typeName === "boolean")
            $type = new UnresolvedType("TsBoolean", array());
        else if ($typeName === "number")
            $type = new UnresolvedType("TsNumber", array());
        else if ($typeName === "any")
            $type = AnyType::$instance;
        else if ($typeName === "void")
            $type = VoidType::$instance;
        else {
            $typeArguments = $this->parseTypeArgs();
            $type = new UnresolvedType($typeName, $typeArguments);
        }
        
        $this->nodeManager->addNode($type, $startPos);
        
        while ($this->reader->readToken("[]")) {
            $type = new UnresolvedType("TsArray", array($type));
            $this->nodeManager->addNode($type, $startPos);
        }
        
        return $type;
    }
    
    function parseExpression() {
        return $this->expressionParser->parse();
    }
    
    function unaryPrehook() {
        if ($this->reader->readToken("null"))
            return new NullLiteral();
        else if ($this->reader->readToken("true"))
            return new BooleanLiteral(true);
        else if ($this->reader->readToken("false"))
            return new BooleanLiteral(false);
        else if ($this->reader->readToken("`")) {
            $parts = array();
            $litPart = "";
            while (true) {
                if ($this->reader->readExactly("`")) {
                    if ($litPart !== "") {
                        $parts[] = TemplateStringPart::Literal($litPart);
                        $litPart = "";
                    }
                    
                    break;
                }
                else if ($this->reader->readExactly("\${")) {
                    if ($litPart !== "") {
                        $parts[] = TemplateStringPart::Literal($litPart);
                        $litPart = "";
                    }
                    
                    $expr = $this->parseExpression();
                    $parts[] = TemplateStringPart::Expression($expr);
                    $this->reader->expectToken("}");
                }
                else if ($this->reader->readExactly("\\")) {
                    $chr = $this->reader->readChar();
                    if ($chr === "n")
                        $litPart .= "\n";
                    else if ($chr === "r")
                        $litPart .= "\r";
                    else if ($chr === "t")
                        $litPart .= "\t";
                    else if ($chr === "`")
                        $litPart .= "`";
                    else if ($chr === "\$")
                        $litPart .= "\$";
                    else if ($chr === "\\")
                        $litPart .= "\\";
                    else
                        $this->reader->fail("invalid escape", $this->reader->offset - 1);
                }
                else {
                    $chr = $this->reader->readChar();
                    $chrCode = ord($chr[0]);
                    if (!(32 <= $chrCode && $chrCode <= 126) || $chr === "`" || $chr === "\\")
                        $this->reader->fail("not allowed character (code=" . $chrCode . ")", $this->reader->offset - 1);
                    $litPart .= $chr;
                }
            }
            return new TemplateString($parts);
        }
        else if ($this->reader->readToken("new")) {
            $type = $this->parseType();
            if ($type instanceof UnresolvedType) {
                $this->reader->expectToken("(");
                $args = $this->expressionParser->parseCallArguments();
                return new UnresolvedNewExpression($type, $args);
            }
            else
                throw new \OneLang\Error("[TypeScriptParser2] Expected UnresolvedType here!");
        }
        else if ($this->reader->readToken("<")) {
            $newType = $this->parseType();
            $this->reader->expectToken(">");
            $expression = $this->parseExpression();
            return new CastExpression($newType, $expression);
        }
        else if ($this->reader->readToken("/")) {
            $pattern = "";
            while (true) {
                $chr = $this->reader->readChar();
                if ($chr === "\\") {
                    $chr2 = $this->reader->readChar();
                    $pattern .= $chr2 === "/" ? "/" : "\\" . $chr2;
                }
                else if ($chr === "/")
                    break;
                else
                    $pattern .= $chr;
            }
            $modifiers = $this->reader->readModifiers(array("g", "i"));
            return new RegexLiteral($pattern, in_array("i", $modifiers), in_array("g", $modifiers));
        }
        else if ($this->reader->readToken("typeof")) {
            $expr = $this->expressionParser->parse($this->expressionParser->prefixPrecedence);
            $this->reader->expectToken("===");
            $check = $this->reader->expectString();
            
            $tsType = null;
            if ($check === "string")
                $tsType = "TsString";
            else if ($check === "boolean")
                $tsType = "TsBoolean";
            else if ($check === "object")
                $tsType = "Object";
            else if ($check === "function")
                // TODO: ???
                $tsType = "Function";
            else if ($check === "undefined")
                // TODO: ???
                $tsType = "Object";
            else
                $this->reader->fail("unexpected typeof comparison");
            
            return new InstanceOfExpression($expr, new UnresolvedType($tsType, array()));
        }
        else if ($this->reader->peekRegex("\\([A-Za-z0-9_]+\\s*[:,]|\\(\\)") !== null) {
            $params = $this->parseLambdaParams();
            $this->reader->expectToken("=>");
            $block = $this->parseLambdaBlock();
            return new Lambda($params, $block);
        }
        else if ($this->reader->readToken("await")) {
            $expression = $this->parseExpression();
            return new AwaitExpression($expression);
        }
        
        $mapLiteral = $this->expressionParser->parseMapLiteral();
        if ($mapLiteral !== null)
            return $mapLiteral;
        
        $arrayLiteral = $this->expressionParser->parseArrayLiteral();
        if ($arrayLiteral !== null)
            return $arrayLiteral;
        
        return null;
    }
    
    function parseLambdaBlock() {
        $block = $this->parseBlock();
        if ($block !== null)
            return $block;
        
        $returnExpr = $this->parseExpression();
        if ($returnExpr instanceof ParenthesizedExpression)
            $returnExpr = $returnExpr->expression;
        return new Block(array(new ReturnStatement($returnExpr)));
    }
    
    function parseTypeAndInit() {
        $type = $this->reader->readToken(":") ? $this->parseType() : null;
        $init = $this->reader->readToken("=") ? $this->parseExpression() : null;
        
        if ($type === null && $init === null)
            $this->reader->fail("expected type declaration or initializer");
        
        return new TypeAndInit($type, $init);
    }
    
    function expectBlockOrStatement() {
        $block = $this->parseBlock();
        if ($block !== null)
            return $block;
        
        $stmts = array();
        $stmt = $this->expectStatement();
        if ($stmt !== null)
            $stmts[] = $stmt;
        return new Block($stmts);
    }
    
    function expectStatement() {
        $statement = null;
        
        $leadingTrivia = $this->reader->readLeadingTrivia();
        $startPos = $this->reader->offset;
        
        $requiresClosing = true;
        $varDeclMatches = $this->reader->readRegex("(const|let|var)\\b");
        if ($varDeclMatches !== null) {
            $name = $this->reader->expectIdentifier("expected variable name");
            $typeAndInit = $this->parseTypeAndInit();
            $statement = new VariableDeclaration($name, $typeAndInit->type, $typeAndInit->init);
        }
        else if ($this->reader->readToken("delete"))
            $statement = new UnsetStatement($this->parseExpression());
        else if ($this->reader->readToken("if")) {
            $requiresClosing = false;
            $this->reader->expectToken("(");
            $condition = $this->parseExpression();
            $this->reader->expectToken(")");
            $then = $this->expectBlockOrStatement();
            $else_ = $this->reader->readToken("else") ? $this->expectBlockOrStatement() : null;
            $statement = new IfStatement($condition, $then, $else_);
        }
        else if ($this->reader->readToken("while")) {
            $requiresClosing = false;
            $this->reader->expectToken("(");
            $condition = $this->parseExpression();
            $this->reader->expectToken(")");
            $body = $this->expectBlockOrStatement();
            $statement = new WhileStatement($condition, $body);
        }
        else if ($this->reader->readToken("do")) {
            $requiresClosing = false;
            $body = $this->expectBlockOrStatement();
            $this->reader->expectToken("while");
            $this->reader->expectToken("(");
            $condition = $this->parseExpression();
            $this->reader->expectToken(")");
            $statement = new DoStatement($condition, $body);
        }
        else if ($this->reader->readToken("for")) {
            $requiresClosing = false;
            $this->reader->expectToken("(");
            $varDeclMod = $this->reader->readAnyOf(array("const", "let", "var"));
            $itemVarName = $varDeclMod === null ? null : $this->reader->expectIdentifier();
            if ($itemVarName !== null && $this->reader->readToken("of")) {
                $items = $this->parseExpression();
                $this->reader->expectToken(")");
                $body = $this->expectBlockOrStatement();
                $statement = new ForeachStatement(new ForeachVariable($itemVarName), $items, $body);
            }
            else {
                $forVar = null;
                if ($itemVarName !== null) {
                    $typeAndInit = $this->parseTypeAndInit();
                    $forVar = new ForVariable($itemVarName, $typeAndInit->type, $typeAndInit->init);
                }
                $this->reader->expectToken(";");
                $condition = $this->parseExpression();
                $this->reader->expectToken(";");
                $incrementor = $this->parseExpression();
                $this->reader->expectToken(")");
                $body = $this->expectBlockOrStatement();
                $statement = new ForStatement($forVar, $condition, $incrementor, $body);
            }
        }
        else if ($this->reader->readToken("try")) {
            $block = $this->expectBlock("try body is missing");
            
            $catchVar = null;
            $catchBody = null;
            if ($this->reader->readToken("catch")) {
                $this->reader->expectToken("(");
                $catchVar = new CatchVariable($this->reader->expectIdentifier(), null);
                $this->reader->expectToken(")");
                $catchBody = $this->expectBlock("catch body is missing");
            }
            
            $finallyBody = $this->reader->readToken("finally") ? $this->expectBlock() : null;
            return new TryStatement($block, $catchVar, $catchBody, $finallyBody);
        }
        else if ($this->reader->readToken("return")) {
            $expr = $this->reader->peekToken(";") ? null : $this->parseExpression();
            $statement = new ReturnStatement($expr);
        }
        else if ($this->reader->readToken("throw")) {
            $expr = $this->parseExpression();
            $statement = new ThrowStatement($expr);
        }
        else if ($this->reader->readToken("break"))
            $statement = new BreakStatement();
        else if ($this->reader->readToken("continue"))
            $statement = new ContinueStatement();
        else if ($this->reader->readToken("debugger;"))
            return null;
        else {
            $expr = $this->parseExpression();
            $statement = new ExpressionStatement($expr);
            $isBinarySet = $expr instanceof BinaryExpression && in_array($expr->operator, array("=", "+=", "-="));
            $isUnarySet = $expr instanceof UnaryExpression && in_array($expr->operator, array("++", "--"));
            if (!($expr instanceof UnresolvedCallExpression || $isBinarySet || $isUnarySet || $expr instanceof AwaitExpression))
                $this->reader->fail("this expression is not allowed as statement");
        }
        
        if ($statement === null)
            $this->reader->fail("unknown statement");
        
        $statement->leadingTrivia = $leadingTrivia;
        $this->nodeManager->addNode($statement, $startPos);
        
        $statementLastLine = $this->reader->wsLineCounter;
        if (!$this->reader->readToken(";") && $requiresClosing && $this->reader->wsLineCounter === $statementLastLine)
            $this->reader->fail("statement is not closed", $this->reader->wsOffset);
        
        return $statement;
    }
    
    function parseBlock() {
        if (!$this->reader->readToken("{"))
            return null;
        $startPos = $this->reader->prevTokenOffset;
        
        $statements = array();
        if (!$this->reader->readToken("}"))
            do {
                $statement = $this->expectStatement();
                if ($statement !== null)
                    $statements[] = $statement;
            } while (!$this->reader->readToken("}"));
        
        $block = new Block($statements);
        $this->nodeManager->addNode($block, $startPos);
        return $block;
    }
    
    function expectBlock($errorMsg = null) {
        $block = $this->parseBlock();
        if ($block === null)
            $this->reader->fail($errorMsg ?? "expected block here");
        return $block;
    }
    
    function parseTypeArgs() {
        $typeArguments = array();
        if ($this->reader->readToken("<")) {
            do {
                $generics = $this->parseType();
                $typeArguments[] = $generics;
            } while ($this->reader->readToken(","));
            $this->reader->expectToken(">");
        }
        return $typeArguments;
    }
    
    function parseGenericsArgs() {
        $typeArguments = array();
        if ($this->reader->readToken("<")) {
            do {
                $generics = $this->reader->expectIdentifier();
                $typeArguments[] = $generics;
            } while ($this->reader->readToken(","));
            $this->reader->expectToken(">");
        }
        return $typeArguments;
    }
    
    function parseExprStmtFromString($expression) {
        $expr = $this->createExpressionParser(new Reader($expression))->parse();
        return new ExpressionStatement($expr);
    }
    
    function parseMethodSignature($isConstructor, $declarationOnly) {
        $params = array();
        $fields = array();
        if (!$this->reader->readToken(")")) {
            do {
                $leadingTrivia = $this->reader->readLeadingTrivia();
                $paramStart = $this->reader->offset;
                $isPublic = $this->reader->readToken("public");
                if ($isPublic && !$isConstructor)
                    $this->reader->fail("public modifier is only allowed in constructor definition");
                
                $paramName = $this->reader->expectIdentifier();
                $this->context[] = "arg:" . $paramName;
                $typeAndInit = $this->parseTypeAndInit();
                $param = new MethodParameter($paramName, $typeAndInit->type, $typeAndInit->init, $leadingTrivia);
                $params[] = $param;
                
                // init should be used as only the constructor's method parameter, but not again as a field initializer too
                //   (otherwise it would called twice if cloned or cause AST error is just referenced from two separate places)
                if ($isPublic) {
                    $field = new Field($paramName, $typeAndInit->type, null, Visibility::PUBLIC, false, $param, $param->leadingTrivia);
                    $fields[] = $field;
                    $param->fieldDecl = $field;
                }
                
                $this->nodeManager->addNode($param, $paramStart);
                array_pop($this->context);
            } while ($this->reader->readToken(","));
            
            $this->reader->expectToken(")");
        }
        
        $returns = null;
        if (!$isConstructor)
            // in case of constructor, "returns" won't be used
            $returns = $this->reader->readToken(":") ? $this->parseType() : ($this->missingReturnTypeIsVoid ? VoidType::$instance : null);
        
        $body = null;
        $superCallArgs = null;
        if ($declarationOnly)
            $this->reader->expectToken(";");
        else {
            $body = $this->expectBlock("method body is missing");
            $firstStmt = count($body->statements) > 0 ? $body->statements[0] : null;
            if ($firstStmt instanceof ExpressionStatement && $firstStmt->expression instanceof UnresolvedCallExpression && $firstStmt->expression->func instanceof Identifier && $firstStmt->expression->func->text === "super") {
                $superCallArgs = $firstStmt->expression->args;
                array_shift($body->statements);
            }
        }
        
        return new MethodSignature($params, $fields, $body, $returns, $superCallArgs);
    }
    
    function parseIdentifierOrString() {
        return $this->reader->readString() ?? $this->reader->expectIdentifier();
    }
    
    function parseInterface($leadingTrivia, $isExported) {
        if (!$this->reader->readToken("interface"))
            return null;
        $intfStart = $this->reader->prevTokenOffset;
        
        $intfName = $this->reader->expectIdentifier("expected identifier after 'interface' keyword");
        $this->context[] = "I:" . $intfName;
        
        $intfTypeArgs = $this->parseGenericsArgs();
        
        $baseInterfaces = array();
        if ($this->reader->readToken("extends"))
            do
                $baseInterfaces[] = $this->parseType(); while ($this->reader->readToken(","));
        
        $methods = array();
        $fields = array();
        
        $this->reader->expectToken("{");
        while (!$this->reader->readToken("}")) {
            $memberLeadingTrivia = $this->reader->readLeadingTrivia();
            
            $memberStart = $this->reader->offset;
            $memberName = $this->parseIdentifierOrString();
            if ($this->reader->readToken(":")) {
                $this->context[] = "F:" . $memberName;
                
                $fieldType = $this->parseType();
                $this->reader->expectToken(";");
                
                $field = new Field($memberName, $fieldType, null, Visibility::PUBLIC, false, null, $memberLeadingTrivia);
                $fields[] = $field;
                
                $this->nodeManager->addNode($field, $memberStart);
                array_pop($this->context);
            }
            else {
                $this->context[] = "M:" . $memberName;
                $methodTypeArgs = $this->parseGenericsArgs();
                $this->reader->expectToken("(");
                // method
                   
                $sig = $this->parseMethodSignature(false, true);
                
                $method = new Method($memberName, $methodTypeArgs, $sig->params, $sig->body, Visibility::PUBLIC, false, $sig->returns, false, $memberLeadingTrivia);
                $methods[] = $method;
                $this->nodeManager->addNode($method, $memberStart);
                array_pop($this->context);
            }
        }
        
        $intf = new Interface_($intfName, $intfTypeArgs, $baseInterfaces, $fields, $methods, $isExported, $leadingTrivia);
        $this->nodeManager->addNode($intf, $intfStart);
        array_pop($this->context);
        return $intf;
    }
    
    function parseSpecifiedType() {
        $typeName = $this->reader->readIdentifier();
        $typeArgs = $this->parseTypeArgs();
        return new UnresolvedType($typeName, $typeArgs);
    }
    
    function parseClass($leadingTrivia, $isExported, $declarationOnly) {
        $clsModifiers = $this->reader->readModifiers(array("abstract"));
        if (!$this->reader->readToken("class"))
            return null;
        $clsStart = $this->reader->prevTokenOffset;
        
        $clsName = $this->reader->expectIdentifier("expected identifier after 'class' keyword");
        $this->context[] = "C:" . $clsName;
        
        $typeArgs = $this->parseGenericsArgs();
        $baseClass = $this->reader->readToken("extends") ? $this->parseSpecifiedType() : null;
        
        $baseInterfaces = array();
        if ($this->reader->readToken("implements"))
            do
                $baseInterfaces[] = $this->parseSpecifiedType(); while ($this->reader->readToken(","));
        
        $constructor = null;
        $fields = array();
        $methods = array();
        $properties = array();
        
        $this->reader->expectToken("{");
        while (!$this->reader->readToken("}")) {
            $memberLeadingTrivia = $this->reader->readLeadingTrivia();
            
            $memberStart = $this->reader->offset;
            $modifiers = $this->reader->readModifiers(array("static", "public", "protected", "private", "readonly", "async", "abstract"));
            $isStatic = in_array("static", $modifiers);
            $isAsync = in_array("async", $modifiers);
            $isAbstract = in_array("abstract", $modifiers);
            $visibility = in_array("private", $modifiers) ? Visibility::PRIVATE : (in_array("protected", $modifiers) ? Visibility::PROTECTED : Visibility::PUBLIC);
            
            $memberName = $this->parseIdentifierOrString();
            $methodTypeArgs = $this->parseGenericsArgs();
            if ($this->reader->readToken("(")) {
                // method
                $isConstructor = $memberName === "constructor";
                
                /* @var $member */
                $sig = $this->parseMethodSignature($isConstructor, $declarationOnly || $isAbstract);
                if ($isConstructor) {
                    $member = $constructor = new Constructor($sig->params, $sig->body, $sig->superCallArgs, $memberLeadingTrivia);
                    foreach ($sig->fields as $field)
                        $fields[] = $field;
                }
                else {
                    $method = new Method($memberName, $methodTypeArgs, $sig->params, $sig->body, $visibility, $isStatic, $sig->returns, $isAsync, $memberLeadingTrivia);
                    $methods[] = $method;
                    $member = $method;
                }
                
                $this->nodeManager->addNode($member, $memberStart);
            }
            else if ($memberName === "get" || $memberName === "set") {
                // property
                $propName = $this->reader->expectIdentifier();
                $prop = \OneLang\ArrayHelper::find($properties, function ($x) use ($propName) { return $x->name === $propName; });
                $propType = null;
                $getter = null;
                $setter = null;
                
                if ($memberName === "get") {
                    // get propName(): propType { return ... }
                    $this->context[] = "P[G]:" . $propName;
                    $this->reader->expectToken("()", "expected '()' after property getter name");
                    $propType = $this->reader->readToken(":") ? $this->parseType() : null;
                    if ($declarationOnly) {
                        if ($propType === null)
                            $this->reader->fail("Type is missing for property in declare class");
                        $this->reader->expectToken(";");
                    }
                    else {
                        $getter = $this->expectBlock("property getter body is missing");
                        if ($prop !== null)
                            $prop->getter = $getter;
                    }
                }
                else if ($memberName === "set") {
                    // set propName(value: propType) { ... }
                    $this->context[] = "P[S]:" . $propName;
                    $this->reader->expectToken("(", "expected '(' after property setter name");
                    $this->reader->expectIdentifier();
                    $propType = $this->reader->readToken(":") ? $this->parseType() : null;
                    $this->reader->expectToken(")");
                    if ($declarationOnly) {
                        if ($propType === null)
                            $this->reader->fail("Type is missing for property in declare class");
                        $this->reader->expectToken(";");
                    }
                    else {
                        $setter = $this->expectBlock("property setter body is missing");
                        if ($prop !== null)
                            $prop->setter = $setter;
                    }
                }
                
                if ($prop === null) {
                    $prop = new Property($propName, $propType, $getter, $setter, $visibility, $isStatic, $memberLeadingTrivia);
                    $properties[] = $prop;
                    $this->nodeManager->addNode($prop, $memberStart);
                }
                
                array_pop($this->context);
            }
            else {
                $this->context[] = "F:" . $memberName;
                
                $typeAndInit = $this->parseTypeAndInit();
                $this->reader->expectToken(";");
                
                $field = new Field($memberName, $typeAndInit->type, $typeAndInit->init, $visibility, $isStatic, null, $memberLeadingTrivia);
                $fields[] = $field;
                
                $this->nodeManager->addNode($field, $memberStart);
                array_pop($this->context);
            }
        }
        
        $cls = new Class_($clsName, $typeArgs, $baseClass, $baseInterfaces, $fields, $properties, $constructor, $methods, $isExported, $leadingTrivia);
        $this->nodeManager->addNode($cls, $clsStart);
        array_pop($this->context);
        return $cls;
    }
    
    function parseEnum($leadingTrivia, $isExported) {
        if (!$this->reader->readToken("enum"))
            return null;
        $enumStart = $this->reader->prevTokenOffset;
        
        $name = $this->reader->expectIdentifier("expected identifier after 'enum' keyword");
        $this->context[] = "E:" . $name;
        
        $members = array();
        
        $this->reader->expectToken("{");
        if (!$this->reader->readToken("}")) {
            do {
                if ($this->reader->peekToken("}"))
                    break;
                // eg. "enum { A, B, }" (but multiline)
                
                $enumMember = new EnumMember($this->reader->expectIdentifier());
                $members[] = $enumMember;
                $this->nodeManager->addNode($enumMember, $this->reader->prevTokenOffset);
                
                // TODO: generated code compatibility
                $this->reader->readToken("= \"" . $enumMember->name . "\"");
            } while ($this->reader->readToken(","));
            $this->reader->expectToken("}");
        }
        
        $enumObj = new Enum($name, $members, $isExported, $leadingTrivia);
        $this->nodeManager->addNode($enumObj, $enumStart);
        array_pop($this->context);
        return $enumObj;
    }
    
    static function calculateRelativePath($currFile, $relPath) {
        if (!substr_compare($relPath, ".", 0, strlen(".")) === 0)
            throw new \OneLang\Error("relPath must start with '.', but got '" . $relPath . "'");
        
        $curr = preg_split("/\\//", $currFile);
        array_pop($curr);
        // filename does not matter
        foreach (preg_split("/\\//", $relPath) as $part) {
            if ($part === "")
                throw new \OneLang\Error("relPath should not contain multiple '/' next to each other (relPath='" . $relPath . "')");
            if ($part === ".")
                // "./" == stay in current directory
                continue;
            else if ($part === "..") {
                // "../" == parent directory
                if (count($curr) === 0)
                    throw new \OneLang\Error("relPath goes out of root (curr='" . $currFile . "', relPath='" . $relPath . "')");
                array_pop($curr);
            }
            else
                $curr[] = $part;
        }
        return implode("/", $curr);
    }
    
    static function calculateImportScope($currScope, $importFile) {
        if (substr_compare($importFile, ".", 0, strlen(".")) === 0)
            // relative
            return new ExportScopeRef($currScope->packageName, TypeScriptParser2::calculateRelativePath($currScope->scopeName, $importFile));
        else {
            $path = preg_split("/\\//", $importFile);
            $pkgName = array_shift($path);
            return new ExportScopeRef($pkgName, count($path) === 0 ? Package::$INDEX : implode("/", $path));
        }
    }
    
    function readIdentifier() {
        $rawId = $this->reader->readIdentifier();
        return preg_replace("/_+$/", "", $rawId);
    }
    
    function parseImport($leadingTrivia) {
        if (!$this->reader->readToken("import"))
            return null;
        $importStart = $this->reader->prevTokenOffset;
        
        $importAllAlias = null;
        $importParts = array();
        
        if ($this->reader->readToken("*")) {
            $this->reader->expectToken("as");
            $importAllAlias = $this->reader->expectIdentifier();
        }
        else {
            $this->reader->expectToken("{");
            do {
                if ($this->reader->peekToken("}"))
                    break;
                
                $imp = $this->reader->expectIdentifier();
                if ($this->reader->readToken("as"))
                    $this->reader->fail("This is not yet supported");
                $importParts[] = new UnresolvedImport($imp);
                $this->nodeManager->addNode($imp, $this->reader->prevTokenOffset);
            } while ($this->reader->readToken(","));
            $this->reader->expectToken("}");
        }
        
        $this->reader->expectToken("from");
        $moduleName = $this->reader->expectString();
        $this->reader->expectToken(";");
        
        $importScope = $this->exportScope !== null ? TypeScriptParser2::calculateImportScope($this->exportScope, $moduleName) : null;
        
        $imports = array();
        if (count($importParts) > 0)
            $imports[] = new Import($importScope, false, $importParts, null, $leadingTrivia);
        
        if ($importAllAlias !== null)
            $imports[] = new Import($importScope, true, null, $importAllAlias, $leadingTrivia);
        //this.nodeManager.addNode(imports, importStart);
        return $imports;
    }
    
    function parseSourceFile() {
        $imports = array();
        $enums = array();
        $intfs = array();
        $classes = array();
        $funcs = array();
        while (true) {
            $leadingTrivia = $this->reader->readLeadingTrivia();
            if ($this->reader->get_eof())
                break;
            
            $imps = $this->parseImport($leadingTrivia);
            if ($imps !== null) {
                foreach ($imps as $imp)
                    $imports[] = $imp;
                continue;
            }
            
            $modifiers = $this->reader->readModifiers(array("export", "declare"));
            $isExported = in_array("export", $modifiers);
            $isDeclaration = in_array("declare", $modifiers);
            
            $cls = $this->parseClass($leadingTrivia, $isExported, $isDeclaration);
            if ($cls !== null) {
                $classes[] = $cls;
                continue;
            }
            
            $enumObj = $this->parseEnum($leadingTrivia, $isExported);
            if ($enumObj !== null) {
                $enums[] = $enumObj;
                continue;
            }
            
            $intf = $this->parseInterface($leadingTrivia, $isExported);
            if ($intf !== null) {
                $intfs[] = $intf;
                continue;
            }
            
            if ($this->reader->readToken("function")) {
                $funcName = $this->readIdentifier();
                $this->reader->expectToken("(");
                $sig = $this->parseMethodSignature(false, $isDeclaration);
                $funcs[] = new GlobalFunction($funcName, $sig->params, $sig->body, $sig->returns, $isExported, $leadingTrivia);
                continue;
            }
            
            break;
        }
        
        $this->reader->skipWhitespace();
        
        $stmts = array();
        while (true) {
            $leadingTrivia = $this->reader->readLeadingTrivia();
            if ($this->reader->get_eof())
                break;
            
            $stmt = $this->expectStatement();
            if ($stmt === null)
                continue;
            
            $stmt->leadingTrivia = $leadingTrivia;
            $stmts[] = $stmt;
        }
        
        return new SourceFile($imports, $intfs, $classes, $enums, $funcs, new Block($stmts), $this->path, $this->exportScope);
    }
    
    function parse() {
        return $this->parseSourceFile();
    }
    
    static function parseFile($source, $path = null) {
        return (new TypeScriptParser2($source, $path))->parseSourceFile();
    }
}
