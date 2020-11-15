<?php

namespace StdLib\PackageManager;

use onepkg\OneYaml\OneYaml;

class PackageType {
    const INTERFACE_ = 1;
    const IMPLEMENTATION = 2;
}

interface PackageSource {
    function getPackageBundle($ids, $cachedOnly);
    
    function getAllCached();
}

class PackageId {
    public $type;
    public $name;
    public $version;
    
    function __construct($type, $name, $version) {
        $this->type = $type;
        $this->name = $name;
        $this->version = $version;
    }
}

class PackageContent {
    public $id;
    public $files;
    
    function __construct($id, $files, $fromCache) {
        $this->id = $id;
        $this->files = $files;
    }
}

class PackageBundle {
    public $packages;
    
    function __construct($packages) {
        $this->packages = $packages;
    }
}

class PackageNativeImpl {
    public $pkgName;
    public $pkgVendor;
    public $pkgVersion;
    public $fileName;
    public $code;
}

class InterfaceDependency {
    public $name;
    public $minver;
    
    function __construct($name, $minver) {
        $this->name = $name;
        $this->minver = $minver;
    }
}

class InterfaceYaml {
    public $fileVersion;
    public $vendor;
    public $name;
    public $version;
    public $definitionFile;
    public $dependencies;
    
    function __construct($fileVersion, $vendor, $name, $version, $definitionFile, $dependencies) {
        $this->fileVersion = $fileVersion;
        $this->vendor = $vendor;
        $this->name = $name;
        $this->version = $version;
        $this->definitionFile = $definitionFile;
        $this->dependencies = $dependencies;
    }
    
    static function fromYaml($obj) {
        return new InterfaceYaml($obj->dbl("file-version"), $obj->str("vendor"), $obj->str("name"), $obj->dbl("version"), $obj->str("definition-file"), array_map(function ($dep) { return new InterfaceDependency($dep->str("name"), $dep->dbl("minver")); }, $obj->arr("dependencies")));
    }
}

class InterfacePackage {
    public $interfaceYaml;
    public $definition;
    public $content;
    
    function __construct($content) {
        $this->content = $content;
        $this->interfaceYaml = InterfaceYaml::fromYaml(OneYaml::load(@$content->files["interface.yaml"] ?? null));
        $this->definition = @$content->files[$this->interfaceYaml->definitionFile] ?? null;
    }
}

class ImplPkgImplIntf {
    public $name;
    public $minver;
    public $maxver;
    
    function __construct($name, $minver, $maxver) {
        $this->name = $name;
        $this->minver = $minver;
        $this->maxver = $maxver;
    }
}

class ImplPkgImplementation {
    public $interface_;
    public $language;
    public $nativeIncludes;
    public $nativeIncludeDir;
    
    function __construct($interface_, $language, $nativeIncludes, $nativeIncludeDir) {
        $this->interface_ = $interface_;
        $this->language = $language;
        $this->nativeIncludes = $nativeIncludes;
        $this->nativeIncludeDir = $nativeIncludeDir;
    }
}

class ImplPackageYaml {
    public $fileVersion;
    public $vendor;
    public $name;
    public $description;
    public $version;
    public $includes;
    public $implements_;
    
    function __construct($fileVersion, $vendor, $name, $description, $version, $includes, $implements_) {
        $this->fileVersion = $fileVersion;
        $this->vendor = $vendor;
        $this->name = $name;
        $this->description = $description;
        $this->version = $version;
        $this->includes = $includes;
        $this->implements_ = $implements_;
    }
    
    static function fromYaml($obj) {
        return new ImplPackageYaml($obj->dbl("file-version"), $obj->str("vendor"), $obj->str("name"), $obj->str("description"), $obj->str("version"), $obj->strArr("includes"), array_map(function ($impl) { return new ImplPkgImplementation(new ImplPkgImplIntf($impl->obj("interface")->str("name"), $impl->obj("interface")->dbl("minver"), $impl->obj("interface")->dbl("maxver")), $impl->str("language"), $impl->strArr("native-includes"), $impl->str("native-include-dir")); }, $obj->arr("implements")));
    }
}

