<?php

namespace OneLang\One\ITransformer;

use OneLang\One\Ast\Types\SourceFile;

interface ITransformer {
    function visitFiles($files);
}
