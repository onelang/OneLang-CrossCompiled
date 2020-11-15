<?php

namespace One\Transforms\ResolveImports;

use One\Ast\Types\Workspace;
use One\Ast\Types\UnresolvedImport;
use One\Ast\Types\SourceFile;
use One\Ast\Types\Package;
use One\AstTransformer\AstTransformer;

class ResolveImports extends AstTransformer {
    public $workspace;
    
    function __construct($workspace) {
        parent::__construct("ResolveImports");
        $this->workspace = $workspace;
    }
    
    function visitFile($sourceFile) {
        ResolveImports::processFile($this->workspace, $sourceFile);
    }
    
    static function processFile($ws, $file) {
        foreach ($file->imports as $imp) {
            $impPkg = $ws->getPackage($imp->exportScope->packageName);
            $scope = $impPkg->getExportedScope($imp->exportScope->scopeName);
            $imp->imports = $imp->importAll ? $scope->getAllExports() : array_map(function ($x) use ($scope) { return $x instanceof UnresolvedImport ? $scope->getExport($x->name) : $x; }, $imp->imports);
            $file->addAvailableSymbols($imp->imports);
        }
    }
    
    static function processWorkspace($ws) {
        foreach (array_values($ws->packages) as $pkg)
            foreach (array_values($pkg->files) as $file)
                ResolveImports::processFile($ws, $file);
    }
}
