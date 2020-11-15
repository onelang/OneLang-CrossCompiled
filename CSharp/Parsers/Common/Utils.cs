using System.Linq;

namespace Parsers.Common
{
    public class Utils {
        public static int getPadLen(string line)
        {
            for (int i = 0; i < line.length(); i++) {
                if (line.get(i) != " ")
                    return i;
            }
            return -1;
        }
        
        public static string deindent(string str)
        {
            var lines = str.split(new RegExp("\\n")).ToList();
            if (lines.length() == 1)
                return str;
            
            if (Utils.getPadLen(lines.get(0)) == -1)
                lines.shift();
            
            var minPadLen = 9999;
            foreach (var padLen in lines.map(x => Utils.getPadLen(x)).filter(x => x != -1)) {
                if (padLen < minPadLen)
                    minPadLen = padLen;
            }
            
            if (minPadLen == 9999)
                return lines.map(x => "").join("\n");
            
            // @java final var minPadLen2 = minPadLen;
            var minPadLen2 = minPadLen;
            var newStr = lines.map(x => x.length() != 0 ? x.substr(minPadLen2) : x).join("\n");
            return newStr;
        }
    }
}