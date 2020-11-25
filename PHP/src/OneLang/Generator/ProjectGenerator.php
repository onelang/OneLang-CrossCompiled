<?php

namespace OneLang\Generator\ProjectGenerator;

use OneLang\File\OneFile;
use OneLang\Yaml\OneYaml;
use OneLang\Json\OneJson;
use OneLang\Generator\IGenerator\IGenerator;
use OneLang\Generator\JavaGenerator\JavaGenerator;
use OneLang\Generator\CsharpGenerator\CsharpGenerator;
use OneLang\Generator\PythonGenerator\PythonGenerator;
use OneLang\Generator\PhpGenerator\PhpGenerator;
use OneLang\One\CompilerHelper\CompilerHelper;
use OneLang\StdLib\PackageManager\ImplementationPackage;
use OneLang\VM\Values\ArrayValue;
use OneLang\VM\Values\IVMValue;
use OneLang\VM\Values\ObjectValue;
use OneLang\VM\Values\StringValue;
use OneLang\Template\TemplateParser\TemplateParser;
use OneLang\Generator\TemplateFileGeneratorPlugin\TemplateFileGeneratorPlugin;
use OneLang\VM\ExprVM\VMContext;

class ProjectTemplateMeta {
    public $language;
    public $destinationDir;
    public $packageDir;
    public $templateFiles;
    
    function __construct($language, $destinationDir, $packageDir, $templateFiles) {
        $this->language = $language;
        $this->destinationDir = $destinationDir;
        $this->packageDir = $packageDir;
        $this->templateFiles = $templateFiles;
    }
    
    static function fromYaml($obj) {
        return new ProjectTemplateMeta($obj->str("language"), $obj->str("destination-dir"), $obj->str("package-dir"), $obj->strArr("template-files"));
    }
}

class ProjectTemplate {
    public $meta;
    public $srcFiles;
    public $templateDir;
    
    function __construct($templateDir) {
        $this->templateDir = $templateDir;
        $this->meta = ProjectTemplateMeta::fromYaml(OneYaml::load(OneFile::readText($templateDir . "/index.yaml")));
        $this->srcFiles = OneFile::listFiles($templateDir . "/src", true);
    }
    
    function generate($dstDir, $model) {
        foreach ($this->srcFiles as $fn) {
            $srcFn = $this->templateDir . "/src/" . $fn;
            $dstFn = $dstDir . "/" . $fn;
            if (in_array($fn, $this->meta->templateFiles)) {
                $tmpl = (new TemplateParser(OneFile::readText($srcFn)))->parse();
                $dstFile = $tmpl->format(new VMContext($model));
                OneFile::writeText($dstFn, $dstFile);
            }
            else
                OneFile::copy($srcFn, $dstFn);
        }
    }
}

class ProjectDependency {
    public $name;
    public $version;
    
    function __construct($name, $version) {
        $this->name = $name;
        $this->version = $version;
    }
}

class OneProjectFile {
    public $name;
    public $dependencies;
    public $sourceDir;
    public $sourceLang;
    public $nativeSourceDir;
    public $outputDir;
    public $projectTemplates;
    
    function __construct($name, $dependencies, $sourceDir, $sourceLang, $nativeSourceDir, $outputDir, $projectTemplates) {
        $this->name = $name;
        $this->dependencies = $dependencies;
        $this->sourceDir = $sourceDir;
        $this->sourceLang = $sourceLang;
        $this->nativeSourceDir = $nativeSourceDir;
        $this->outputDir = $outputDir;
        $this->projectTemplates = $projectTemplates;
    }
    
    static function fromJson($json) {
        return new OneProjectFile($json->get("name")->asString(), array_map(function ($dep) { return new ProjectDependency($dep->get("name")->asString(), $dep->get("version")->asString()); }, array_map(function ($dep) { return $dep->asObject(); }, $json->get("dependencies")->getArrayItems())), $json->get("sourceDir")->asString(), $json->get("sourceLang")->asString(), $json->get("nativeSourceDir")->asString(), $json->get("outputDir")->asString(), array_map(function ($x) { return $x->asString(); }, $json->get("projectTemplates")->getArrayItems()));
    }
}

class ProjectGenerator {
    public $projectFile;
    public $srcDir;
    public $outDir;
    public $baseDir;
    public $projDir;
    
