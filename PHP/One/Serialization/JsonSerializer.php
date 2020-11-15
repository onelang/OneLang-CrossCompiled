<?php

namespace One\Serialization\JsonSerializer;

use One\Ast\AstTypes\ClassType;
use One\Ast\AstTypes\EnumType;
use One\Ast\AstTypes\IInterfaceType;
use One\Ast\AstTypes\InterfaceType;
use One\Ast\AstTypes\TypeHelper;
use One\Ast\Interfaces\IType;
use One\Ast\Types\Class_;
use One\Ast\Types\LiteralTypes;
use One\Ast\Types\Package;

class JsonSerializer {
    public $circleDetector;
    public $litTypes;
    
    function __construct($litTypes) {
        $this->litTypes = $litTypes;
        $this->circleDetector = new \OneLang\Map();
    }
    
    function pad($str) {
        return implode("\n", array_map(function ($x) { return "    " . $x; }, preg_split("/\\n/", $str)));
    }
    
    function serialize($obj) {
        $declType = $obj->getDeclaredType();
        if ($obj->isNull())
            return "null";
        else if (TypeHelper::equals($declType, $this->litTypes->string))
            return json_encode($obj->getStringValue(), JSON_UNESCAPED_SLASHES);
        else if (TypeHelper::equals($declType, $this->litTypes->boolean))
            return $obj->getBooleanValue() ? "true" : "false";
        else if (TypeHelper::isAssignableTo($declType, $this->litTypes->array)) {
            $items = array();
            foreach ($obj->getArrayItems() as $item)
                $items[] = $this->serialize($item);
            return count($items) === 0 ? "[]" : "[\n" . $this->pad(implode(",\n", $items)) . "\n]";
        }
        else if (TypeHelper::isAssignableTo($declType, $this->litTypes->map)) {
            $items = array();
            foreach ($obj->getMapKeys() as $key) {
                $value = $obj->getMapValue($key);
                $items[] = "\"" . $key . "\": " . $this->serialize($value);
            }
            return count($items) === 0 ? "{}" : "{\n" . $this->pad(implode(",\n", $items)) . "\n}";
        }
        else if ($declType instanceof ClassType || $declType instanceof InterfaceType) {
            $rawValue = $obj->getUniqueIdentifier();
            if ($this->circleDetector->has($rawValue))
                return "{\"$ref\":\"" . $this->circleDetector->get($rawValue) . "\"}";
            $id = "id_" . $this->circleDetector->size;
            $this->circleDetector->set($rawValue, $id);
            
            $valueType = $obj->getValueType();
            $decl = ($declType)->getDecl();
            
            $members = array();
            
            $members[] = "\"$id\": \"" . $id . "\"";
            
            if ($valueType !== null && !TypeHelper::equals($valueType, $declType))
                $members[] = "\"$type\": \"" . $valueType->repr() . "\"";
            
            foreach (array_values(array_filter($decl->fields, function ($x) { return !$x->isStatic; })) as $field) {
                if (array_key_exists("json-ignore", $field->attributes))
                    continue;
                //console.log(`processing ${field.parentInterface.name}::${field.name}`);
                $value = $obj->getField($field->name);
                $serializedValue = $this->serialize($value);
                if (!in_array($serializedValue, array("[]", "{}", "null", "false", "\"\"")))
                    $members[] = "\"" . $field->name . "\": " . $serializedValue;
            }
            return count($members) === 0 ? "{}" : "{\n" . $this->pad(implode(",\n", $members)) . "\n}";
        }
        else if ($declType instanceof EnumType)
            return "\"" . $obj->getEnumValueAsString() . "\"";
        return "\"<UNKNOWN-TYPE>\"";
    }
}
