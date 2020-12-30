<?php

namespace OneLang\Test\TestCases\OneFileTests;

use OneLang\File\OneFile;
use OneLang\Test\TestCase\ITestCollection;
use OneLang\Test\TestCase\SyncTestCase;
use OneLang\Test\TestCase\TestCase;

class OneFileTests implements ITestCollection {
    public $name = "OneFileTests";
    public $baseDir;
    
    function __construct($baseDir) {
        $this->baseDir = $baseDir;
    }
    
    function listXCompiledNativeSources() {
        \OneLang\Core\console::log(implode("\n", OneFile::listFiles($this->baseDir . "/xcompiled-src/native", true)));
    }
    
    function getTestCases() {
        return array(new SyncTestCase("ListXCompiledNativeSources", function ($_) { return $this->listXCompiledNativeSources(); }));
    }
}
