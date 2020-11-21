<?php

namespace OneLang\One\IssueDetectors\CircularDependencyDetector;

use OneLang\One\Ast\AstTypes\ClassType;
use OneLang\One\Ast\Types\IInterface;
use OneLang\One\Ast\Types\IResolvedImportable;
use OneLang\One\Ast\Types\Package;
use OneLang\One\Ast\Types\SourceFile;
use OneLang\One\Ast\Types\Workspace;

class DetectionMode {
    const ALLIMPORTS = 1;
    const ALLINHERITENCE = 2;
    const BASECLASSESONLY = 3;
}

interface IGraphVisitor<TNode> {
    function processNode($node);
}

class GraphCycleDetector {
    public $nodeIsInPath;
    public $visitor;
    
    function __construct($visitor) {
        $this->visitor = $visitor;
        $this->nodeIsInPath = null;
    }
    
    function findCycles($nodes) {
        $this->nodeIsInPath = new \OneLang\Core\Map();
        foreach ($nodes as $node)
            $this->visitNode($node);
    }
    
    function visitNode($node) {
        if (!$this->nodeIsInPath->has($node)) {
            // untouched node
            $this->nodeIsInPath->set($node, true);
            $this->visitor->processNode($node);
            $this->nodeIsInPath->set($node, false);
            return false;
        }
        else
            // true = node used in current path = cycle
            // false = node was already scanned previously (not a cycle)
            return $this->nodeIsInPath->get($node);
    }
}

class CircularDependencyDetector implements IGraphVisitor<SourceFile> {
    public $detector;
    public $detectionMode;
    
    function __construct($detectionMode) {
        $this->detectionMode = $detectionMode;
        $this->detector = new GraphCycleDetector($this);
    }
    
    function processIntfs($file, $type, $intfs) {
        foreach ($intfs as $intf)
            foreach ($intf->getAllBaseInterfaces() as $baseIntf) {
                if ($baseIntf->parentFile !== $file && $this->detector->visitNode($baseIntf->parentFile))
                    \OneLang\Core\console::error("Circular dependency found in file '" . $file->exportScope->getId() . "': " . $type . " '" . $intf->name . "' inherited from '" . $baseIntf->name . "' (from '" . $baseIntf->parentFile->exportScope->getId() . "')");
            }
    }
    
    function processNode($file) {
        if ($this->detectionMode === DetectionMode::ALLIMPORTS)
            foreach ($file->imports as $imp)
                foreach ($imp->imports as $impSym) {
                    $impFile = ($impSym)->parentFile;
                    if ($this->detector->visitNode($impFile))
                        \OneLang\Core\console::error("Circular dependency found in file '" . $file->exportScope->getId() . "' via the import '" . $impSym->name . "' imported from '" . $impFile->exportScope->getId() . "'");
                }
        else if ($this->detectionMode === DetectionMode::ALLINHERITENCE) {
            $this->processIntfs($file, "class", $file->classes);
            $this->processIntfs($file, "interface", $file->interfaces);
        }
        else if ($this->detectionMode === DetectionMode::BASECLASSESONLY)
            foreach ($file->classes as $cls) {
                $baseClass = ($cls->baseClass)->decl;
                if ($baseClass->parentFile !== $file && $this->detector->visitNode($baseClass->parentFile))
                    \OneLang\Core\console::error("Circular dependency found in file '" . $file->exportScope->getId() . "': class '" . $cls->name . "' inherited from '" . $baseClass->name . "' (from '" . $baseClass->parentFile->exportScope->getId() . "')");
            }
    }
    
    function processPackage($pkg) {
        $this->detector->findCycles(array_values($pkg->files));
    }
    
    function processWorkspace($ws) {
        foreach (array_values($ws->packages) as $pkg)
            $this->processPackage($pkg);
    }
}
