<?php

namespace OneLang\StdLib\PackagesFolderSource;

use OneFile\OneFile;
use OneLang\StdLib\PackageManager\PackageSource;
use OneLang\StdLib\PackageManager\PackageId;
use OneLang\StdLib\PackageManager\PackageBundle;
use OneLang\StdLib\PackageManager\PackageContent;
use OneLang\StdLib\PackageManager\PackageType;

class PackagesFolderSource implements PackageSource {
    public $packagesDir;
    
    function __construct($packagesDir = "packages") {
        $this->packagesDir = $packagesDir;
    }
    
    function getPackageBundle($ids, $cachedOnly) {
        throw new \OneCore\Error("Method not implemented.");
    }
    
    function getAllCached() {
        $packages = Array();
        $allFiles = OneFile::listFiles($this->packagesDir, true);
        foreach ($allFiles as $fn) {
            if ($fn === "bundle.json")
                continue;
            // TODO: hack
            $pathParts = preg_split("/\\//", $fn);
            // [0]=implementations/interfaces, [1]=package-name, [2:]=path
            $type = array_shift($pathParts);
            $pkgDir = array_shift($pathParts);
            if ($type !== "implementations" && $type !== "interfaces")
                continue;
            // skip e.g. bundle.json
            $pkgIdStr = $type . "/" . $pkgDir;
            $pkg = @$packages[$pkgIdStr] ?? null;
            if ($pkg === null) {
                $pkgDirParts = preg_split("/-/", $pkgDir);
                $version = preg_replace("/^v/", "", array_pop($pkgDirParts));
                $pkgType = $type === "implementations" ? PackageType::IMPLEMENTATION : PackageType::INTERFACE_;
                $pkgId = new PackageId($pkgType, implode("-", $pkgDirParts), $version);
                $pkg = new PackageContent($pkgId, Array(), true);
                $packages[$pkgIdStr] = $pkg;
            }
            $pkg->files[implode("/", $pathParts)] = OneFile::readText($this->packagesDir . "/" . $fn);
        }
        return new PackageBundle(array_values($packages));
    }
}
