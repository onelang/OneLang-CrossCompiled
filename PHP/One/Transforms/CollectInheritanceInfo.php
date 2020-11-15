<?php

namespace One\Transforms\CollectInheritanceInfo;

use One\Ast\Types\Package;
use One\Ast\Types\Class_;
use One\Ast\Types\Interface_;
use One\Ast\Types\SourceFile;
use One\ITransformer\ITransformer;

class CollectInheritanceInfo implements ITransformer {
    public $name;
    
    function __construct() {
        // C# fix
        $this->name = "CollectInheritanceInfo";
    }
    
    function visitClass($cls) {
        $allBaseIIntfs = $cls->getAllBaseInterfaces();
        $intfs = array_values(array_filter(array_map(function ($x) { return $x instanceof Interface_ ? $x : null; }, $allBaseIIntfs), function ($x) { return $x !== null; }));
        $clses = array_values(array_filter(array_map(function ($x) { return $x instanceof Class_ ? $x : null; }, $allBaseIIntfs), function ($x) use ($cls) { return $x !== null && $x !== $cls; }));
        
        foreach ($cls->fields as $field)
            $field->interfaceDeclarations = array_values(array_filter(array_map(function ($x) use ($field) { return \OneLang\ArrayHelper::find($x->fields, function ($f) use ($field) { return $f->name === $field->name; }); }, $intfs), function ($x) { return $x !== null; }));
        
        foreach ($cls->methods as $method) {
            $method->interfaceDeclarations = array_values(array_filter(array_map(function ($x) use ($method) { return \OneLang\ArrayHelper::find($x->methods, function ($m) use ($method) { return $m->name === $method->name; }); }, $intfs), function ($x) { return $x !== null; }));
            $method->overrides = \OneLang\ArrayHelper::find(array_map(function ($x) use ($method) { return \OneLang\ArrayHelper::find($x->methods, function ($m) use ($method) { return $m->name === $method->name; }); }, $clses), function ($x) { return $x !== null; });
            if ($method->overrides !== null)
                $method->overrides->overriddenBy[] = $method;
        }
    }
    
    function visitFiles($files) {
        foreach ($files as $file)
            foreach ($file->classes as $cls)
                $this->visitClass($cls);
    }
}
