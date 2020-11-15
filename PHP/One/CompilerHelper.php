<?php

namespace One\CompilerHelper;

use onepkg\OneFile\OneFile;
use One\Compiler\Compiler;

class CompilerHelper {
    public static $baseDir = "./";
    
    static function initProject($projectName, $sourceDir, $lang = "ts", $packagesDir = null) {
        if ($lang !== "ts")
            throw new \OneLang\Error("Only typescript is supported.");
        
        $compiler = new Compiler();
        $compiler->init($packagesDir ?? CompilerHelper::$baseDir . "packages/");
        $compiler->setupNativeResolver(OneFile::readText(CompilerHelper::$baseDir . "langs/NativeResolvers/typescript.ts"));
        $compiler->newWorkspace($projectName);
        
        foreach (array_values(array_filter(OneFile::listFiles($sourceDir, true), function ($x) { return substr_compare($x, ".ts", strlen($x) - strlen(".ts"), strlen(".ts")) === 0; })) as $file)
            $compiler->addProjectFile($file, OneFile::readText($sourceDir . "/" . $file));
        
        return $compiler;
    }
}
