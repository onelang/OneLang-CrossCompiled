<?php

namespace One\Ast\Types;

use One\Ast\AstTypes\ClassType;
use One\Ast\AstTypes\GenericsType;
use One\Ast\AstTypes\EnumType;
use One\Ast\AstTypes\InterfaceType;
use One\Ast\Expressions\Expression;
use One\Ast\References\ClassReference;
use One\Ast\References\EnumReference;
use One\Ast\References\ThisReference;
use One\Ast\References\MethodParameterReference;
use One\Ast\References\SuperReference;
use One\Ast\References\StaticFieldReference;
use One\Ast\References\EnumMemberReference;
use One\Ast\References\InstanceFieldReference;
use One\Ast\References\StaticPropertyReference;
use One\Ast\References\InstancePropertyReference;
use One\Ast\References\IReferencable;
use One\Ast\References\Reference;
use One\Ast\References\GlobalFunctionReference;
use One\Ast\References\StaticThisReference;
use One\Ast\References\VariableReference;
use One\Ast\AstHelper\AstHelper;
use One\Ast\Statements\Block;
use One\Ast\Interfaces\IType;

class Visibility {
    const PUBLIC = 1;
    const PROTECTED = 2;
    const PRIVATE = 3;
}

interface IAstNode {
    
}

interface IVariable {
    
}

interface IClassMember {
    
}

interface IVariableWithInitializer extends IVariable {
    
}

interface IHasAttributesAndTrivia {
    
}

interface ISourceFileMember {
    
}

interface IInterface {
    function getAllBaseInterfaces();
}

interface IImportable {
    
}

interface IResolvedImportable extends IImportable {
    
}

interface IMethodBase extends IAstNode {
    
}

interface IMethodBaseWithTrivia extends IMethodBase, IHasAttributesAndTrivia {
    
}

class MutabilityInfo {
    public $unused;
    public $reassigned;
    public $mutated;
    
    function __construct($unused, $reassigned, $mutated) {
        $this->unused = $unused;
        $this->reassigned = $reassigned;
        $this->mutated = $mutated;
    }
}

class ExportedScope {
    public $exports;
    
    function __construct()
    {
        $this->exports = new \OneLang\Map();
    }
    
    function getExport($name) {
        $exp = $this->exports->get($name);
        if ($exp === null)
            throw new \OneLang\Error("Export " . $name . " was not found in exported symbols.");
        return $exp;
    }
    
    function addExport($name, $value) {
        $this->exports->set($name, $value);
    }
    
    function getAllExports() {
        return \OneLang\Array_::from($this->exports->values());
    }
}

class Package {
    public $name;
    public $definitionOnly;
    public static $INDEX = "index";
    public $files;
    public $exportedScopes;
    
    function __construct($name, $definitionOnly) {
        $this->name = $name;
        $this->definitionOnly = $definitionOnly;
        $this->files = Array();
        $this->exportedScopes = Array();
    }
    
    static function collectExportsFromFile($file, $exportAll, $scope = null) {
        if ($scope === null)
            $scope = new ExportedScope();
        
        foreach (array_values(array_filter($file->classes, function ($x) use ($exportAll) { return $x->isExported || $exportAll; })) as $cls)
            $scope->addExport($cls->name, $cls);
        
        foreach (array_values(array_filter($file->interfaces, function ($x) use ($exportAll) { return $x->isExported || $exportAll; })) as $intf)
            $scope->addExport($intf->name, $intf);
        
        foreach (array_values(array_filter($file->enums, function ($x) use ($exportAll) { return $x->isExported || $exportAll; })) as $enum_)
            $scope->addExport($enum_->name, $enum_);
        
        foreach (array_values(array_filter($file->funcs, function ($x) use ($exportAll) { return $x->isExported || $exportAll; })) as $func)
            $scope->addExport($func->name, $func);
        
        return $scope;
    }
    
    function addFile($file, $exportAll = false) {
        if ($file->sourcePath->pkg !== $this || $file->exportScope->packageName !== $this->name)
            throw new \OneLang\Error("This file belongs to another package!");
        
        $this->files[$file->sourcePath->path] = $file;
        $scopeName = $file->exportScope->scopeName;
        $this->exportedScopes[$scopeName] = Package::collectExportsFromFile($file, $exportAll, @$this->exportedScopes[$scopeName] ?? null);
    }
    
