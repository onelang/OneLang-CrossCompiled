<?php

namespace OneLang\Parsers\Common\IParser;

use OneLang\Parsers\Common\NodeManager\NodeManager;
use OneLang\One\Ast\Types\SourceFile;

interface IParser {
    function parse();
}
