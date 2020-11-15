<?php

namespace Generator\GeneratedFile;

class GeneratedFile {
    public $path;
    public $content;
    
    function __construct($path, $content) {
        $this->path = $path;
        $this->content = $content;
    }
}
