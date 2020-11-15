<?php

namespace StdLib\PackageBundleSource;

use StdLib\PackageManager\PackageSource;
use StdLib\PackageManager\PackageId;
use StdLib\PackageManager\PackageBundle;

class PackageBundleSource implements PackageSource {
    public $bundle;
    
    function __construct($bundle) {
        $this->bundle = $bundle;
    }
    
    function getPackageBundle($ids, $cachedOnly) {
        throw new \OneLang\Error("Method not implemented.");
    }
    
    function getAllCached() {
        return $this->bundle;
    }
}
