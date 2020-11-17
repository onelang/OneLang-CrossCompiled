<?php

namespace OneLang\Test\SelfTestRunner;

use OneFile\OneFile;
use OneLang\One\CompilerHelper\CompilerHelper;
use OneLang\Generator\IGenerator\IGenerator;
use OneLang\One\Compiler\Compiler;
use OneLang\One\Compiler\ICompilerHooks;
use OneLang\Test\PackageStateCapture\PackageStateCapture;

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
        $stageFn = $this->baseDir . "/test/artifacts/ProjectTest/OneLang/stages/" . $this->stage . "_" . $stageName . ".txt";
        $this->stage++;
        $stageSummary = $state->getSummary();
        $expected = OneFile::readText($stageFn);
        if ($stageSummary !== $expected) {
            OneFile::writeText($stageFn . "_diff.txt", $stageSummary);
            throw new \OneCore\Error("Stage result differs from expected: " . $stageName . " -> " . $stageFn);
        }
        else
            \OneCore\console::log("[+] Stage passed: " . $stageName);
    }
}

class SelfTestRunner {
    public $baseDir;
    
    function __construct($baseDir) {
        $this->baseDir = $baseDir;
        CompilerHelper::$baseDir = $baseDir;
    }
    
    function runTest($generator) {
        \OneCore\console::log("[-] SelfTestRunner :: START");
        $compiler = CompilerHelper::initProject("OneLang", $this->baseDir . "src/");
        $compiler->hooks = new CompilerHooks($compiler, $this->baseDir);
        $compiler->processWorkspace();
        $generated = $generator->generate($compiler->projectPkg);
        
        $langName = $generator->getLangName();
        $ext = "." . $generator->getExtension();
        
        $allMatch = true;
        foreach ($generated as $genFile) {
            $projBase = $this->baseDir . "test/artifacts/ProjectTest/OneLang";
            $tsGenPath = $this->baseDir . "/xcompiled/" . $langName . "/" . $genFile->path;
            $reGenPath = $projBase . "/" . $langName . "_Regen/" . $genFile->path;
            $tsGenContent = OneFile::readText($tsGenPath);
            $reGenContent = $genFile->content;
            
            if ($tsGenContent !== $reGenContent) {
                OneFile::writeText($reGenPath, $genFile->content);
                \OneCore\console::error("Content does not match: " . $genFile->path);
                $allMatch = false;
            }
            else
                \OneCore\console::log("[+] Content matches: " . $genFile->path);
        }
        
        \OneCore\console::log($allMatch ? "[+} SUCCESS! All generated files are the same" : "[!] FAIL! Not all files are the same");
        \OneCore\console::log("[-] SelfTestRunner :: DONE");
        return $allMatch;
    }
}
