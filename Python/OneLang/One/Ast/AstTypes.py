from OneLangStdLib import *
import OneLang.One.Ast.Interfaces as ints
import OneLangStdLib as one

class TypeHelper:
    def __init__(self):
        pass
    
    @classmethod
    def args_repr(cls, args):
        return "" if len(args) == 0 else f'''<{", ".join(list(map(lambda x: x.repr(), args)))}>'''
    
    @classmethod
    def is_generic(cls, type):
        if isinstance(type, GenericsType):
            return True
        elif isinstance(type, ClassType):
            return ArrayHelper.some(lambda x: TypeHelper.is_generic(x), type.type_arguments)
        elif isinstance(type, InterfaceType):
            return ArrayHelper.some(lambda x: TypeHelper.is_generic(x), type.type_arguments)
        elif isinstance(type, LambdaType):
            return ArrayHelper.some(lambda x: TypeHelper.is_generic(x.type), type.parameters) or TypeHelper.is_generic(type.return_type)
        else:
            return False
    
    @classmethod
    def equals(cls, type1, type2):
        if type1 == None or type2 == None:
            raise Error("Type is missing!")
        if isinstance(type1, VoidType) and isinstance(type2, VoidType):
            return True
        if isinstance(type1, AnyType) and isinstance(type2, AnyType):
            return True
        if isinstance(type1, GenericsType) and isinstance(type2, GenericsType):
            return type1.type_var_name == type2.type_var_name
        if isinstance(type1, EnumType) and isinstance(type2, EnumType):
            return type1.decl == type2.decl
        if isinstance(type1, LambdaType) and isinstance(type2, LambdaType):
            return TypeHelper.equals(type1.return_type, type2.return_type) and len(type1.parameters) == len(type2.parameters) and ArrayHelper.every(lambda t, i: TypeHelper.equals(t.type, type2.parameters[i].type), type1.parameters)
        if isinstance(type1, ClassType) and isinstance(type2, ClassType):
            return type1.decl == type2.decl and len(type1.type_arguments) == len(type2.type_arguments) and ArrayHelper.every(lambda t, i: TypeHelper.equals(t, type2.type_arguments[i]), type1.type_arguments)
        if isinstance(type1, InterfaceType) and isinstance(type2, InterfaceType):
            return type1.decl == type2.decl and len(type1.type_arguments) == len(type2.type_arguments) and ArrayHelper.every(lambda t, i: TypeHelper.equals(t, type2.type_arguments[i]), type1.type_arguments)
        return False
    
    @classmethod
    def is_assignable_to(cls, to_be_assigned, where_to):
        # AnyType can assigned to any type except to void
        if isinstance(to_be_assigned, AnyType) and not (isinstance(where_to, VoidType)):
            return True
        # any type can assigned to AnyType except void
        if isinstance(where_to, AnyType) and not (isinstance(to_be_assigned, VoidType)):
            return True
        # any type can assigned to GenericsType except void
        if isinstance(where_to, GenericsType) and not (isinstance(to_be_assigned, VoidType)):
            return True
        # null can be assigned anywhere
        # TODO: filter out number and boolean types...
        if isinstance(to_be_assigned, NullType) and not (isinstance(where_to, VoidType)):
            return True
        
        if TypeHelper.equals(to_be_assigned, where_to):
            return True
        
        if isinstance(to_be_assigned, ClassType) and isinstance(where_to, ClassType):
            return (to_be_assigned.decl.base_class != None and TypeHelper.is_assignable_to(to_be_assigned.decl.base_class, where_to)) or to_be_assigned.decl == where_to.decl and ArrayHelper.every(lambda x, i: TypeHelper.is_assignable_to(x, where_to.type_arguments[i]), to_be_assigned.type_arguments)
        if isinstance(to_be_assigned, ClassType) and isinstance(where_to, InterfaceType):
            return (to_be_assigned.decl.base_class != None and TypeHelper.is_assignable_to(to_be_assigned.decl.base_class, where_to)) or ArrayHelper.some(lambda x: TypeHelper.is_assignable_to(x, where_to), to_be_assigned.decl.base_interfaces)
        if isinstance(to_be_assigned, InterfaceType) and isinstance(where_to, InterfaceType):
            return ArrayHelper.some(lambda x: TypeHelper.is_assignable_to(x, where_to), to_be_assigned.decl.base_interfaces) or to_be_assigned.decl == where_to.decl and ArrayHelper.every(lambda x, i: TypeHelper.is_assignable_to(x, where_to.type_arguments[i]), to_be_assigned.type_arguments)
        if isinstance(to_be_assigned, LambdaType) and isinstance(where_to, LambdaType):
            return len(to_be_assigned.parameters) == len(where_to.parameters) and ArrayHelper.every(lambda p, i: TypeHelper.is_assignable_to(p.type, where_to.parameters[i].type), to_be_assigned.parameters) and (TypeHelper.is_assignable_to(to_be_assigned.return_type, where_to.return_type) or isinstance(where_to.return_type, GenericsType))
        
        return False

@one.static_init
class VoidType:
    @classmethod
    def static_init(cls):
        cls.instance = cls()
    
    def __init__(self):
        pass
    
    def repr(self):
        return "Void"

@one.static_init
class AnyType:
    @classmethod
    def static_init(cls):
        cls.instance = cls()
    
    def __init__(self):
        pass
    
    def repr(self):
        return "Any"

@one.static_init
class NullType:
    @classmethod
    def static_init(cls):
        cls.instance = cls()
    
    def __init__(self):
        pass
    
    def repr(self):
        return "Null"

class GenericsType:
    def __init__(self, type_var_name):
        self.type_var_name = type_var_name
    
    def repr(self):
        return f'''G:{self.type_var_name}'''

class EnumType:
    def __init__(self, decl):
        self.decl = decl
    
    def repr(self):
        return f'''E:{self.decl.name}'''

class InterfaceType:
    def __init__(self, decl, type_arguments):
        self.decl = decl
        self.type_arguments = type_arguments
    
    def get_decl(self):
        return self.decl
    
    def repr(self):
        return f'''I:{self.decl.name}{TypeHelper.args_repr(self.type_arguments)}'''

class ClassType:
    def __init__(self, decl, type_arguments):
        self.decl = decl
        self.type_arguments = type_arguments
    
    def get_decl(self):
        return self.decl
    
    def repr(self):
        return f'''C:{self.decl.name}{TypeHelper.args_repr(self.type_arguments)}'''

class UnresolvedType:
    def __init__(self, type_name, type_arguments):
        self.type_name = type_name
        self.type_arguments = type_arguments
    
    def repr(self):
        return f'''X:{self.type_name}{TypeHelper.args_repr(self.type_arguments)}'''

class LambdaType:
    def __init__(self, parameters, return_type):
        self.parameters = parameters
        self.return_type = return_type
    
    def repr(self):
        return f'''L:({", ".join(list(map(lambda x: x.type.repr(), self.parameters)))})=>{self.return_type.repr()}'''