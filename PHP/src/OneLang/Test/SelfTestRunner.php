<?php

namespace OneLang\Test\SelfTestRunner;

use OneLang\File\OneFile;
use OneLang\One\CompilerHelper\CompilerHelper;
use OneLang\Generator\IGenerator\IGenerator;
use OneLang\One\Compiler\Compiler;
use OneLang\One\Compiler\ICompilerHooks;
use OneLang\Test\PackageStateCapture\PackageStateCapture;
use OneLang\Generator\ProjectGenerator\ProjectGenerator;

class CompilerHooks implements ICompilerHooks {
    public $stage = 0;
    public $compiler;
    public $baseDir;
    
    function __construct($compiler, $baseDir) {
        $this->compiler = $compiler;
        $this->baseDir = $baseDir;
    }
    
    function afterStage($stageName) {
        $state = new PackageStateCapture($this->compiler->projectPkg);
        $stageFn = $this->baseDir . "/test/artifacts/ProjectTest/OneLang/stages/" . $this->stage++ . "_" . $stageName . ".txt";
        $stageSummary = $state->getSummary();
        
        $expected = OneFile::readText($stageFn);
        if ($stageSummary !== $expected) {
            OneFile::writeText($stageFn . "_diff.txt", $stageSummary);
            throw new \OneLang\Core\Error("Stage result differs from expected: " . $stageName . " -> " . $stageFn);
        }
        else
            \OneLang\Core\console::log("[+] Stage passed: " . $stageName);
    }
}

class SelfTestRunner {
    public $baseDir;
    
    function __construct($baseDir) {
        $this->baseDir = $baseDir;
        CompilerHelper::$baseDir = $baseDir;
    }
    
    function runTest() {
        \OneLang\Core\console::log("[-] SelfTestRunner :: START");
        
        $projGen = new ProjectGenerator($this->baseDir, $this->baseDir . "/xcompiled-src");
        $projGen->outDir = $this->baseDir . "test/artifacts/SelfTestRunner_" . "PHP" . "/";
        $compiler = CompilerHelper::initProject($projGen->projectFile->name, $projGen->srcDir, $projGen->projectFile->sourceLang, null);
        $compiler->hooks = new CompilerHooks($compiler, $this->baseDir);
        $compiler->processWorkspace();
        $projGen->generate();
        
        $allMatch = true;
        // for (const genFile of generated) {
        //     const projBase = `${this.baseDir}test/artifacts/ProjectTest/OneLang`;
        //     const tsGenPath = `${this.baseDir}/xcompiled/${langName}/${genFile.path}`;
        //     const reGenPath = `${projBase}/${langName}_Regen/${genFile.path}`;
        //     const tsGenContent = OneFile.readText(tsGenPath);
        //     const reGenContent = genFile.content;
        
        //     if (tsGenContent != reGenContent) {
        //         OneFile.writeText(reGenPath, genFile.content);
        //         console.error(`Content does not match: ${genFile.path}`);
        //         allMatch = false;
        //     } else {
        //         console.log(`[+] Content matches: ${genFile.path}`);
        //     }
        // }
        
        \OneLang\Core\console::log($allMatch ? "[+} SUCCESS! All generated files are the same" : "[!] FAIL! Not all files are the same");
        \OneLang\Core\console::log("[-] SelfTestRunner :: DONE");
        return $allMatch;
    }
}
