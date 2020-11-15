using System.Collections.Generic;
using System;

namespace Utils
{
    public class ArrayHelper {
        public static T[] sortBy<T>(T[] items, Func<T, int> keySelector)
        {
            // @java-import java.util.Arrays
            // @java Arrays.sort(items, (a, b) -> keySelector.apply(a) - keySelector.apply(b));
            // @java return items;
            return items.sort((a, b) => keySelector(a) - keySelector(b));
        }
        
        public static void removeLastN<T>(List<T> items, int count)
        {
            // @java items.subList(items.size() - count, items.size()).clear();
            items.splice(items.length() - count, count);
        }
    }
}