<?php

namespace OneLang\Test\TestCase;

interface ITestCollection {
    function getTestCases();
}

class TestCase {
    public $name;
    public $action;
    
    function __construct($name, $action) {
        $this->name = $name;
        $this->action = $action;
    }
}

class SyncTestCase extends TestCase {
    public $syncAction;
    
    function __construct($name, $syncAction) {
        parent::__construct($name, null);
        $this->syncAction = $syncAction;
        $this->action = function ($artifactDir) { return $this->execute($artifactDir); };
    }
    
    function execute($artifactDir) {
        call_user_func($this->syncAction, $artifactDir);
    }
}
