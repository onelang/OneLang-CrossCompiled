<?php

namespace OneLang\Generator\IGeneratorPlugin;

use OneLang\One\Ast\Interfaces\IExpression;
use OneLang\One\Ast\Statements\Statement;

interface IGeneratorPlugin {
    function expr($expr);
    
    function stmt($stmt);
}
