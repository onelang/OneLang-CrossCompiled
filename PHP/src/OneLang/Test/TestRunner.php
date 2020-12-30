<?php

namespace OneLang\Test\TestRunner;

use OneLang\One\CompilerHelper\CompilerHelper;
use OneLang\Test\TestCase\ITestCollection;
use OneLang\Test\TestCase\TestCase;
use OneLang\Test\TestCases\BasicTests\BasicTests;
use OneLang\Test\TestCases\OneFileTests\OneFileTests;
use OneLang\Test\TestCases\ProjectGeneratorTest\ProjectGeneratorTest;

class TestRunner {
    public $tests;
    public $argsDict;
    public $outputDir;
    public $baseDir;
    public $args;
    
    function __construct($baseDir, $args) {
        $this->baseDir = $baseDir;
        $this->args = $args;
        $this->tests = array();
        $this->argsDict = Array();
        CompilerHelper::$baseDir = $baseDir . "/";
        $this->tests[] = new BasicTests();
        $this->tests[] = new OneFileTests($this->baseDir);
        $this->tests[] = new ProjectGeneratorTest($this->baseDir);
        
        $this->parseArgs();
        $this->outputDir = (@$this->argsDict["output-dir"] ?? null) ?? $baseDir . "/test/artifacts/TestRunner/" . "PHP";
    }
    
    function parseArgs() {
        for ($i = 0; $i < count($this->args) - 1; $i++) {
            if ((substr_compare($this->args[$i], "--", 0, strlen("--")) === 0))
                $this->argsDict[substr($this->args[$i], 2)] = $this->args[$i + 1];
        }
    }
    
    function runTests() {
        \OneLang\Core\console::log("### TestRunner -> START ###");
        
        foreach ($this->tests as $coll) {
            \OneLang\Core\console::log("### TestCollection -> " . $coll->name . " -> START ###");
            foreach ($coll->getTestCases() as $test) {
                \OneLang\Core\console::log("### TestCase -> " . $test->name . " -> START ###");
                try {
                    $outputDir = $this->outputDir . "/" . $coll->name . "/" . $test->name . "/";
                    \OneLang\Core\console::log("### TestCase -> " . $test->name . " -> OUTPUT-DIR -> " . $outputDir . " ###");
                    call_user_func($test->action, $outputDir);
                } catch (Exception $e) {
                    \OneLang\Core\console::log("### TestCase -> " . $test->name . " -> ERROR -> " . $e->message . " ###");
                }
                \OneLang\Core\console::log("### TestCase -> " . $test->name . " -> END ###");
            }
            \OneLang\Core\console::log("### TestCollection -> " . $coll->name . " -> END ###");
        }
        
        \OneLang\Core\console::log("### TestRunner -> END ###");
    }
}
