package OneLang.Parsers.Common.Utils;



import OneStd.Objects;
import java.util.Arrays;
import java.util.ArrayList;
import OneStd.RegExp;
import java.util.stream.Collectors;

public class Utils {
    public static Integer getPadLen(String line) {
        for (Integer i = 0; i < line.length(); i++) {
            if (!Objects.equals(line.substring(i, i + 1), " "))
                return i;
        }
        return -1;
    }
    
    public static String deindent(String str) {
        var lines = new ArrayList<>(Arrays.asList(str.split("\\n", -1)));
        if (lines.size() == 1)
            return str;
        
        if (Utils.getPadLen(lines.get(0)) == -1)
            lines.remove(0);
        
        var minPadLen = 9999;
        for (var padLen : Arrays.stream(lines.stream().map(x -> Utils.getPadLen(x)).toArray(Integer[]::new)).filter(x -> x != -1).toArray(Integer[]::new)) {
            if (padLen < minPadLen)
                minPadLen = padLen;
        }
        
        if (minPadLen == 9999)
            return Arrays.stream(lines.stream().map(x -> "").toArray(String[]::new)).collect(Collectors.joining("\n"));
        
        // @java final var minPadLen2 = minPadLen;
        final var minPadLen2 = minPadLen;
        var newStr = Arrays.stream(lines.stream().map(x -> x.length() != 0 ? x.substring(minPadLen2) : x).toArray(String[]::new)).collect(Collectors.joining("\n"));
        return newStr;
    }
}