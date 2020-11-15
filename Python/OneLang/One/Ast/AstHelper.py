from OneLangStdLib import *
import OneLang.One.Ast.Types as types
import OneLang.One.Ast.AstTypes as astTypes

class AstHelper:
    def __init__(self):
        pass
    
    @classmethod
    def collect_all_base_interfaces(cls, intf):
        result = dict()
        to_be_processed = [intf]
        
        while len(to_be_processed) > 0:
            curr = to_be_processed.pop()
            result[curr] = None
            
            if isinstance(curr, types.Class) and curr.base_class != None:
                to_be_processed.append((curr.base_class).decl)
            
            for base_intf in curr.base_interfaces:
                to_be_processed.append((base_intf).decl)
        
        return Array.from_(result.keys())