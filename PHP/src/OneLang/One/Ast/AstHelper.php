<?php

namespace OneLang\One\Ast\AstHelper;

use OneLang\One\Ast\Types\IInterface;
use OneLang\One\Ast\Types\Class_;
use OneLang\One\Ast\AstTypes\InterfaceType;
use OneLang\One\Ast\AstTypes\ClassType;

class AstHelper {
    static function collectAllBaseInterfaces($intf) {
        $result = new \OneCore\Set();
        $toBeProcessed = array($intf);
        
        while (count($toBeProcessed) > 0) {
            $curr = array_pop($toBeProcessed);
            $result->add($curr);
            
            if ($curr instanceof Class_ && $curr->baseClass !== null)
                $toBeProcessed[] = ($curr->baseClass)->decl;
            
            foreach ($curr->baseInterfaces as $baseIntf)
                $toBeProcessed[] = ($baseIntf)->decl;
        }
        
        return \OneCore\Array_::from($result->values());
    }
}
