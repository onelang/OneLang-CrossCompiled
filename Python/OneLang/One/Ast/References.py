from OneLangStdLib import *
import OneLang.One.Ast.Types as types
import OneLang.One.Ast.Statements as stats
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.AstTypes as astTypes
import OneLang.One.Ast.Interfaces as ints

class Reference(exprs.Expression):
    def __init__(self):
        super().__init__()

class VariableReference(Reference):
    def __init__(self):
        super().__init__()
    
    def get_variable(self):
        raise Error("Abstract method")

class ClassReference(Reference):
    def __init__(self, decl):
        self.decl = decl
        super().__init__()
        decl.class_references.append(self)
    
    def set_actual_type(self, type, allow_void = False, allow_generic = False):
        raise Error("ClassReference cannot have a type!")

class GlobalFunctionReference(Reference):
    def __init__(self, decl):
        self.decl = decl
        super().__init__()
        decl.references.append(self)
    
    def set_actual_type(self, type, allow_void = False, allow_generic = False):
        raise Error("GlobalFunctionReference cannot have a type!")
    
    def get_method_base(self):
        return self.decl

class MethodParameterReference(VariableReference):
    def __init__(self, decl):
        self.decl = decl
        super().__init__()
        decl.references.append(self)
    
    def set_actual_type(self, type, allow_void = False, allow_generic = False):
        super().set_actual_type(type, False, ArrayHelper.some(lambda x: astTypes.TypeHelper.is_generic(x.type), self.decl.parent_method.parameters) if isinstance(self.decl.parent_method, types.Lambda) else len(self.decl.parent_method.parent_class.type_arguments) > 0 if isinstance(self.decl.parent_method, types.Constructor) else len(self.decl.parent_method.type_arguments) > 0 or len(self.decl.parent_method.parent_interface.type_arguments) > 0 if isinstance(self.decl.parent_method, types.Method) else False)
    
    def get_variable(self):
        return self.decl

class EnumReference(Reference):
    def __init__(self, decl):
        self.decl = decl
        super().__init__()
        decl.references.append(self)
    
    def set_actual_type(self, type, allow_void = False, allow_generic = False):
        raise Error("EnumReference cannot have a type!")

class EnumMemberReference(Reference):
    def __init__(self, decl):
        self.decl = decl
        super().__init__()
        decl.references.append(self)
    
    def set_actual_type(self, type, allow_void = False, allow_generic = False):
        if not (isinstance(type, astTypes.EnumType)):
            raise Error("Expected EnumType!")
        super().set_actual_type(type)

class StaticThisReference(Reference):
    def __init__(self, cls_):
        self.cls_ = cls_
        super().__init__()
        cls_.static_this_references.append(self)
    
    def set_actual_type(self, type, allow_void = False, allow_generic = False):
        raise Error("StaticThisReference cannot have a type!")

class ThisReference(Reference):
    def __init__(self, cls_):
        self.cls_ = cls_
        super().__init__()
        cls_.this_references.append(self)
    
    def set_actual_type(self, type, allow_void = False, allow_generic = False):
        if not (isinstance(type, astTypes.ClassType)):
            raise Error("Expected ClassType!")
        super().set_actual_type(type, False, len(self.cls_.type_arguments) > 0)

class SuperReference(Reference):
    def __init__(self, cls_):
        self.cls_ = cls_
        super().__init__()
        cls_.super_references.append(self)
    
    def set_actual_type(self, type, allow_void = False, allow_generic = False):
        if not (isinstance(type, astTypes.ClassType)):
            raise Error("Expected ClassType!")
        super().set_actual_type(type, False, len(self.cls_.type_arguments) > 0)

class VariableDeclarationReference(VariableReference):
    def __init__(self, decl):
        self.decl = decl
        super().__init__()
        decl.references.append(self)
    
    def get_variable(self):
        return self.decl

class ForVariableReference(VariableReference):
    def __init__(self, decl):
        self.decl = decl
        super().__init__()
        decl.references.append(self)
    
    def get_variable(self):
        return self.decl

class CatchVariableReference(VariableReference):
    def __init__(self, decl):
        self.decl = decl
        super().__init__()
        decl.references.append(self)
    
    def get_variable(self):
        return self.decl

class ForeachVariableReference(VariableReference):
    def __init__(self, decl):
        self.decl = decl
        super().__init__()
        decl.references.append(self)
    
    def get_variable(self):
        return self.decl

class StaticFieldReference(VariableReference):
    def __init__(self, decl):
        self.decl = decl
        super().__init__()
        decl.static_references.append(self)
    
    def set_actual_type(self, type, allow_void = False, allow_generic = False):
        if astTypes.TypeHelper.is_generic(type):
            raise Error("StaticField's type cannot be Generic")
        super().set_actual_type(type)
    
    def get_variable(self):
        return self.decl

class StaticPropertyReference(VariableReference):
    def __init__(self, decl):
        self.decl = decl
        super().__init__()
        decl.static_references.append(self)
    
    def set_actual_type(self, type, allow_void = False, allow_generic = False):
        if astTypes.TypeHelper.is_generic(type):
            raise Error("StaticProperty's type cannot be Generic")
        super().set_actual_type(type)
    
    def get_variable(self):
        return self.decl

class InstanceFieldReference(VariableReference):
    def __init__(self, object, field):
        self.object = object
        self.field = field
        super().__init__()
        field.instance_references.append(self)
    
    def get_variable(self):
        return self.field

class InstancePropertyReference(VariableReference):
    def __init__(self, object, property):
        self.object = object
        self.property = property
        super().__init__()
        property.instance_references.append(self)
    
    def get_variable(self):
        return self.property