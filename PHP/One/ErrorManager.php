<?php

namespace One\ErrorManager;

use One\Ast\Types\SourceFile;
use One\Ast\Types\IInterface;
use One\Ast\Types\IMethodBase;
use One\Ast\Types\Method;
use One\Ast\Types\IAstNode;
use One\Ast\Types\Field;
use One\Ast\Types\Property;
use One\Ast\Types\Constructor;
use One\Ast\Types\Lambda;
use One\AstTransformer\AstTransformer;
use One\Ast\Statements\Statement;
use Utils\TSOverviewGenerator\TSOverviewGenerator;
use One\Ast\Expressions\Expression;

class LogType {
    const INFO = 1;
    const WARNING = 2;
    const ERROR = 3;
}

class CompilationError {
    public $msg;
    public $isWarning;
    public $transformerName;
    public $node;
    
    function __construct($msg, $isWarning, $transformerName, $node) {
        $this->msg = $msg;
        $this->isWarning = $isWarning;
        $this->transformerName = $transformerName;
        $this->node = $node;
    }
}

class ErrorManager {
    public $transformer;
    public $currentNode;
    public $errors;
    public $lastContextInfo;
    
    function get_location() {
        $t = $this->transformer;
        
        $par = $this->currentNode;
        while ($par instanceof Expression)
            $par = $par->parentNode;
        
        $location = null;
        if ($par instanceof Field)
            $location = $par->parentInterface->parentFile->sourcePath->path . " -> " . $par->parentInterface->name . "::" . $par->name . " (field)";
        else if ($par instanceof Property)
            $location = $par->parentClass->parentFile->sourcePath->path . " -> " . $par->parentClass->name . "::" . $par->name . " (property)";
        else if ($par instanceof Method)
            $location = $par->parentInterface->parentFile->sourcePath->path . " -> " . $par->parentInterface->name . "::" . $par->name . " (method)";
        else if ($par instanceof Constructor)
            $location = $par->parentClass->parentFile->sourcePath->path . " -> " . $par->parentClass->name . "::constructor";
        else if ($par === null) { }
        else if ($par instanceof Statement) { }
        else { }
        
        if ($location === null && $t !== null && $t->currentFile !== null) {
            $location = $t->currentFile->sourcePath->path;
            if ($t->currentInterface !== null) {
                $location .= " -> " . $t->currentInterface->name;
                if ($t->currentMethod instanceof Method)
                    $location .= "::" . $t->currentMethod->name;
                else if ($t->currentMethod instanceof Constructor)
                    $location .= "::constructor";
                else if ($t->currentMethod instanceof Lambda)
                    $location .= "::<lambda>";
                else if ($t->currentMethod === null) { }
                else { }
            }
        }
        
        return $location;
    }
    
    function get_currentNodeRepr() {
        return TSOverviewGenerator::$preview->nodeRepr($this->currentNode);
    }
    
    function get_currentStatementRepr() {
        return $this->transformer->currentStatement === null ? "<null>" : TSOverviewGenerator::$preview->stmt($this->transformer->currentStatement);
    }
    
    function __construct()
    {
        $this->transformer = null;
        $this->currentNode = null;
        $this->errors = array();
    }
    
    function resetContext($transformer = null) {
        $this->transformer = $transformer;
    }
    
    function log($type, $msg) {
        $t = $this->transformer;
        $text = ($t !== null ? "[" . $t->name . "] " : "") . $msg;
        
        if ($this->currentNode !== null)
            $text .= "\n  Node: " . $this->get_currentNodeRepr();
        
        $location = $this->get_location();
        if ($location !== null)
            $text .= "\n  Location: " . $location;
        
        if ($t !== null && $t->currentStatement !== null)
            $text .= "\n  Statement: " . $this->get_currentStatementRepr();
        
        if ($this->lastContextInfo !== null)
            $text .= "\n  Context: " . $this->lastContextInfo;
        
        if ($type === LogType::INFO)
            \OneLang\console::log($text);
        else if ($type === LogType::WARNING)
            \OneLang\console::error("[WARNING] " . $text . "\n");
        else if ($type === LogType::ERROR)
            \OneLang\console::error($text . "\n");
        else { }
        
        if ($type === LogType::ERROR || $type === LogType::WARNING)
            $this->errors[] = new CompilationError($msg, $type === LogType::WARNING, $t !== null ? $t->name : null, $this->currentNode);
    }
    
    function info($msg) {
        $this->log(LogType::INFO, $msg);
    }
    
    function warn($msg) {
        $this->log(LogType::WARNING, $msg);
    }
    
    function throw($msg) {
        $this->log(LogType::ERROR, $msg);
    }
}
