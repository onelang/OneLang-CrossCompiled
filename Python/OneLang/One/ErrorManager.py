from OneLangStdLib import *
from enum import Enum
import OneLang.One.Ast.Types as types
import OneLang.One.AstTransformer as astTrans
import OneLang.One.Ast.Statements as stats
import OneLang.Utils.TSOverviewGenerator as tSOvervGen
import OneLang.One.Ast.Expressions as exprs

class LOG_TYPE(Enum):
    INFO = 1
    WARNING = 2
    ERROR = 3

class CompilationError:
    def __init__(self, msg, is_warning, transformer_name, node):
        self.msg = msg
        self.is_warning = is_warning
        self.transformer_name = transformer_name
        self.node = node

class ErrorManager:
    def __init__(self):
        self.transformer = None
        self.current_node = None
        self.errors = []
        self.last_context_info = None
    
    def get_location(self):
        t = self.transformer
        
        par = self.current_node
        while isinstance(par, exprs.Expression):
            par = par.parent_node
        
        location = None
        if isinstance(par, types.Field):
            location = f'''{par.parent_interface.parent_file.source_path.path} -> {par.parent_interface.name}::{par.name} (field)'''
        elif isinstance(par, types.Property):
            location = f'''{par.parent_class.parent_file.source_path.path} -> {par.parent_class.name}::{par.name} (property)'''
        elif isinstance(par, types.Method):
            location = f'''{par.parent_interface.parent_file.source_path.path} -> {par.parent_interface.name}::{par.name} (method)'''
        elif isinstance(par, types.Constructor):
            location = f'''{par.parent_class.parent_file.source_path.path} -> {par.parent_class.name}::constructor'''
        elif par == None:
            pass
        elif isinstance(par, stats.Statement):
            pass
        else:
            pass
        
        if location == None and t != None and t.current_file != None:
            location = f'''{t.current_file.source_path.path}'''
            if t.current_interface != None:
                location += f''' -> {t.current_interface.name}'''
                if isinstance(t.current_method, types.Method):
                    location += f'''::{t.current_method.name}'''
                elif isinstance(t.current_method, types.Constructor):
                    location += f'''::constructor'''
                elif isinstance(t.current_method, types.Lambda):
                    location += f'''::<lambda>'''
                elif t.current_method == None:
                    pass
                else:
                    pass
        
        return location
    
    def get_current_node_repr(self):
        return tSOvervGen.TSOverviewGenerator.preview.node_repr(self.current_node)
    
    def get_current_statement_repr(self):
        return "<null>" if self.transformer.current_statement == None else tSOvervGen.TSOverviewGenerator.preview.stmt(self.transformer.current_statement)
    
    def reset_context(self, transformer = None):
        self.transformer = transformer
    
    def log(self, type, msg):
        t = self.transformer
        text = (f'''[{t.name}] ''' if t != None else "") + msg
        
        if self.current_node != None:
            text += f'''\n  Node: {self.get_current_node_repr()}'''
        
        location = self.get_location()
        if location != None:
            text += f'''\n  Location: {location}'''
        
        if t != None and t.current_statement != None:
            text += f'''\n  Statement: {self.get_current_statement_repr()}'''
        
        if self.last_context_info != None:
            text += f'''\n  Context: {self.last_context_info}'''
        
        if type == LOG_TYPE.INFO:
            console.log(text)
        elif type == LOG_TYPE.WARNING:
            console.error(f'''[WARNING] {text}\n''')
        elif type == LOG_TYPE.ERROR:
            console.error(f'''{text}\n''')
        else:
            pass
        
        if type == LOG_TYPE.ERROR or type == LOG_TYPE.WARNING:
            self.errors.append(CompilationError(msg, type == LOG_TYPE.WARNING, t.name if t != None else None, self.current_node))
    
    def info(self, msg):
        self.log(LOG_TYPE.INFO, msg)
    
    def warn(self, msg):
        self.log(LOG_TYPE.WARNING, msg)
    
    def throw(self, msg):
        self.log(LOG_TYPE.ERROR, msg)