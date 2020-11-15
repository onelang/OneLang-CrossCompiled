from OneLangStdLib import *
from enum import Enum
import OneLang.One.Ast.AstTypes as astTypes
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.References as refs
import OneLang.One.Ast.AstHelper as astHelp
import OneLang.One.Ast.Statements as stats
import OneLang.One.Ast.Interfaces as ints
import OneLangStdLib as one

class VISIBILITY(Enum):
    PUBLIC = 1
    PROTECTED = 2
    PRIVATE = 3

class MutabilityInfo:
    def __init__(self, unused, reassigned, mutated):
        self.unused = unused
        self.reassigned = reassigned
        self.mutated = mutated

class ExportedScope:
    def __init__(self):
        self.exports = Map()
    
    def get_export(self, name):
        exp = self.exports.get(name)
        if exp == None:
            raise Error(f'''Export {name} was not found in exported symbols.''')
        return exp
    
    def add_export(self, name, value):
        self.exports.set(name, value)
    
    def get_all_exports(self):
        return Array.from_(self.exports.values())

@one.static_init
class Package:
    @classmethod
    def static_init(cls):
        cls.index = "index"
    
    def __init__(self, name, definition_only):
        self.name = name
        self.definition_only = definition_only
        self.files = {}
        self.exported_scopes = {}
    
    @classmethod
    def collect_exports_from_file(cls, file, export_all, scope = None):
        if scope == None:
            scope = ExportedScope()
        
        for cls_ in list(filter(lambda x: x.is_exported or export_all, file.classes)):
            scope.add_export(cls_.name, cls_)
        
        for intf in list(filter(lambda x: x.is_exported or export_all, file.interfaces)):
            scope.add_export(intf.name, intf)
        
        for enum_ in list(filter(lambda x: x.is_exported or export_all, file.enums)):
            scope.add_export(enum_.name, enum_)
        
        for func in list(filter(lambda x: x.is_exported or export_all, file.funcs)):
            scope.add_export(func.name, func)
        
        return scope
    
    def add_file(self, file, export_all = False):
        if file.source_path.pkg != self or file.export_scope.package_name != self.name:
            raise Error("This file belongs to another package!")
        
        self.files[file.source_path.path] = file
        scope_name = file.export_scope.scope_name
        self.exported_scopes[scope_name] = Package.collect_exports_from_file(file, export_all, self.exported_scopes.get(scope_name))
    
    def get_exported_scope(self, name):
        scope = self.exported_scopes.get(name)
        if scope == None:
            raise Error(f'''Scope "{name}" was not found in package "{self.name}"''')
        return scope

class Workspace:
    def __init__(self):
        self.packages = {}
    
    def add_package(self, pkg):
        self.packages[pkg.name] = pkg
    
    def get_package(self, name):
        pkg = self.packages.get(name)
        if pkg == None:
            raise Error(f'''Package was not found: "{name}"''')
        return pkg

class SourcePath:
    def __init__(self, pkg, path):
        self.pkg = pkg
        self.path = path
    
    def to_string(self):
        return f'''{self.pkg.name}/{self.path}'''

class LiteralTypes:
    def __init__(self, boolean, numeric, string, regex, array, map, error, promise):
        self.boolean = boolean
        self.numeric = numeric
        self.string = string
        self.regex = regex
        self.array = array
        self.map = map
        self.error = error
        self.promise = promise

class SourceFile:
    def __init__(self, imports, interfaces, classes, enums, funcs, main_block, source_path, export_scope):
        self.imports = imports
        self.interfaces = interfaces
        self.classes = classes
        self.enums = enums
        self.funcs = funcs
        self.main_block = main_block
        self.source_path = source_path
        self.export_scope = export_scope
        self.available_symbols = Map()
        self.literal_types = None
        self.array_types = []
        file_scope = Package.collect_exports_from_file(self, True)
        self.add_available_symbols(file_scope.get_all_exports())
    
    def add_available_symbols(self, items):
        for item in items:
            self.available_symbols.set(item.name, item)

class ExportScopeRef:
    def __init__(self, package_name, scope_name):
        self.package_name = package_name
        self.scope_name = scope_name
    
    def get_id(self):
        return f'''{self.package_name}.{self.scope_name}'''

class Import:
    def __init__(self, export_scope, import_all, imports, import_as, leading_trivia):
        self.export_scope = export_scope
        self.import_all = import_all
        self.imports = imports
        self.import_as = import_as
        self.leading_trivia = leading_trivia
        self.parent_file = None
        self.attributes = None
        if import_as != None and not import_all:
            raise Error("importAs only supported with importAll!")

class Enum:
    def __init__(self, name, values, is_exported, leading_trivia):
        self.name = name
        self.values = values
        self.is_exported = is_exported
        self.leading_trivia = leading_trivia
        self.parent_file = None
        self.attributes = None
        self.references = []
        self.type = astTypes.EnumType(self)
    
    def create_reference(self):
        return refs.EnumReference(self)

