package OneLang.Generator.NameUtils;



import java.util.ArrayList;
import java.util.stream.Collectors;

public class NameUtils {
    public static String shortName(String fullName) {
        var nameParts = new ArrayList<String>();
        var partStartIdx = 0;
        for (Integer i = 1; i < fullName.length(); i++) {
            var chrCode = (int)fullName.charAt(i);
            var chrIsUpper = 65 <= chrCode && chrCode <= 90;
            if (chrIsUpper) {
                nameParts.add(fullName.substring(partStartIdx, i));
                partStartIdx = i;
            }
        }
        nameParts.add(fullName.substring(partStartIdx));
        
        var shortNameParts = new ArrayList<String>();
        for (Integer i = 0; i < nameParts.size(); i++) {
            var p = nameParts.get(i);
            if (p.length() > 5) {
                var cutPoint = 3;
                for (; cutPoint <= 4; cutPoint++) {
                    if ("aeoiu".contains(p.substring(cutPoint, cutPoint + 1)))
                        break;
                }
                p = p.substring(0, 0 + cutPoint);
            }
            shortNameParts.add(i == 0 ? p.toLowerCase() : p);
        }
        
        var shortName = shortNameParts.stream().collect(Collectors.joining(""));
        if (fullName.endsWith("s") && !shortName.endsWith("s"))
            shortName += "s";
        return shortName;
    }
}