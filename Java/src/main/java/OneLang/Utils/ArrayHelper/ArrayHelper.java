package OneLang.Utils.ArrayHelper;



import java.util.Arrays;
import java.util.function.Function;
import java.util.List;

public class ArrayHelper {
    public static <T> T[] sortBy(T[] items, Function<T, Integer> keySelector) {
        // @java-import java.util.Arrays
        // @java Arrays.sort(items, (a, b) -> keySelector.apply(a) - keySelector.apply(b));
        // @java return items;
        Arrays.sort(items, (a, b) -> keySelector.apply(a) - keySelector.apply(b));
        return items;
    }
    
    public static <T> void removeLastN(List<T> items, Integer count) {
        // @java items.subList(items.size() - count, items.size()).clear();
        items.subList(items.size() - count, items.size()).clear();
    }
}