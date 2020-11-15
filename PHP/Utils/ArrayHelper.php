<?php

namespace Utils\ArrayHelper;

class ArrayHelper {
    static function sortBy<T>($items, $keySelector) {
        // @java-import java.util.Arrays
        // @java Arrays.sort(items, (a, b) -> keySelector.apply(a) - keySelector.apply(b));
        // @java return items;
        return sort($items);
    }
    
    static function removeLastN<T>($items, $count) {
        // @java items.subList(items.size() - count, items.size()).clear();
        $items->splice(count($items) - $count, $count);
    }
}
