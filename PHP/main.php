<?php

namespace OneLang;

class AutoLoader {
    static function init() {
        spl_autoload_register(function ($class) {
            $path = explode("\\", $class);
            $clsName = array_pop($path);
            $fileName = implode("/", $path);
            //print("[OneLangAutoLoader] Class: $class -> $fileName.php\n");
            if (!file_exists("$fileName.php"))
                print("[OneLangAutoLoader] Class: $class -> $fileName.php\n");
            require_once("$fileName.php");
        });
    }
}
AutoLoader::init();

class console {
    static function log($data) {
        print($data . "\n");
    }

    static function error($data) {
        print("[ERROR] " . $data . "\n");
    }
}

class ArrayHelper {
    static function sortBy($arr, $func) {
        usort($arr, function($a, $b) use ($func) { return $func($a) - $func($b); });
        return $arr;
    }

    static function find($arr, $func) {
        foreach($arr as $item)
            if ($func($item))
                return $item;
        return null;
    }

    static function every($arr, $func) {
        foreach($arr as $i => $item)
            if (!$func($item, $i))
                return false;
        return true;
    }

    static function some($arr, $func) { return self::find($arr, $func) !== null; }
}

class RegExp {
    public $regex;
    public $lastIndex = 0;

    function __construct($pattern, $flags = null) {
        //print("construct pattern=$pattern\n");
        $pattern = str_replace("/", "\\/", $pattern);
        $this->regex = "/$pattern/A";
    }

    function exec($input) {
        //print("preg_match, pattern='{$this->regex}', offset={$this->lastIndex}, input='" . str_replace("\n", "\\n", substr($input, $this->lastIndex, 30)) . "'\n");
        if (preg_match($this->regex, $input, $matches, PREG_OFFSET_CAPTURE, $this->lastIndex) === 0)
            return null;

        //var_dump($matches);
        $this->lastIndex = $matches[0][1] + strlen($matches[0][0]);
        //print("new offset={$this->lastIndex}\n");
        $result = array_map(function($x){ return $x[0]; }, $matches);
        //var_dump($result);
        return $result;
    }
}

class Error extends \Exception { }

class Map {
    public $arr = array();

    function values() { return array_values($this->arr); }
    function has($key) { return array_key_exists($key, $this->arr); }
    function get($key) { return $this->arr[$key] ?? null; }
    function set($key, $value) { $this->arr[$key] = $value; }
    function delete($key) { unset($this->arr[$key]); }
}

class Array_ {
    static function from($arr) { return $arr; }
}

class Set {
    public $arr = array();

    function __construct($items = array()) {
        foreach($items as $item)
            $this->add($item);
    }

    function values() { return array_values($this->arr); }
    function has($item) { return in_array($item, $this->arr); }
    function add($item) { if(!$this->has($item)) $this->arr[] = $item; }
}

use Test\SelfTestRunner\SelfTestRunner;
use Generator\CsharpGenerator\CsharpGenerator;

$testRunner = new SelfTestRunner("../../");
$csharpGen = new CsharpGenerator();
try {
        $testRunner->runTest($csharpGen);
} catch(Error $e) {
    print($e->getMessage() . "\n");
}