class EnumMember:
    def __init__(self, name):
        self.name = name
        self.parent_enum = None
        self.references = []

class UnresolvedImport:
    def __init__(self, name):
        self.name = name
        self.is_exported = True

class Interface:
    def __init__(self, name, type_arguments, base_interfaces, fields, methods, is_exported, leading_trivia):
        self.name = name
        self.type_arguments = type_arguments
        self.base_interfaces = base_interfaces
        self.fields = fields
        self.methods = methods
        self.is_exported = is_exported
        self.leading_trivia = leading_trivia
        self.parent_file = None
        self.attributes = None
        self.type = astTypes.InterfaceType(self, list(map(lambda x: astTypes.GenericsType(x), self.type_arguments)))
        self._base_interface_cache = None
    
    def get_all_base_interfaces(self):
        if self._base_interface_cache == None:
            self._base_interface_cache = astHelp.AstHelper.collect_all_base_interfaces(self)
        return self._base_interface_cache

class Class:
    def __init__(self, name, type_arguments, base_class, base_interfaces, fields, properties, constructor_, methods, is_exported, leading_trivia):
        self.name = name
        self.type_arguments = type_arguments
        self.base_class = base_class
        self.base_interfaces = base_interfaces
        self.fields = fields
        self.properties = properties
        self.constructor_ = constructor_
        self.methods = methods
        self.is_exported = is_exported
        self.leading_trivia = leading_trivia
        self.parent_file = None
        self.attributes = None
        self.class_references = []
        self.this_references = []
        self.static_this_references = []
        self.super_references = []
        self.type = astTypes.ClassType(self, list(map(lambda x: astTypes.GenericsType(x), self.type_arguments)))
        self._base_interface_cache = None
    
    def create_reference(self):
        return refs.ClassReference(self)
    
    def get_all_base_interfaces(self):
        if self._base_interface_cache == None:
            self._base_interface_cache = astHelp.AstHelper.collect_all_base_interfaces(self)
        return self._base_interface_cache

class Field:
    def __init__(self, name, type, initializer, visibility, is_static, constructor_param, leading_trivia):
        self.name = name
        self.type = type
        self.initializer = initializer
        self.visibility = visibility
        self.is_static = is_static
        self.constructor_param = constructor_param
        self.leading_trivia = leading_trivia
        self.parent_interface = None
        self.attributes = None
        self.static_references = []
        self.instance_references = []
        self.interface_declarations = None
        self.mutability = None

class Property:
    def __init__(self, name, type, getter, setter, visibility, is_static, leading_trivia):
        self.name = name
        self.type = type
        self.getter = getter
        self.setter = setter
        self.visibility = visibility
        self.is_static = is_static
        self.leading_trivia = leading_trivia
        self.parent_class = None
        self.attributes = None
        self.static_references = []
        self.instance_references = []
        self.mutability = None

class MethodParameter:
    def __init__(self, name, type, initializer, leading_trivia):
        self.name = name
        self.type = type
        self.initializer = initializer
        self.leading_trivia = leading_trivia
        self.field_decl = None
        self.parent_method = None
        self.attributes = None
        self.references = []
        self.mutability = None
    
    def create_reference(self):
        return refs.MethodParameterReference(self)

class Constructor:
    def __init__(self, parameters, body, super_call_args, leading_trivia):
        self.parameters = parameters
        self.body = body
        self.super_call_args = super_call_args
        self.leading_trivia = leading_trivia
        self.parent_class = None
        self.attributes = None
        self.throws = None

class Method:
    def __init__(self, name, type_arguments, parameters, body, visibility, is_static, returns, async_, leading_trivia):
        self.name = name
        self.type_arguments = type_arguments
        self.parameters = parameters
        self.body = body
        self.visibility = visibility
        self.is_static = is_static
        self.returns = returns
        self.async_ = async_
        self.leading_trivia = leading_trivia
        self.parent_interface = None
        self.attributes = None
        self.interface_declarations = None
        self.overrides = None
        self.overridden_by = []
        self.throws = None

class GlobalFunction:
    def __init__(self, name, parameters, body, returns, is_exported, leading_trivia):
        self.name = name
        self.parameters = parameters
        self.body = body
        self.returns = returns
        self.is_exported = is_exported
        self.leading_trivia = leading_trivia
        self.parent_file = None
        self.attributes = None
        self.throws = None
        self.references = []
    
    def create_reference(self):
        return refs.GlobalFunctionReference(self)

class Lambda(exprs.Expression):
    def __init__(self, parameters, body):
        self.parameters = parameters
        self.body = body
        self.returns = None
        self.throws = None
        self.captures = None
        super().__init__()