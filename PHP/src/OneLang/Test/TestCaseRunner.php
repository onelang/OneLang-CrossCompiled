<?php

namespace OneLang\Test\TestCaseRunner;

use OneLang\Test\TestCase\ITestCollection;
use OneLang\Test\TestCase\TestCase;
use OneLang\Test\TestCases\BasicTests\BasicTests;
use OneLang\Test\TestCases\ProjectGeneratorTest\ProjectGeneratorTest;

class TestResult {
    public $collectionName;
    public $testName;
    public $result;
    public $error;
    
    function __construct($collectionName, $testName, $result, $error) {
        $this->collectionName = $collectionName;
        $this->testName = $testName;
        $this->result = $result;
        $this->error = $error;
    }
}

class TestCaseRunner {
    public $tests;
    public $baseDir;
    
    function __construct($baseDir) {
        $this->baseDir = $baseDir;
        $this->tests = array();
        $this->tests[] = new BasicTests();
        $this->tests[] = new ProjectGeneratorTest($this->baseDir);
    }
    
    function runTests() {
        $results = array();
        \OneLang\Core\console::log("### TestCaseRunner -> START ###");
        
        foreach ($this->tests as $coll) {
            \OneLang\Core\console::log("### Collection -> " . $coll->name . " -> START ###");
            foreach ($coll->getTestCases() as $test) {
                \OneLang\Core\console::log("### TestCase -> " . $test->name . " -> START ###");
                try {
                    $artifactDir = $this->baseDir . "/test/artifacts/" . $coll->name . "/" . $test->name . "/" . "PHP" . "/";
                    $result = call_user_func($test->action, $artifactDir);
                    \OneLang\Core\console::log("### TestCase -> " . $test->name . " -> RESULT: " . json_encode($result, JSON_UNESCAPED_SLASHES) . " ###");
                    $results[] = new TestResult($coll->name, $test->name, $result, null);
                } catch (Exception $e) {
                    \OneLang\Core\console::log("### TestCase -> " . $test->name . " -> ERROR: " . $e . " ###");
                    $results[] = new TestResult($coll->name, $test->name, null, ($e)->message);
                }
                \OneLang\Core\console::log("### TestCase -> " . $test->name . " -> END ###");
            }
            \OneLang\Core\console::log("### Collection -> " . $coll->name . " -> END ###");
        }
        
        \OneLang\Core\console::log("### TestCaseRunner -> SAVE ###");
        OneFile::writeText($this->baseDir . "/test/artifacts/TestCaseRunner_results.json", json_encode($results, JSON_UNESCAPED_SLASHES));
        
        \OneLang\Core\console::log("### TestCaseRunner -> END ###");
        return $results;
    }
}
