from onelang_core import *

class ObjectValue:
    def __init__(self, props):
        self.props = props
    
    def equals(self, other):
        return False

class StringValue:
    def __init__(self, value):
        self.value = value
    
    def equals(self, other):
        return isinstance(other, StringValue) and other.value == self.value

class NumericValue:
    def __init__(self, value):
        self.value = value
    
    def equals(self, other):
        return isinstance(other, NumericValue) and other.value == self.value

class BooleanValue:
    def __init__(self, value):
        self.value = value
    
    def equals(self, other):
        return isinstance(other, BooleanValue) and other.value == self.value

class ArrayValue:
    def __init__(self, items):
        self.items = items
    
    def equals(self, other):
        return False