<?php

namespace Generator\IGeneratorPlugin;

use One\Ast\Interfaces\IExpression;
use One\Ast\Statements\Statement;

interface IGeneratorPlugin {
    function expr($expr);
    
    function stmt($stmt);
}
