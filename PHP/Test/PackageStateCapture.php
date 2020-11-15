<?php

namespace Test\PackageStateCapture;

use One\Ast\Types\Package;
use Utils\TSOverviewGenerator\TSOverviewGenerator;

class PackageStateCapture {
    public $overviews;
    public $pkg;
    
    function __construct($pkg) {
        $this->pkg = $pkg;
        $this->overviews = Array();
        foreach (array_values($pkg->files) as $file)
            $this->overviews[$file->sourcePath->path] = (new TSOverviewGenerator(false, false))->generate($file);
    }
    
    function getSummary() {
        return implode("\n\n", array_map(function ($file) { return "=== " . $file . " ===\n\n" . @$this->overviews[$file] ?? null; }, array_keys($this->overviews)));
    }
}
