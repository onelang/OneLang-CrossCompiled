<?php

namespace OneLang\Generator\IGenerator;

use OneLang\Generator\GeneratedFile\GeneratedFile;
use OneLang\One\Ast\Types\Package;
use OneLang\One\ITransformer\ITransformer;

interface IGenerator {
    function getLangName();
    
    function getExtension();
    
    function getTransforms();
    
    function generate($pkg);
}