class ImplementationPackage {
    public $implementationYaml;
    public $implementations;
    public $content;
    
    function __construct($content) {
        $this->content = $content;
        $this->implementations = array();
        $this->implementationYaml = ImplPackageYaml::fromYaml(OneYaml::load(@$content->files["package.yaml"] ?? null));
        $this->implementations = array();
        foreach ($this->implementationYaml->implements_ ?? array() as $impl)
            $this->implementations[] = $impl;
        foreach ($this->implementationYaml->includes ?? array() as $include) {
            $included = ImplPackageYaml::fromYaml(OneYaml::load(@$content->files[$include] ?? null));
            foreach ($included->implements_ as $impl)
                $this->implementations[] = $impl;
        }
    }
}

class PackageManager {
    public $interfacesPkgs;
    public $implementationPkgs;
    public $source;
    
    function __construct($source) {
        $this->source = $source;
        $this->interfacesPkgs = array();
        $this->implementationPkgs = array();
    }
    
    function loadAllCached() {
        $allPackages = $this->source->getAllCached();
        
        foreach (array_values(array_filter($allPackages->packages, function ($x) { return $x->id->type === PackageType::INTERFACE_; })) as $content)
            $this->interfacesPkgs[] = new InterfacePackage($content);
        
        foreach (array_values(array_filter($allPackages->packages, function ($x) { return $x->id->type === PackageType::IMPLEMENTATION; })) as $content)
            $this->implementationPkgs[] = new ImplementationPackage($content);
    }
    
    function getLangImpls($langName) {
        $allImpls = array();
        foreach ($this->implementationPkgs as $pkg)
            foreach ($pkg->implementations as $impl)
                $allImpls[] = $impl;
        return array_values(array_filter($allImpls, function ($x) use ($langName) { return $x->language === $langName; }));
    }
    
    function getInterfaceDefinitions() {
        return implode("\n", array_map(function ($x) { return $x->definition; }, $this->interfacesPkgs));
    }
    
    function getLangNativeImpls($langName) {
        $result = array();
        foreach ($this->implementationPkgs as $pkg)
            foreach (array_values(array_filter($pkg->implementations, function ($x) use ($langName) { return $x->language === $langName; })) as $pkgImpl) {
                $fileNamePaths = Array();
                foreach ($pkgImpl->nativeIncludes as $fileName)
                    $fileNamePaths[$fileName] = "native/" . $fileName;
                
                $incDir = $pkgImpl->nativeIncludeDir;
                if ($incDir !== null) {
                    if (!substr_compare($incDir, "/", strlen($incDir) - strlen("/"), strlen("/")) === 0)
                        $incDir .= "/";
                    $prefix = "native/" . $incDir;
                    foreach (array_map(function ($x) use ($prefix) { return substr($x, strlen($prefix)); }, array_values(array_filter(array_keys($pkg->content->files), function ($x) use ($prefix) { return substr_compare($x, $prefix, 0, strlen($prefix)) === 0; }))) as $fn)
                        $fileNamePaths[$fn] = $prefix . $fn;
                }
                
                foreach (array_keys($fileNamePaths) as $fileName) {
                    $path = @$fileNamePaths[$fileName] ?? null;
                    $code = @$pkg->content->files[$path] ?? null;
                    if ($code === null)
                        throw new \OneLang\Error("File '" . $fileName . "' was not found for package '" . $pkg->implementationYaml->name . "'");
                    $impl = new PackageNativeImpl();
                    $impl->pkgName = $pkg->implementationYaml->name;
                    $impl->pkgVendor = $pkg->implementationYaml->vendor;
                    $impl->pkgVersion = $pkg->implementationYaml->version;
                    $impl->fileName = $fileName;
                    $impl->code = $code;
                    $result[] = $impl;
                }
            }
        return $result;
    }
}
