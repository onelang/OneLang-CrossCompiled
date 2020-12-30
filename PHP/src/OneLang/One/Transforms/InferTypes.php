<?php

namespace OneLang\One\Transforms\InferTypes;

use OneLang\One\AstTransformer\AstTransformer;
use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\Ast\Types\Package;
use OneLang\One\Ast\Types\Property;
use OneLang\One\Ast\Types\Field;
use OneLang\One\Ast\Types\IMethodBase;
use OneLang\One\Ast\Types\IVariableWithInitializer;
use OneLang\One\Ast\Types\Lambda;
use OneLang\One\Ast\Types\IVariable;
use OneLang\One\Ast\Types\Method;
use OneLang\One\Ast\Types\Class_;
use OneLang\One\Ast\Types\SourceFile;
use OneLang\One\Transforms\InferTypesPlugins\BasicTypeInfer\BasicTypeInfer;
use OneLang\One\Transforms\InferTypesPlugins\Helpers\InferTypesPlugin\InferTypesPlugin;
use OneLang\One\Transforms\InferTypesPlugins\ArrayAndMapLiteralTypeInfer\ArrayAndMapLiteralTypeInfer;
use OneLang\One\Transforms\InferTypesPlugins\ResolveFieldAndPropertyAccess\ResolveFieldAndPropertyAccess;
use OneLang\One\Transforms\InferTypesPlugins\ResolveMethodCalls\ResolveMethodCalls;
use OneLang\One\Transforms\InferTypesPlugins\LambdaResolver\LambdaResolver;
use OneLang\One\Ast\Statements\Statement;
use OneLang\One\Ast\Statements\Block;
use OneLang\One\Ast\Statements\ReturnStatement;
use OneLang\One\Transforms\InferTypesPlugins\ResolveEnumMemberAccess\ResolveEnumMemberAccess;
use OneLang\One\Transforms\InferTypesPlugins\InferReturnType\InferReturnType;
use OneLang\One\Transforms\InferTypesPlugins\TypeScriptNullCoalesce\TypeScriptNullCoalesce;
use OneLang\One\Transforms\InferTypesPlugins\InferForeachVarType\InferForeachVarType;
use OneLang\One\Transforms\InferTypesPlugins\ResolveFuncCalls\ResolveFuncCalls;
use OneLang\One\Transforms\InferTypesPlugins\NullabilityCheckWithNot\NullabilityCheckWithNot;
use OneLang\One\Transforms\InferTypesPlugins\ResolveNewCall\ResolveNewCalls;
use OneLang\One\Transforms\InferTypesPlugins\ResolveElementAccess\ResolveElementAccess;
use OneLang\One\Ast\AstTypes\ClassType;

class InferTypesStage {
    const INVALID = 1;
    const FIELDS = 2;
    const PROPERTIES = 3;
    const METHODS = 4;
}

class InferTypes extends AstTransformer {
    protected $stage;
    public $plugins;
    public $contextInfoIdx = 0;
    
    function __construct() {
        parent::__construct("InferTypes");
        $this->plugins = array();
        $this->addPlugin(new BasicTypeInfer());
        $this->addPlugin(new ArrayAndMapLiteralTypeInfer());
        $this->addPlugin(new ResolveFieldAndPropertyAccess());
        $this->addPlugin(new ResolveMethodCalls());
        $this->addPlugin(new LambdaResolver());
        $this->addPlugin(new InferReturnType());
        $this->addPlugin(new ResolveEnumMemberAccess());
        $this->addPlugin(new TypeScriptNullCoalesce());
        $this->addPlugin(new InferForeachVarType());
        $this->addPlugin(new ResolveFuncCalls());
        $this->addPlugin(new NullabilityCheckWithNot());
        $this->addPlugin(new ResolveNewCalls());
        $this->addPlugin(new ResolveElementAccess());
    }
    
    function processLambda($lambda) {
        parent::visitMethodBase($lambda);
    }
    
    function processMethodBase($method) {
        parent::visitMethodBase($method);
    }
    
    function processBlock($block) {
        parent::visitBlock($block);
    }
    
    function processVariable($variable) {
        parent::visitVariable($variable);
    }
    
    function processStatement($stmt) {
        parent::visitStatement($stmt);
    }
    
    function processExpression($expr) {
        parent::visitExpression($expr);
    }
    
    function addPlugin($plugin) {
        $plugin->main = $this;
        $plugin->errorMan = $this->errorMan;
        $this->plugins[] = $plugin;
    }
    
    protected function visitVariableWithInitializer($variable) {
        if ($variable->type !== null && $variable->initializer !== null)
            $variable->initializer->setExpectedType($variable->type);
        
        $variable = parent::visitVariableWithInitializer($variable);
        
        if ($variable->type === null && $variable->initializer !== null)
            $variable->type = $variable->initializer->getType();
        
        return $variable;
    }
    
