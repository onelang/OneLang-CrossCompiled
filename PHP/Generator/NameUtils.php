<?php

namespace Generator\NameUtils;

class NameUtils {
    static function shortName($fullName) {
        $nameParts = array();
        $partStartIdx = 0;
        for ($i = 1; $i < strlen($fullName); $i++) {
            $chrCode = ord($fullName[$i]);
            $chrIsUpper = 65 <= $chrCode && $chrCode <= 90;
            if ($chrIsUpper) {
                $nameParts[] = substr($fullName, $partStartIdx, $i - ($partStartIdx));
                $partStartIdx = $i;
            }
        }
        $nameParts[] = substr($fullName, $partStartIdx);
        
        $shortNameParts = array();
        for ($i = 0; $i < count($nameParts); $i++) {
            $p = $nameParts[$i];
            if (strlen($p) > 5) {
                $cutPoint = 3;
                for (; $cutPoint <= 4; $cutPoint++) {
                    if (strpos("aeoiu", $p[$cutPoint]) !== false)
                        break;
                }
                $p = substr($p, 0, $cutPoint);
            }
            $shortNameParts[] = $i === 0 ? strtolower($p) : $p;
        }
        
        $shortName = implode("", $shortNameParts);
        if (substr_compare($fullName, "s", strlen($fullName) - strlen("s"), strlen("s")) === 0 && !substr_compare($shortName, "s", strlen($shortName) - strlen("s"), strlen("s")) === 0)
            $shortName .= "s";
        return $shortName;
    }
}
