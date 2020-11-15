<?php

namespace Generator\IGenerator;

use Generator\GeneratedFile\GeneratedFile;
use One\Ast\Types\Package;
use One\ITransformer\ITransformer;

interface IGenerator {
    function getLangName();
    
    function getExtension();
    
    function getTransforms();
    
    function generate($pkg);
}