    protected function runTransformRound($expr) {
        if ($expr->actualType !== null)
            return $expr;
        
        $this->errorMan->currentNode = $expr;
        
        $transformers = array_values(array_filter($this->plugins, function ($x) use ($expr) { return $x->canTransform($expr); }));
        if (count($transformers) > 1)
            $this->errorMan->throw("Multiple transformers found: " . implode(", ", array_map(function ($x) { return $x->name; }, $transformers)));
        if (count($transformers) !== 1)
            return $expr;
        
        $plugin = $transformers[0];
        $this->contextInfoIdx++;
        $this->errorMan->lastContextInfo = "[" . $this->contextInfoIdx . "] running transform plugin \"" . $plugin->name . "\"";
        try {
            $newExpr = $plugin->transform($expr);
            // expression changed, restart the type infering process on the new expression
            if ($newExpr !== null)
                $newExpr->parentNode = $expr->parentNode;
            return $newExpr;
        } catch (Exception $e) {
            $this->errorMan->currentNode = $expr;
            $this->errorMan->throw("Error while running type transformation phase: " . $e);
            return $expr;
        }
    }
    
    protected function detectType($expr) {
        foreach ($this->plugins as $plugin) {
            if (!$plugin->canDetectType($expr))
                continue;
            $this->contextInfoIdx++;
            $this->errorMan->lastContextInfo = "[" . $this->contextInfoIdx . "] running type detection plugin \"" . $plugin->name . "\"";
            $this->errorMan->currentNode = $expr;
            try {
                if ($plugin->detectType($expr))
                    return true;
            } catch (Exception $e) {
                $this->errorMan->throw("Error while running type detection phase: " . $e);
            }
        }
        return false;
    }
    
    protected function visitExpression($expr) {
        $transExpr = $expr;
        while (true) {
            $newExpr = $this->runTransformRound($transExpr);
            if ($newExpr === $transExpr)
                break;
            $transExpr = $newExpr;
        }
        
        // if the plugin did not handle the expression, we use the default visit method
        if ($transExpr === $expr)
            $transExpr = parent::visitExpression($expr);
        
        if ($transExpr->actualType !== null)
            return $transExpr;
        
        $detectSuccess = $this->detectType($transExpr);
        
        if ($transExpr->actualType === null) {
            if ($detectSuccess)
                $this->errorMan->throw("Type detection failed, although plugin tried to handle it");
            else
                $this->errorMan->throw("Type detection failed: none of the plugins could resolve the type");
        }
        
        return $transExpr;
    }
    
    protected function visitStatement($stmt) {
        $this->currentStatement = $stmt;
        
        if ($stmt instanceof ReturnStatement && $stmt->expression !== null && $this->currentClosure instanceof Method && $this->currentClosure->returns !== null) {
            $returnType = $this->currentClosure->returns;
            if ($returnType instanceof ClassType && $returnType->decl === $this->currentFile->literalTypes->promise->decl && $this->currentClosure->async)
                $returnType = $returnType->typeArguments[0];
            $stmt->expression->setExpectedType($returnType);
        }
        
        foreach ($this->plugins as $plugin) {
            if ($plugin->handleStatement($stmt))
                return $stmt;
        }
        
        return parent::visitStatement($stmt);
    }
    
    protected function visitField($field) {
        if ($this->stage !== InferTypesStage::FIELDS)
            return;
        parent::visitField($field);
    }
    
    protected function visitProperty($prop) {
        if ($this->stage !== InferTypesStage::PROPERTIES)
            return;
        
        foreach ($this->plugins as $plugin) {
            if ($plugin->handleProperty($prop))
                return;
        }
        
        parent::visitProperty($prop);
    }
    
    protected function visitMethodBase($method) {
        if ($this->stage !== InferTypesStage::METHODS)
            return;
        
        foreach ($this->plugins as $plugin) {
            if ($plugin->handleMethod($method))
                return;
        }
        
        parent::visitMethodBase($method);
    }
    
    protected function visitLambda($lambda) {
        if ($lambda->actualType !== null)
            return $lambda;
        
        $prevClosure = $this->currentClosure;
        $this->currentClosure = $lambda;
        
        foreach ($this->plugins as $plugin) {
            if ($plugin->handleLambda($lambda))
                return $lambda;
        }
        
        $this->currentClosure = $prevClosure;
        parent::visitMethodBase($lambda);
        return $lambda;
    }
    
    function runPluginsOn($expr) {
        return $this->visitExpression($expr);
    }
    
    protected function visitClass($cls) {
        if ((@$cls->attributes["external"] ?? null) === "true")
            return;
        parent::visitClass($cls);
    }
    
    function visitFiles($files) {
        foreach (array(InferTypesStage::FIELDS, InferTypesStage::PROPERTIES, InferTypesStage::METHODS) as $stage) {
            $this->stage = $stage;
            foreach ($files as $file)
                $this->visitFile($file);
        }
    }
}
