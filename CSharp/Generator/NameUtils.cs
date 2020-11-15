using System.Collections.Generic;

namespace Generator
{
    public class NameUtils {
        public static string shortName(string fullName)
        {
            var nameParts = new List<string>();
            var partStartIdx = 0;
            for (int i = 1; i < fullName.length(); i++) {
                var chrCode = fullName.charCodeAt(i);
                var chrIsUpper = 65 <= chrCode && chrCode <= 90;
                if (chrIsUpper) {
                    nameParts.push(fullName.substring(partStartIdx, i));
                    partStartIdx = i;
                }
            }
            nameParts.push(fullName.substr(partStartIdx));
            
            var shortNameParts = new List<string>();
            for (int i = 0; i < nameParts.length(); i++) {
                var p = nameParts.get(i);
                if (p.length() > 5) {
                    var cutPoint = 3;
                    for (; cutPoint <= 4; cutPoint++) {
                        if ("aeoiu".includes(p.get(cutPoint)))
                            break;
                    }
                    p = p.substr(0, cutPoint);
                }
                shortNameParts.push(i == 0 ? p.toLowerCase() : p);
            }
            
            var shortName = shortNameParts.join("");
            if (fullName.endsWith("s") && !shortName.endsWith("s"))
                shortName += "s";
            return shortName;
        }
    }
}