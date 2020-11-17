<?php

namespace OneLang\One\Compiler;

use OneLang\One\Ast\Types\Workspace;
use OneLang\One\Ast\Types\Package;
use OneLang\One\Ast\Types\SourcePath;
use OneLang\One\Ast\Types\SourceFile;
use OneLang\One\Ast\Types\ExportedScope;
use OneLang\One\Ast\Types\LiteralTypes;
use OneLang\One\Ast\Types\Class_;
use OneLang\One\Ast\Types\ExportScopeRef;
use OneLang\Parsers\TypeScriptParser\TypeScriptParser2;
use OneLang\StdLib\PackageManager\PackageManager;
use OneLang\StdLib\PackagesFolderSource\PackagesFolderSource;
use OneLang\One\Transforms\FillParent\FillParent;
use OneLang\One\Transforms\FillAttributesFromTrivia\FillAttributesFromTrivia;
use OneLang\One\Transforms\ResolveGenericTypeIdentifiers\ResolveGenericTypeIdentifiers;
use OneLang\One\Transforms\ResolveUnresolvedTypes\ResolveUnresolvedTypes;
use OneLang\One\Transforms\ResolveImports\ResolveImports;
use OneLang\One\Transforms\ConvertToMethodCall\ConvertToMethodCall;
use OneLang\One\Transforms\ResolveIdentifiers\ResolveIdentifiers;
use OneLang\One\Transforms\InstanceOfImplicitCast\InstanceOfImplicitCast;
use OneLang\One\Transforms\DetectMethodCalls\DetectMethodCalls;
use OneLang\One\Transforms\InferTypes\InferTypes;
use OneLang\One\Transforms\CollectInheritanceInfo\CollectInheritanceInfo;
use OneLang\One\Transforms\FillMutabilityInfo\FillMutabilityInfo;
use OneLang\One\AstTransformer\AstTransformer;
use OneLang\One\ITransformer\ITransformer;
use OneLang\One\Transforms\LambdaCaptureCollector\LambdaCaptureCollector;

interface ICompilerHooks {
    function afterStage($stageName);
}

class Compiler {
    public $pacMan;
    public $workspace;
    public $nativeFile;
    public $nativeExports;
    public $projectPkg;
    public $hooks;
    
    function __construct()
    {
        $this->pacMan = null;
        $this->workspace = null;
        $this->nativeFile = null;
        $this->nativeExports = null;
        $this->projectPkg = null;
        $this->hooks = null;
    }
    
    function init($packagesDir) {
        $this->pacMan = new PackageManager(new PackagesFolderSource($packagesDir));
        $this->pacMan->loadAllCached();
    }
    
    function getTransformers($forDeclarationFile) {
        $transforms = array();
        if ($forDeclarationFile) {
            $transforms[] = new FillParent();
            $transforms[] = new FillAttributesFromTrivia();
            $transforms[] = new ResolveImports($this->workspace);
            $transforms[] = new ResolveGenericTypeIdentifiers();
            $transforms[] = new ResolveUnresolvedTypes();
            $transforms[] = new FillMutabilityInfo();
        }
        else {
            $transforms[] = new FillParent();
            $transforms[] = new FillAttributesFromTrivia();
            $transforms[] = new ResolveImports($this->workspace);
            $transforms[] = new ResolveGenericTypeIdentifiers();
            $transforms[] = new ConvertToMethodCall();
            $transforms[] = new ResolveUnresolvedTypes();
            $transforms[] = new ResolveIdentifiers();
            $transforms[] = new InstanceOfImplicitCast();
            $transforms[] = new DetectMethodCalls();
            $transforms[] = new InferTypes();
            $transforms[] = new CollectInheritanceInfo();
            $transforms[] = new FillMutabilityInfo();
            $transforms[] = new LambdaCaptureCollector();
        }
        return $transforms;
    }
    
    function setupNativeResolver($content) {
        $this->nativeFile = TypeScriptParser2::parseFile($content);
        $this->nativeExports = Package::collectExportsFromFile($this->nativeFile, true);
        foreach ($this->getTransformers(true) as $trans)
            $trans->visitFiles(array($this->nativeFile));
    }
    
    function newWorkspace($pkgName = "@") {
        $this->workspace = new Workspace();
        foreach ($this->pacMan->interfacesPkgs as $intfPkg) {
            $libName = $intfPkg->interfaceYaml->vendor . "." . $intfPkg->interfaceYaml->name . "-v" . $intfPkg->interfaceYaml->version;
            $this->addInterfacePackage($libName, $intfPkg->definition);
        }
        
        $this->projectPkg = new Package($pkgName, false);
        $this->workspace->addPackage($this->projectPkg);
    }
    
    function addInterfacePackage($libName, $definitionFileContent) {
        $libPkg = new Package($libName, true);
        $file = TypeScriptParser2::parseFile($definitionFileContent, new SourcePath($libPkg, Package::$INDEX));
        $this->setupFile($file);
        $libPkg->addFile($file, true);
        $this->workspace->addPackage($libPkg);
    }
    
    function setupFile($file) {
        $file->addAvailableSymbols($this->nativeExports->getAllExports());
        $file->literalTypes = new LiteralTypes(($file->availableSymbols->get("TsBoolean"))->type, ($file->availableSymbols->get("TsNumber"))->type, ($file->availableSymbols->get("TsString"))->type, ($file->availableSymbols->get("RegExp"))->type, ($file->availableSymbols->get("TsArray"))->type, ($file->availableSymbols->get("TsMap"))->type, ($file->availableSymbols->get("Error"))->type, ($file->availableSymbols->get("Promise"))->type);
        $file->arrayTypes = array(($file->availableSymbols->get("TsArray"))->type, ($file->availableSymbols->get("IterableIterator"))->type, ($file->availableSymbols->get("RegExpExecArray"))->type, ($file->availableSymbols->get("TsString"))->type, ($file->availableSymbols->get("Set"))->type);
    }
    
    function addProjectFile($fn, $content) {
        $file = TypeScriptParser2::parseFile($content, new SourcePath($this->projectPkg, preg_replace("/\\.ts$/", "", $fn)));
        $this->setupFile($file);
        $this->projectPkg->addFile($file);
    }
    
    function processFiles($files) {
        foreach ($this->getTransformers(false) as $trans) {
            $trans->visitFiles($files);
            if ($this->hooks !== null)
                $this->hooks->afterStage($trans->name);
        }
    }
    
    function processWorkspace() {
        foreach (array_values(array_filter(array_values($this->workspace->packages), function ($x) { return $x->definitionOnly; })) as $pkg)
            foreach ($this->getTransformers(true) as $trans)
                $trans->visitFiles(array_values($pkg->files));
        
        $this->processFiles(array_values($this->projectPkg->files));
    }
}
