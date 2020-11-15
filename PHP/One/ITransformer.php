<?php

namespace One\ITransformer;

use One\Ast\Types\SourceFile;

interface ITransformer {
    function visitFiles($files);
}
