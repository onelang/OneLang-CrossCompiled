from OneLangStdLib import *
import re

class Utils:
    def __init__(self):
        pass
    
    @classmethod
    def get_pad_len(cls, line):
        i = 0
        
        while i < len(line):
            if line[i] != " ":
                return i
            i = i + 1
        return -1
    
    @classmethod
    def deindent(cls, str):
        lines = re.split("\\n", str)
        if len(lines) == 1:
            return str
        
        if Utils.get_pad_len(lines[0]) == -1:
            lines.pop(0)
        
        min_pad_len = 9999
        for pad_len in list(filter(lambda x: x != -1, list(map(lambda x: Utils.get_pad_len(x), lines)))):
            if pad_len < min_pad_len:
                min_pad_len = pad_len
        
        if min_pad_len == 9999:
            return "\n".join(list(map(lambda x: "", lines)))
        
        # @java final var minPadLen2 = minPadLen;
        min_pad_len2 = min_pad_len
        new_str = "\n".join(list(map(lambda x: x[min_pad_len2:] if len(x) != 0 else x, lines)))
        return new_str