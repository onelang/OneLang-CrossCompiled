<?php

namespace OneLang\StdLib\PackageBundleSource;

use OneLang\StdLib\PackageManager\PackageSource;
use OneLang\StdLib\PackageManager\PackageId;
use OneLang\StdLib\PackageManager\PackageBundle;

class PackageBundleSource implements PackageSource {
    public $bundle;
    
    function __construct($bundle) {
        $this->bundle = $bundle;
    }
    
    function getPackageBundle($ids, $cachedOnly) {
        throw new \OneLang\Core\Error("Method not implemented.");
    }
    
    function getAllCached() {
        return $this->bundle;
    }
}
