from OneLangStdLib import *
from enum import Enum
import OneLang.One.Ast.AstTypes as astTypes
import OneLang.One.Ast.Types as types

class DETECTION_MODE(Enum):
    ALL_IMPORTS = 1
    ALL_INHERITENCE = 2
    BASE_CLASSES_ONLY = 3

class GraphCycleDetector:
    def __init__(self, visitor):
        self.node_is_in_path = None
        self.visitor = visitor
    
    def find_cycles(self, nodes):
        self.node_is_in_path = Map()
        for node in nodes:
            self.visit_node(node)
    
    def visit_node(self, node):
        if not self.node_is_in_path.has(node):
            # untouched node
            self.node_is_in_path.set(node, True)
            self.visitor.process_node(node)
            self.node_is_in_path.set(node, False)
            return False
        else:
            # true = node used in current path = cycle
            # false = node was already scanned previously (not a cycle)
            return self.node_is_in_path.get(node)

class CircularDependencyDetector:
    def __init__(self, detection_mode):
        self.detector = GraphCycleDetector(self)
        self.detection_mode = detection_mode
    
    def process_intfs(self, file, type, intfs):
        for intf in intfs:
            for base_intf in intf.get_all_base_interfaces():
                if base_intf.parent_file != file and self.detector.visit_node(base_intf.parent_file):
                    console.error(f'''Circular dependency found in file \'{file.export_scope.get_id()}\': {type} \'{intf.name}\' inherited from \'{base_intf.name}\' (from \'{base_intf.parent_file.export_scope.get_id()}\')''')
    
    def process_node(self, file):
        if self.detection_mode == DETECTION_MODE.ALL_IMPORTS:
            for imp in file.imports:
                for imp_sym in imp.imports:
                    imp_file = (imp_sym).parent_file
                    if self.detector.visit_node(imp_file):
                        console.error(f'''Circular dependency found in file \'{file.export_scope.get_id()}\' via the import \'{imp_sym.name}\' imported from \'{imp_file.export_scope.get_id()}\'''')
        elif self.detection_mode == DETECTION_MODE.ALL_INHERITENCE:
            self.process_intfs(file, "class", file.classes)
            self.process_intfs(file, "interface", file.interfaces)
        elif self.detection_mode == DETECTION_MODE.BASE_CLASSES_ONLY:
            for cls_ in file.classes:
                base_class = (cls_.base_class).decl
                if base_class.parent_file != file and self.detector.visit_node(base_class.parent_file):
                    console.error(f'''Circular dependency found in file \'{file.export_scope.get_id()}\': class \'{cls_.name}\' inherited from \'{base_class.name}\' (from \'{base_class.parent_file.export_scope.get_id()}\')''')
    
    def process_package(self, pkg):
        self.detector.find_cycles(pkg.files.values())
    
    def process_workspace(self, ws):
        for pkg in ws.packages.values():
            self.process_package(pkg)