    function __construct($baseDir, $projDir) {
        $this->baseDir = $baseDir;
        $this->projDir = $projDir;
        $this->projectFile = null;
        $this->projectFile = OneProjectFile::fromJson(OneJson::parse(OneFile::readText($projDir . "/one.json"))->asObject());
        $this->srcDir = $this->projDir . "/" . $this->projectFile->sourceDir;
        $this->outDir = $this->projDir . "/" . $this->projectFile->outputDir;
    }
    
    function generate() {
        // copy native source codes from one project
        $nativeSrcDir = $this->projDir . "/" . $this->projectFile->nativeSourceDir;
        foreach (OneFile::listFiles($nativeSrcDir, true) as $fn)
            OneFile::copy($nativeSrcDir . "/" . $fn, $this->outDir . "/" . $fn);
        
        $generators = array(new JavaGenerator(), new CsharpGenerator(), new PythonGenerator(), new PhpGenerator());
        foreach ($this->projectFile->projectTemplates as $tmplName) {
            $compiler = CompilerHelper::initProject($this->projectFile->name, $this->srcDir, $this->projectFile->sourceLang, null);
            $compiler->processWorkspace();
            
            $projTemplate = new ProjectTemplate($this->baseDir . "/project-templates/" . $tmplName);
            $langId = $projTemplate->meta->language;
            $generator = \OneLang\Core\ArrayHelper::find($generators, function ($x) use ($langId) { return strtolower($x->getLangName()) === $langId; });
            $langName = $generator->getLangName();
            $outDir = $this->outDir . "/" . $langName;
            
            foreach ($generator->getTransforms() as $trans)
                $trans->visitFiles(array_values($compiler->projectPkg->files));
                
            // copy implementation native sources
            $oneDeps = array();
            $nativeDeps = Array();
            foreach ($this->projectFile->dependencies as $dep) {
                $impl = \OneLang\Core\ArrayHelper::find($compiler->pacMan->implementationPkgs, function ($x) use ($dep) { return $x->content->id->name === $dep->name; });
                $oneDeps[] = $impl;
                $langData = @$impl->implementationYaml->languages[$langId] ?? null;
                if ($langData === null)
                    continue;
                
                foreach ($langData->nativeDependencies ?? array() as $natDep)
                    $nativeDeps[$natDep->name] = $natDep->version;
                
                if ($langData->nativeSrcDir !== null) {
                    if ($projTemplate->meta->packageDir === null)
                        throw new \OneLang\Core\Error("Package directory is empty in project template!");
                    $srcDir = $langData->nativeSrcDir . (substr_compare($langData->nativeSrcDir, "/", strlen($langData->nativeSrcDir) - strlen("/"), strlen("/")) === 0 ? "" : "/");
                    $dstDir = $outDir . "/" . $projTemplate->meta->packageDir . "/" . $langData->packageDir ?? $impl->content->id->name;
                    $depFiles = array_map(function ($x) use ($srcDir) { return substr($x, strlen($srcDir)); }, array_values(array_filter(array_keys($impl->content->files), function ($x) use ($srcDir) { return substr_compare($x, $srcDir, 0, strlen($srcDir)) === 0; })));
                    foreach ($depFiles as $fn)
                        OneFile::writeText($dstDir . "/" . $fn, @$impl->content->files[$srcDir . $fn] ?? null);
                }
                
                if ($langData->generatorPlugins !== null)
                    foreach ($langData->generatorPlugins as $genPlugFn)
                        $generator->addPlugin(new TemplateFileGeneratorPlugin($generator, @$impl->content->files[$genPlugFn] ?? null));
            }
            
            // generate cross compiled source code
            \OneLang\Core\console::log("Generating " . $langName . " code...");
            $files = $generator->generate($compiler->projectPkg);
            foreach ($files as $file)
                OneFile::writeText($outDir . "/" . $projTemplate->meta->destinationDir ?? "" . "/" . $file->path, $file->content);
            
            // generate files from project template
            $model = new ObjectValue(Array(
                "dependencies" => new ArrayValue(array_map(function ($name) use ($nativeDeps) { return new ObjectValue(Array(
                    "name" => new StringValue($name),
                    "version" => new StringValue(@$nativeDeps[$name] ?? null)
                )); }, array_keys($nativeDeps))),
                "onepackages" => new ArrayValue(array_map(function ($dep) { return new ObjectValue(Array(
                    "vendor" => new StringValue($dep->implementationYaml->vendor),
                    "id" => new StringValue($dep->implementationYaml->name)
                )); }, $oneDeps))
            ));
            $projTemplate->generate($outDir, $model);
        }
    }
}
