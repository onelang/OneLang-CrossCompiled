from OneLangStdLib import *
import OneLang.One.Ast.Types as types
import OneLang.One.ITransformer as iTrans

class CollectInheritanceInfo:
    def __init__(self):
        self.name = "CollectInheritanceInfo"
        # C# fix
        self.name = "CollectInheritanceInfo"
    
    def visit_class(self, cls_):
        all_base_iintfs = cls_.get_all_base_interfaces()
        intfs = list(filter(lambda x: x != None, list(map(lambda x: x if isinstance(x, types.Interface) else None, all_base_iintfs))))
        clses = list(filter(lambda x: x != None and x != cls_, list(map(lambda x: x if isinstance(x, types.Class) else None, all_base_iintfs))))
        
        for field in cls_.fields:
            field.interface_declarations = list(filter(lambda x: x != None, list(map(lambda x: next(filter(lambda f: f.name == field.name, x.fields), None), intfs))))
        
        for method in cls_.methods:
            method.interface_declarations = list(filter(lambda x: x != None, list(map(lambda x: next(filter(lambda m: m.name == method.name, x.methods), None), intfs))))
            method.overrides = next(filter(lambda x: x != None, list(map(lambda x: next(filter(lambda m: m.name == method.name, x.methods), None), clses))), None)
            if method.overrides != None:
                method.overrides.overridden_by.append(method)
    
    def visit_files(self, files):
        for file in files:
            for cls_ in file.classes:
                self.visit_class(cls_)