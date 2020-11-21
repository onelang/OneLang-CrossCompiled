<?php

namespace OneLang\Generator\ProjectGenerator;

use OneLang\File\OneFile;
use OneLang\Yaml\OneYaml;
use OneLang\Json\OneJson;
use OneLang\Parsers\Common\Reader\Reader;
use OneLang\One\Ast\Expressions\Expression;
use OneLang\One\Ast\Expressions\Identifier;
use OneLang\One\Ast\Expressions\PropertyAccessExpression;
use OneLang\One\Compiler\Compiler;
use OneLang\Generator\IGenerator\IGenerator;
use OneLang\Parsers\Common\ExpressionParser\ExpressionParser;
use OneLang\Utils\TSOverviewGenerator\TSOverviewGenerator;
use OneLang\Generator\JavaGenerator\JavaGenerator;
use OneLang\Generator\CsharpGenerator\CsharpGenerator;
use OneLang\Generator\PythonGenerator\PythonGenerator;
use OneLang\Generator\PhpGenerator\PhpGenerator;
use OneLang\One\CompilerHelper\CompilerHelper;
use OneLang\StdLib\PackageManager\ImplementationPackage;

interface IVMValue {
    
}

interface ITemplateNode {
    function format($model);
}

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

class ObjectValue implements IVMValue {
    public $props;
    
    function __construct($props) {
        $this->props = $props;
    }
}

class StringValue implements IVMValue {
    public $value;
    
    function __construct($value) {
        $this->value = $value;
    }
}

class ArrayValue implements IVMValue {
    public $items;
    
    function __construct($items) {
        $this->items = $items;
    }
}

class TemplateBlock implements ITemplateNode {
    public $items;
    
    function __construct($items) {
        $this->items = $items;
    }
    
    function format($model) {
        return implode("", array_map(function ($x) use ($model) { return $x->format($model); }, $this->items));
    }
}

class LiteralNode implements ITemplateNode {
    public $value;
    
    function __construct($value) {
        $this->value = $value;
    }
    
    function format($model) {
        return $this->value;
    }
}

class ExprVM {
    public $model;
    
    function __construct($model) {
        $this->model = $model;
    }
    
    static function propAccess($obj, $propName) {
        if (!($obj instanceof ObjectValue))
            throw new \OneLang\Core\Error("You can only access a property of an object!");
        if (!(array_key_exists($propName, ($obj)->props)))
            throw new \OneLang\Core\Error("Property '" . $propName . "' does not exists on this object!");
        return @($obj)->props[$propName] ?? null;
    }
    
    function evaluate($expr) {
        if ($expr instanceof Identifier)
            return ExprVM::propAccess($this->model, $expr->text);
        else if ($expr instanceof PropertyAccessExpression) {
            $objValue = $this->evaluate($expr->object);
            return ExprVM::propAccess($objValue, $expr->propertyName);
        }
        else
            throw new \OneLang\Core\Error("Unsupported expression!");
    }
}

class ExpressionNode implements ITemplateNode {
    public $expr;
    
    function __construct($expr) {
        $this->expr = $expr;
    }
    
    function format($model) {
        $result = (new ExprVM($model))->evaluate($this->expr);
        if ($result instanceof StringValue)
            return $result->value;
        else
            throw new \OneLang\Core\Error("ExpressionNode (" . TSOverviewGenerator::$preview->expr($this->expr) . ") return a non-string result!");
    }
}

class ForNode implements ITemplateNode {
    public $variableName;
    public $itemsExpr;
    public $body;
    
    function __construct($variableName, $itemsExpr, $body) {
        $this->variableName = $variableName;
        $this->itemsExpr = $itemsExpr;
        $this->body = $body;
    }
    
    function format($model) {
        $items = (new ExprVM($model))->evaluate($this->itemsExpr);
        if (!($items instanceof ArrayValue))
            throw new \OneLang\Core\Error("ForNode items (" . TSOverviewGenerator::$preview->expr($this->itemsExpr) . ") return a non-array result!");
        
        $result = "";
        foreach (($items)->items as $item) {
            $model->props[$this->variableName] = $item;
            $result .= $this->body->format($model);
        }
        /* unset @$model->props[$this->variableName] ?? null; */
        return $result;
    }
}

class TemplateParser {
    public $reader;
    public $exprParser;
    public $template;
    
    function __construct($template) {
        $this->template = $template;
        $this->reader = new Reader($template);
        $this->exprParser = new ExpressionParser($this->reader);
    }
    
