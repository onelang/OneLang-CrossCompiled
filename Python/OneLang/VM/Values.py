from onelang_core import *

class ObjectValue:
    def __init__(self, props):
        self.props = props

class StringValue:
    def __init__(self, value):
        self.value = value

class BooleanValue:
    def __init__(self, value):
        self.value = value

class ArrayValue:
    def __init__(self, items):
        self.items = items