    function getExportedScope($name) {
        $scope = @$this->exportedScopes[$name] ?? null;
        if ($scope === null)
            throw new \OneLang\Error("Scope \"" . $name . "\" was not found in package \"" . $this->name . "\"");
        return $scope;
    }
}

class Workspace {
    public $packages;
    
    function __construct()
    {
        $this->packages = Array();
    }
    
    function addPackage($pkg) {
        $this->packages[$pkg->name] = $pkg;
    }
    
    function getPackage($name) {
        $pkg = @$this->packages[$name] ?? null;
        if ($pkg === null)
            throw new \OneLang\Error("Package was not found: \"" . $name . "\"");
        return $pkg;
    }
}

class SourcePath {
    public $pkg;
    public $path;
    
    function __construct($pkg, $path) {
        $this->pkg = $pkg;
        $this->path = $path;
    }
    
    function toString() {
        return $this->pkg->name . "/" . $this->path;
    }
}

class LiteralTypes {
    public $boolean;
    public $numeric;
    public $string;
    public $regex;
    public $array;
    public $map;
    public $error;
    public $promise;
    
    function __construct($boolean, $numeric, $string, $regex, $array, $map, $error, $promise) {
        $this->boolean = $boolean;
        $this->numeric = $numeric;
        $this->string = $string;
        $this->regex = $regex;
        $this->array = $array;
        $this->map = $map;
        $this->error = $error;
        $this->promise = $promise;
    }
}

class SourceFile {
    public $imports;
    public $interfaces;
    public $classes;
    public $enums;
    public $funcs;
    public $mainBlock;
    public $sourcePath;
    public $exportScope;
    public $availableSymbols;
    public $literalTypes;
    public $arrayTypes;
    
    function __construct($imports, $interfaces, $classes, $enums, $funcs, $mainBlock, $sourcePath, $exportScope) {
        $this->imports = $imports;
        $this->interfaces = $interfaces;
        $this->classes = $classes;
        $this->enums = $enums;
        $this->funcs = $funcs;
        $this->mainBlock = $mainBlock;
        $this->sourcePath = $sourcePath;
        $this->exportScope = $exportScope;
        $this->availableSymbols = new \OneLang\Map();
        $this->arrayTypes = array();
        $fileScope = Package::collectExportsFromFile($this, true);
        $this->addAvailableSymbols($fileScope->getAllExports());
    }
    
    function addAvailableSymbols($items) {
        foreach ($items as $item)
            $this->availableSymbols->set($item->name, $item);
    }
}

class ExportScopeRef {
    public $packageName;
    public $scopeName;
    
    function __construct($packageName, $scopeName) {
        $this->packageName = $packageName;
        $this->scopeName = $scopeName;
    }
    
    function getId() {
        return $this->packageName . "." . $this->scopeName;
    }
}

class Import implements IHasAttributesAndTrivia, ISourceFileMember {
    public $exportScope;
    public $importAll;
    public $imports;
    public $importAs;
    public $leadingTrivia;
    public $parentFile;
    public $attributes;
    
    function __construct($exportScope, $importAll, $imports, $importAs, $leadingTrivia) {
        $this->exportScope = $exportScope;
        $this->importAll = $importAll;
        $this->imports = $imports;
        $this->importAs = $importAs;
        $this->leadingTrivia = $leadingTrivia;
        if ($importAs !== null && !$importAll)
            throw new \OneLang\Error("importAs only supported with importAll!");
    }
}

class Enum implements IAstNode, IHasAttributesAndTrivia, IResolvedImportable, ISourceFileMember, IReferencable {
    public $name;
    public $values;
    public $isExported;
    public $leadingTrivia;
    public $parentFile;
    public $attributes;
    public $references;
    public $type;
    
    function __construct($name, $values, $isExported, $leadingTrivia) {
        $this->name = $name;
        $this->values = $values;
        $this->isExported = $isExported;
        $this->leadingTrivia = $leadingTrivia;
        $this->references = array();
        $this->type = new EnumType($this);
    }
    
