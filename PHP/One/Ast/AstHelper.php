<?php

namespace One\Ast\AstHelper;

use One\Ast\Types\IInterface;
use One\Ast\Types\Class_;
use One\Ast\AstTypes\InterfaceType;
use One\Ast\AstTypes\ClassType;

class AstHelper {
    static function collectAllBaseInterfaces($intf) {
        $result = new \OneLang\Set();
        $toBeProcessed = array($intf);
        
        while (count($toBeProcessed) > 0) {
            $curr = array_pop($toBeProcessed);
            $result->add($curr);
            
            if ($curr instanceof Class_ && $curr->baseClass !== null)
                $toBeProcessed[] = ($curr->baseClass)->decl;
            
            foreach ($curr->baseInterfaces as $baseIntf)
                $toBeProcessed[] = ($baseIntf)->decl;
        }
        
        return \OneLang\Array_::from($result->values());
    }
}
