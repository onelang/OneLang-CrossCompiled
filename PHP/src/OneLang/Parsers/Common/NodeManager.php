<?php

namespace OneLang\Parsers\Common\NodeManager;

use OneLang\Parsers\Common\Reader\Reader;

class NodeManager {
    public $nodes;
    public $reader;
    
    function __construct($reader) {
        $this->reader = $reader;
        $this->nodes = array();
    }
    
    function addNode($node, $start) {
        $this->nodes[] = $node;
    }
}