    function createReference() {
        return new EnumReference($this);
    }
}

class EnumMember implements IAstNode {
    public $name;
    public $parentEnum;
    public $references;
    
    function __construct($name) {
        $this->name = $name;
        $this->references = array();
    }
}

class UnresolvedImport implements IImportable {
    public $name;
    public $isExported;
    
    function __construct($name) {
        $this->name = $name;
    }
}

class Interface_ implements IHasAttributesAndTrivia, IInterface, IResolvedImportable, ISourceFileMember {
    public $name;
    public $typeArguments;
    public $baseInterfaces;
    public $fields;
    public $methods;
    public $isExported;
    public $leadingTrivia;
    public $parentFile;
    public $attributes;
    public $type;
    public $_baseInterfaceCache;
    
    function __construct($name, $typeArguments, $baseInterfaces, $fields, $methods, $isExported, $leadingTrivia) {
        $this->name = $name;
        $this->typeArguments = $typeArguments;
        $this->baseInterfaces = $baseInterfaces;
        $this->fields = $fields;
        $this->methods = $methods;
        $this->isExported = $isExported;
        $this->leadingTrivia = $leadingTrivia;
        $this->type = new InterfaceType($this, array_map(function ($x) { return new GenericsType($x); }, $this->typeArguments));
        $this->_baseInterfaceCache = null;
    }
    
    function getAllBaseInterfaces() {
        if ($this->_baseInterfaceCache === null)
            $this->_baseInterfaceCache = AstHelper::collectAllBaseInterfaces($this);
        return $this->_baseInterfaceCache;
    }
}

class Class_ implements IHasAttributesAndTrivia, IInterface, IResolvedImportable, ISourceFileMember, IReferencable {
    public $name;
    public $typeArguments;
    public $baseClass;
    public $baseInterfaces;
    public $fields;
    public $properties;
    public $constructor_;
    public $methods;
    public $isExported;
    public $leadingTrivia;
    public $parentFile;
    public $attributes;
    public $classReferences;
    public $thisReferences;
    public $staticThisReferences;
    public $superReferences;
    public $type;
    public $_baseInterfaceCache;
    
    function __construct($name, $typeArguments, $baseClass, $baseInterfaces, $fields, $properties, $constructor_, $methods, $isExported, $leadingTrivia) {
        $this->name = $name;
        $this->typeArguments = $typeArguments;
        $this->baseClass = $baseClass;
        $this->baseInterfaces = $baseInterfaces;
        $this->fields = $fields;
        $this->properties = $properties;
        $this->constructor_ = $constructor_;
        $this->methods = $methods;
        $this->isExported = $isExported;
        $this->leadingTrivia = $leadingTrivia;
        $this->classReferences = array();
        $this->thisReferences = array();
        $this->staticThisReferences = array();
        $this->superReferences = array();
        $this->type = new ClassType($this, array_map(function ($x) { return new GenericsType($x); }, $this->typeArguments));
        $this->_baseInterfaceCache = null;
    }
    
    function createReference() {
        return new ClassReference($this);
    }
    
    function getAllBaseInterfaces() {
        if ($this->_baseInterfaceCache === null)
            $this->_baseInterfaceCache = AstHelper::collectAllBaseInterfaces($this);
        return $this->_baseInterfaceCache;
    }
}

class Field implements IVariableWithInitializer, IHasAttributesAndTrivia, IClassMember, IAstNode {
    public $name;
    public $type;
    public $initializer;
    public $visibility;
    public $isStatic;
    public $constructorParam;
    public $leadingTrivia;
    public $parentInterface;
    public $attributes;
    public $staticReferences;
    public $instanceReferences;
    public $interfaceDeclarations;
    public $mutability;
    
    function __construct($name, $type, $initializer, $visibility, $isStatic, $constructorParam, $leadingTrivia) {
        $this->name = $name;
        $this->type = $type;
        $this->initializer = $initializer;
        $this->visibility = $visibility;
        $this->isStatic = $isStatic;
        $this->constructorParam = $constructorParam;
        $this->leadingTrivia = $leadingTrivia;
        $this->parentInterface = null;
        $this->staticReferences = array();
        $this->instanceReferences = array();
        $this->interfaceDeclarations = null;
    }
}

