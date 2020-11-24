<?php

namespace OneLang\Generator\IGenerator;

use OneLang\Generator\GeneratedFile\GeneratedFile;
use OneLang\One\Ast\Types\Package;
use OneLang\One\ITransformer\ITransformer;
use OneLang\Generator\IGeneratorPlugin\IGeneratorPlugin;
use OneLang\One\Ast\Interfaces\IExpression;

interface IGenerator {
    function getLangName();
    
    function getExtension();
    
    function getTransforms();
    
    function addPlugin($plugin);
    
    function addInclude($include);
    
    function expr($expr);
    
    function generate($pkg);
}
