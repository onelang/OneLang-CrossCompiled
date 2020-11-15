<?php

namespace One\Ast\Interfaces;

interface IType {
    function repr();
}

interface IExpression {
    function setActualType($actualType, $allowVoid, $allowGeneric);
    
    function setExpectedType($type, $allowVoid);
    
    function getType();
}
