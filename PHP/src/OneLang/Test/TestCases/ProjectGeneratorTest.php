<?php

namespace OneLang\Test\TestCases\ProjectGeneratorTest;

use OneLang\File\OneFile;
use OneLang\Test\TestCase\ITestCollection;
use OneLang\Test\TestCase\TestCase;
use OneLang\One\CompilerHelper\CompilerHelper;
use OneLang\One\Compiler\Compiler;
use OneLang\One\Compiler\ICompilerHooks;
use OneLang\Test\PackageStateCapture\PackageStateCapture;
use OneLang\Generator\ProjectGenerator\ProjectGenerator;

class StageExporter implements ICompilerHooks {
    public $stage = 0;
    public $artifactDir;
    public $compiler;
    
    function __construct($artifactDir, $compiler) {
        $this->artifactDir = $artifactDir;
        $this->compiler = $compiler;
    }
    
    function afterStage($stageName) {
        \OneLang\Core\console::log("Stage finished: " . $stageName);
        OneFile::writeText($this->artifactDir . "/stages/" . $this->stage . "_" . $stageName . ".txt", (new PackageStateCapture($this->compiler->projectPkg))->getSummary());
        $this->stage++;
    }
}

class ProjectGeneratorTest implements ITestCollection {
    public $name = "ProjectGeneratorTest";
    public $baseDir;
    
    function __construct($baseDir) {
        $this->baseDir = $baseDir;
    }
    
    function getTestCases() {
        return array(new TestCase("OneLang", function ($artifactDir) { return $this->compileOneLang($artifactDir); }));
    }
    
    function compileOneLang($artifactDir) {
        \OneLang\Core\console::log("Initalizing project generator...");
        $projGen = new ProjectGenerator($this->baseDir, $this->baseDir . "/xcompiled-src");
        $projGen->outDir = $artifactDir . "/output/";
        
        \OneLang\Core\console::log("Initalizing project for compiler...");
        $compiler = CompilerHelper::initProject($projGen->projectFile->name, $projGen->srcDir, $projGen->projectFile->sourceLang, null);
        $compiler->hooks = new StageExporter($artifactDir, $compiler);
        
        \OneLang\Core\console::log("Processing workspace...");
        $compiler->processWorkspace();
        
        \OneLang\Core\console::log("Generating project...");
        $projGen->generate();
    }
}
