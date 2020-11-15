package OneLang.Parsers.Common.Reader;

import OneLang.Parsers.Common.Utils.Utils;

public class Cursor {
    public Integer offset;
    public Integer line;
    public Integer column;
    public Integer lineStart;
    public Integer lineEnd;
    
    public Cursor(Integer offset, Integer line, Integer column, Integer lineStart, Integer lineEnd)
    {
        this.offset = offset;
        this.line = line;
        this.column = column;
        this.lineStart = lineStart;
        this.lineEnd = lineEnd;
    }
}