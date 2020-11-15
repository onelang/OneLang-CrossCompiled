package OneLang.Parsers.Common.Reader;

import OneLang.Parsers.Common.Utils.Utils;

import OneLang.Parsers.Common.Reader.Cursor;
import OneLang.Parsers.Common.Reader.Reader;

public class ParseError {
    public String message;
    public Cursor cursor;
    public Reader reader;
    
    public ParseError(String message, Cursor cursor, Reader reader)
    {
        this.message = message;
        this.cursor = cursor;
        this.reader = reader;
    }
}