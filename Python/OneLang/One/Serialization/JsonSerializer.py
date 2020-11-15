from OneLangStdLib import *
import OneLang.One.Ast.AstTypes as astTypes
import OneLang.One.Ast.Interfaces as ints
import OneLang.One.Ast.Types as types
import OneLang.index as index
import re

class JsonSerializer:
    def __init__(self, lit_types):
        self.circle_detector = Map()
        self.lit_types = lit_types
    
    def pad(self, str):
        return "\n".join(list(map(lambda x: f'''    {x}''', re.split("\\n", str))))
    
    def serialize(self, obj):
        decl_type = obj.get_declared_type()
        if obj.is_null():
            return "null"
        elif astTypes.TypeHelper.equals(decl_type, self.lit_types.string):
            return JSON.stringify(obj.get_string_value())
        elif astTypes.TypeHelper.equals(decl_type, self.lit_types.boolean):
            return "true" if obj.get_boolean_value() else "false"
        elif astTypes.TypeHelper.is_assignable_to(decl_type, self.lit_types.array):
            items = []
            for item in obj.get_array_items():
                items.append(self.serialize(item))
            return "[]" if len(items) == 0 else f'''[\n{self.pad(",\n".join(items))}\n]'''
        elif astTypes.TypeHelper.is_assignable_to(decl_type, self.lit_types.map):
            items = []
            for key in obj.get_map_keys():
                value = obj.get_map_value(key)
                items.append(f'''"{key}": {self.serialize(value)}''')
            return "{}" if len(items) == 0 else f'''{{\n{self.pad(",\n".join(items))}\n}}'''
        elif isinstance(decl_type, astTypes.ClassType) or isinstance(decl_type, astTypes.InterfaceType):
            raw_value = obj.get_unique_identifier()
            if self.circle_detector.has(raw_value):
                return f'''{{"$ref":"{self.circle_detector.get(raw_value)}"}}'''
            id = f'''id_{self.circle_detector.size}'''
            self.circle_detector.set(raw_value, id)
            
            value_type = obj.get_value_type()
            decl = (decl_type).get_decl()
            
            members = []
            
            members.append(f'''"$id": "{id}"''')
            
            if value_type != None and not astTypes.TypeHelper.equals(value_type, decl_type):
                members.append(f'''"$type": "{value_type.repr()}"''')
            
            for field in list(filter(lambda x: not x.is_static, decl.fields)):
                if "json-ignore" in field.attributes:
                    continue
                #console.log(`processing ${field.parentInterface.name}::${field.name}`);
                value = obj.get_field(field.name)
                serialized_value = self.serialize(value)
                if not serialized_value in ["[]", "{}", "null", "false", "\"\""]:
                    members.append(f'''"{field.name}": {serialized_value}''')
            return "{}" if len(members) == 0 else f'''{{\n{self.pad(",\n".join(members))}\n}}'''
        elif isinstance(decl_type, astTypes.EnumType):
            return f'''"{obj.get_enum_value_as_string()}"'''
        return "\"<UNKNOWN-TYPE>\""