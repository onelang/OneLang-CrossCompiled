<?php

namespace OneLang\VM\Values;

interface IVMValue {
    
}

interface ICallableValue extends IVMValue {
    function call($args);
}

class ObjectValue implements IVMValue {
    public $props;
    
    function __construct($props) {
        $this->props = $props;
    }
}

class StringValue implements IVMValue {
    public $value;
    
    function __construct($value) {
        $this->value = $value;
    }
}

class BooleanValue implements IVMValue {
    public $value;
    
    function __construct($value) {
        $this->value = $value;
    }
}

class ArrayValue implements IVMValue {
    public $items;
    
    function __construct($items) {
        $this->items = $items;
    }
}
