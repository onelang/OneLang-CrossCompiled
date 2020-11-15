package OneLang.Parsers.Common.Reader;

import OneLang.Parsers.Common.Utils.Utils;

import OneLang.Parsers.Common.Reader.ParseError;

public interface IReaderHooks {
    void errorCallback(ParseError error);
}