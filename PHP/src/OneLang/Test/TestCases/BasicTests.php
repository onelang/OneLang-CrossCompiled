<?php

namespace OneLang\Test\TestCases\BasicTests;

use OneLang\Test\TestCase\ITestCollection;
use OneLang\Test\TestCase\SyncTestCase;
use OneLang\Test\TestCase\TestCase;

class BasicTests implements ITestCollection {
    public $name = "BasicTests";
    
    function printsOneLineString() {
        \OneLang\Core\console::log("Hello World!");
    }
    
    function printsMultiLineString() {
        \OneLang\Core\console::log("Hello\nWorld!");
    }
    
    function printsTwoStrings() {
        \OneLang\Core\console::log("Hello");
        \OneLang\Core\console::log("HelloWorld!");
    }
    
    function printsEscapedString() {
        \OneLang\Core\console::log("dollar: \$");
        \OneLang\Core\console::log("backslash: \\");
        \OneLang\Core\console::log("newline: \n");
        \OneLang\Core\console::log("escaped newline: \\n");
        \OneLang\Core\console::log("dollar after escape: \\\$");
    }
    
    function printsEscapedTemplateString() {
        \OneLang\Core\console::log("dollar: \$");
        \OneLang\Core\console::log("backslash: \\");
        \OneLang\Core\console::log("newline: \n");
        \OneLang\Core\console::log("escaped newline: \\n");
        \OneLang\Core\console::log("dollar after escape: \\\$");
    }
    
    function regexReplace() {
        \OneLang\Core\console::log(preg_replace("/\\$/", "x", "a\$b\$c"));
        \OneLang\Core\console::log(preg_replace("/x/", "$", "Test1: xx"));
        \OneLang\Core\console::log(preg_replace("/y/", "\\\\", "Test2: yy"));
        \OneLang\Core\console::log(preg_replace("/z/", "\\\\$", "Test3: zz"));
    }
    
    function json() {
        \OneLang\Core\console::log(json_encode(null, JSON_UNESCAPED_SLASHES));
        \OneLang\Core\console::log(json_encode(true, JSON_UNESCAPED_SLASHES));
        \OneLang\Core\console::log(json_encode(false, JSON_UNESCAPED_SLASHES));
        \OneLang\Core\console::log(json_encode("string", JSON_UNESCAPED_SLASHES));
        \OneLang\Core\console::log(json_encode(0.123, JSON_UNESCAPED_SLASHES));
        \OneLang\Core\console::log(json_encode(123, JSON_UNESCAPED_SLASHES));
        \OneLang\Core\console::log(json_encode(123.456, JSON_UNESCAPED_SLASHES));
        \OneLang\Core\console::log(json_encode(Array("a" => "b"), JSON_UNESCAPED_SLASHES));
        \OneLang\Core\console::log(json_encode("\$", JSON_UNESCAPED_SLASHES));
        \OneLang\Core\console::log(json_encode("A \\ B", JSON_UNESCAPED_SLASHES));
        \OneLang\Core\console::log(json_encode("A \\\\ B", JSON_UNESCAPED_SLASHES));
    }
    
    function phpGeneratorBugs() {
        \OneLang\Core\console::log("Step1: " . "A \$ B");
        \OneLang\Core\console::log("Step2: " . json_encode("A \$ B", JSON_UNESCAPED_SLASHES));
        \OneLang\Core\console::log("Step3: " . preg_replace("/\\$/", "\\\\$", json_encode("A \$ B", JSON_UNESCAPED_SLASHES)));
        \OneLang\Core\console::log("Step3 w/o JSON: " . preg_replace("/\\$/", "\\\\$", "A \$ B"));
    }
    
    function getTestCases() {
        return array(new SyncTestCase("PrintsOneLineString", function ($_) { return $this->printsOneLineString(); }), new SyncTestCase("PrintsMultiLineString", function ($_) { return $this->printsMultiLineString(); }), new SyncTestCase("PrintsTwoStrings", function ($_) { return $this->printsTwoStrings(); }), new SyncTestCase("PrintsEscapedString", function ($_) { return $this->printsEscapedString(); }), new SyncTestCase("PrintsEscapedTemplateString", function ($_) { return $this->printsEscapedTemplateString(); }), new SyncTestCase("RegexReplace", function ($_) { return $this->regexReplace(); }), new SyncTestCase("Json", function ($_) { return $this->json(); }), new SyncTestCase("PhpGeneratorBugs", function ($_) { return $this->phpGeneratorBugs(); }));
    }
}