class Property implements IVariable, IHasAttributesAndTrivia, IClassMember, IAstNode {
    public $name;
    public $type;
    public $getter;
    public $setter;
    public $visibility;
    public $isStatic;
    public $leadingTrivia;
    public $parentClass;
    public $attributes;
    public $staticReferences;
    public $instanceReferences;
    public $mutability;
    
    function __construct($name, $type, $getter, $setter, $visibility, $isStatic, $leadingTrivia) {
        $this->name = $name;
        $this->type = $type;
        $this->getter = $getter;
        $this->setter = $setter;
        $this->visibility = $visibility;
        $this->isStatic = $isStatic;
        $this->leadingTrivia = $leadingTrivia;
        $this->parentClass = null;
        $this->staticReferences = array();
        $this->instanceReferences = array();
    }
}

class MethodParameter implements IVariableWithInitializer, IReferencable, IHasAttributesAndTrivia {
    public $name;
    public $type;
    public $initializer;
    public $leadingTrivia;
    public $fieldDecl;
    public $parentMethod;
    public $attributes;
    public $references;
    public $mutability;
    
    function __construct($name, $type, $initializer, $leadingTrivia) {
        $this->name = $name;
        $this->type = $type;
        $this->initializer = $initializer;
        $this->leadingTrivia = $leadingTrivia;
        $this->fieldDecl = null;
        $this->parentMethod = null;
        $this->references = array();
    }
    
    function createReference() {
        return new MethodParameterReference($this);
    }
}

class Constructor implements IMethodBaseWithTrivia {
    public $parameters;
    public $body;
    public $superCallArgs;
    public $leadingTrivia;
    public $parentClass;
    public $attributes;
    public $throws;
    
    function __construct($parameters, $body, $superCallArgs, $leadingTrivia) {
        $this->parameters = $parameters;
        $this->body = $body;
        $this->superCallArgs = $superCallArgs;
        $this->leadingTrivia = $leadingTrivia;
        $this->parentClass = null;
    }
}

class Method implements IMethodBaseWithTrivia, IClassMember {
    public $name;
    public $typeArguments;
    public $parameters;
    public $body;
    public $visibility;
    public $isStatic;
    public $returns;
    public $async;
    public $leadingTrivia;
    public $parentInterface;
    public $attributes;
    public $interfaceDeclarations;
    public $overrides;
    public $overriddenBy;
    public $throws;
    
    function __construct($name, $typeArguments, $parameters, $body, $visibility, $isStatic, $returns, $async, $leadingTrivia) {
        $this->name = $name;
        $this->typeArguments = $typeArguments;
        $this->parameters = $parameters;
        $this->body = $body;
        $this->visibility = $visibility;
        $this->isStatic = $isStatic;
        $this->returns = $returns;
        $this->async = $async;
        $this->leadingTrivia = $leadingTrivia;
        $this->parentInterface = null;
        $this->interfaceDeclarations = null;
        $this->overrides = null;
        $this->overriddenBy = array();
    }
}

class GlobalFunction implements IMethodBaseWithTrivia, IResolvedImportable, IReferencable {
    public $name;
    public $parameters;
    public $body;
    public $returns;
    public $isExported;
    public $leadingTrivia;
    public $parentFile;
    public $attributes;
    public $throws;
    public $references;
    
    function __construct($name, $parameters, $body, $returns, $isExported, $leadingTrivia) {
        $this->name = $name;
        $this->parameters = $parameters;
        $this->body = $body;
        $this->returns = $returns;
        $this->isExported = $isExported;
        $this->leadingTrivia = $leadingTrivia;
        $this->references = array();
    }
    
    function createReference() {
        return new GlobalFunctionReference($this);
    }
}

class Lambda extends Expression implements IMethodBase {
    public $parameters;
    public $body;
    public $returns;
    public $throws;
    public $captures;
    
    function __construct($parameters, $body) {
        parent::__construct();
        $this->parameters = $parameters;
        $this->body = $body;
        $this->returns = null;
        $this->captures = null;
    }
}
