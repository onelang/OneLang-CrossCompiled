<?php

namespace Parsers\Common\IParser;

use Parsers\Common\NodeManager\NodeManager;
use One\Ast\Types\SourceFile;

interface IParser {
    function parse();
}
