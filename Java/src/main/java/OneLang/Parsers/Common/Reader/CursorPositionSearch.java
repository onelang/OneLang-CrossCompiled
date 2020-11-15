package OneLang.Parsers.Common.Reader;

import OneLang.Parsers.Common.Utils.Utils;

import java.util.List;
import java.util.ArrayList;
import OneStd.Objects;
import OneLang.Parsers.Common.Reader.Cursor;

public class CursorPositionSearch {
    public List<Integer> lineOffsets;
    public String input;
    
    public CursorPositionSearch(String input)
    {
        this.input = input;
        this.lineOffsets = new ArrayList<>(List.of(0));
        for (Integer i = 0; i < input.length(); i++) {
            if (Objects.equals(input.substring(i, i + 1), "\n"))
                this.lineOffsets.add(i + 1);
        }
        this.lineOffsets.add(input.length());
    }
    
    public Integer getLineIdxForOffset(Integer offset) {
        var low = 0;
        var high = this.lineOffsets.size() - 1;
        
        while (low <= high) {
            // @java var middle = (int)Math.floor((low + high) / 2);
            var middle = (int)Math.floor((low + high) / 2);
            var middleOffset = this.lineOffsets.get(middle);
            if (offset == middleOffset)
                return middle;
            else if (offset <= middleOffset)
                high = middle - 1;
            else
                low = middle + 1;
        }
        
        return low - 1;
    }
    
    public Cursor getCursorForOffset(Integer offset) {
        var lineIdx = this.getLineIdxForOffset(offset);
        var lineStart = this.lineOffsets.get(lineIdx);
        var lineEnd = this.lineOffsets.get(lineIdx + 1);
        var column = offset - lineStart + 1;
        if (column < 1)
            throw new Error("Column should not be < 1");
        return new Cursor(offset, lineIdx + 1, offset - lineStart + 1, lineStart, lineEnd);
    }
}