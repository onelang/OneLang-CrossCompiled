<?php

namespace OneLang\VM\Values;

interface IVMValue {
    function equals($other);
}

interface ICallableValue extends IVMValue {
    function call($args);
}

class ObjectValue implements IVMValue {
    public $props;
    
    function __construct($props) {
        $this->props = $props;
    }
    
    function equals($other) {
        return false;
    }
}

class StringValue implements IVMValue {
    public $value;
    
    function __construct($value) {
        $this->value = $value;
    }
    
    function equals($other) {
        return $other instanceof StringValue && $other->value === $this->value;
    }
}

class NumericValue implements IVMValue {
    public $value;
    
    function __construct($value) {
        $this->value = $value;
    }
    
    function equals($other) {
        return $other instanceof NumericValue && $other->value === $this->value;
    }
}

class BooleanValue implements IVMValue {
    public $value;
    
    function __construct($value) {
        $this->value = $value;
    }
    
    function equals($other) {
        return $other instanceof BooleanValue && $other->value === $this->value;
    }
}

class ArrayValue implements IVMValue {
    public $items;
    
    function __construct($items) {
        $this->items = $items;
    }
    
    function equals($other) {
        return false;
    }
}
