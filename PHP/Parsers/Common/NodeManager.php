<?php

namespace Parsers\Common\NodeManager;

use Parsers\Common\Reader\Reader;

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
