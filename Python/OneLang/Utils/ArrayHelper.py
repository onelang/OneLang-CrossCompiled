from OneLangStdLib import *

class ArrayHelper:
    def __init__(self):
        pass
    
    @classmethod
    def sort_by(cls, items, key_selector):
        # @java-import java.util.Arrays
        # @java Arrays.sort(items, (a, b) -> keySelector.apply(a) - keySelector.apply(b));
        # @java return items;
        return items.sort(lambda a, b: key_selector(a) - key_selector(b))
    
    @classmethod
    def remove_last_n(cls, items, count):
        # @java items.subList(items.size() - count, items.size()).clear();
        items.splice(len(items) - count, count)