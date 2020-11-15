from OneLangStdLib import *
import OneLang.Parsers.Common.Reader as read

class NodeManager:
    def __init__(self, reader):
        self.nodes = []
        self.reader = reader
    
    def add_node(self, node, start):
        self.nodes.append(node)