    function parseBlock() {
        $items = array();
        while (!$this->reader->get_eof()) {
            if ($this->reader->peekToken("{{/"))
                break;
            if ($this->reader->readToken("{{")) {
                if ($this->reader->readToken("for")) {
                    $varName = $this->reader->readIdentifier();
                    $this->reader->expectToken("of");
                    $itemsExpr = $this->exprParser->parse();
                    $this->reader->expectToken("}}");
                    $body = $this->parseBlock();
                    $this->reader->expectToken("{{/for}}");
                    $items[] = new ForNode($varName, $itemsExpr, $body);
                }
                else {
                    $expr = $this->exprParser->parse();
                    $items[] = new ExpressionNode($expr);
                    $this->reader->expectToken("}}");
                }
            }
            else {
                $literal = $this->reader->readUntil("{{", true);
                if (substr_compare($literal, "\\", strlen($literal) - strlen("\\"), strlen("\\")) === 0 && !substr_compare($literal, "\\\\", strlen($literal) - strlen("\\\\"), strlen("\\\\")) === 0)
                    $literal = substr($literal, 0, strlen($literal) - 1 - (0)) . "{{";
                if ($literal !== "")
                    $items[] = new LiteralNode($literal);
            }
        }
        return new TemplateBlock($items);
    }
    
    function parse() {
        return $this->parseBlock();
    }
}

class TemplateFile {
    public $main;
    public $template;
    
    function __construct($template) {
        $this->template = $template;
        $this->main = (new TemplateParser($template))->parse();
    }
    
    function format($model) {
        return $this->main->format($model);
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
                $tmplFile = new TemplateFile(OneFile::readText($srcFn));
                $dstFile = $tmplFile->format($model);
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
    public $outputDir;
    public $projectTemplates;
    
    function __construct($name, $dependencies, $sourceDir, $sourceLang, $outputDir, $projectTemplates) {
        $this->name = $name;
        $this->dependencies = $dependencies;
        $this->sourceDir = $sourceDir;
        $this->sourceLang = $sourceLang;
        $this->outputDir = $outputDir;
        $this->projectTemplates = $projectTemplates;
    }
    
    static function fromJson($json) {
        return new OneProjectFile($json->get("name")->asString(), array_map(function ($dep) { return new ProjectDependency($dep->get("name")->asString(), $dep->get("version")->asString()); }, array_map(function ($dep) { return $dep->asObject(); }, $json->get("dependencies")->getArrayItems())), $json->get("sourceDir")->asString(), $json->get("sourceLang")->asString(), $json->get("outputDir")->asString(), array_map(function ($x) { return $x->asString(); }, $json->get("projectTemplates")->getArrayItems()));
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
        $generators = array(new JavaGenerator(), new CsharpGenerator(), new PythonGenerator(), new PhpGenerator());
        foreach ($this->projectFile->projectTemplates as $tmplName) {
            $compiler = CompilerHelper::initProject($this->projectFile->name, $this->srcDir, $this->projectFile->sourceLang, null);
            $compiler->processWorkspace();
            
            $projTemplate = new ProjectTemplate($this->baseDir . "/project-templates/" . $tmplName);
            $langId = $projTemplate->meta->language;
            $generator = \OneLang\Core\ArrayHelper::find($generators, function ($x) use ($langId) { return strtolower($x->getLangName()) === $langId; });
            $langName = $generator->getLangName();
            
            foreach ($generator->getTransforms() as $trans)
                $trans->visitFiles(array_values($compiler->projectPkg->files));
            
            $outDir = $this->outDir . "/" . $langName;
            \OneLang\Core\console::log("Generating " . $langName . " code...");
            $files = $generator->generate($compiler->projectPkg);
            foreach ($files as $file)
                OneFile::writeText($outDir . "/" . $projTemplate->meta->destinationDir ?? "" . "/" . $file->path, $file->content);
            
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
                    $dstDir = $outDir . "/" . $projTemplate->meta->packageDir . "/" . $impl->content->id->name;
                    $depFiles = array_map(function ($x) use ($srcDir) { return substr($x, strlen($srcDir)); }, array_values(array_filter(array_keys($impl->content->files), function ($x) use ($srcDir) { return substr_compare($x, $srcDir, 0, strlen($srcDir)) === 0; })));
                    foreach ($depFiles as $fn)
                        OneFile::writeText($dstDir . "/" . $fn, @$impl->content->files[$srcDir . $fn] ?? null);
                }
            }
            
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
