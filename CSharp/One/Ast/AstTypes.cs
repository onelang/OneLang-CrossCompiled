using One.Ast;

namespace One.Ast
{
    public interface IPrimitiveType : IType {
        
    }
    
    public interface IHasTypeArguments {
        IType[] typeArguments { get; set; }
    }
    
    public interface IInterfaceType : IType {
        IType[] typeArguments { get; set; }
        
        IInterface getDecl();
    }
    
    public class TypeHelper {
        public static string argsRepr(IType[] args)
        {
            return args.length() == 0 ? "" : $"<{args.map(x => x.repr()).join(", ")}>";
        }
        
        public static bool isGeneric(IType type)
        {
            if (type is GenericsType)
                return true;
            else if (type is ClassType classType)
                return classType.typeArguments.some(x => TypeHelper.isGeneric(x));
            else if (type is InterfaceType intType)
                return intType.typeArguments.some(x => TypeHelper.isGeneric(x));
            else if (type is LambdaType lambdType)
                return lambdType.parameters.some(x => TypeHelper.isGeneric(x.type)) || TypeHelper.isGeneric(lambdType.returnType);
            else
                return false;
        }
        
        public static bool equals(IType type1, IType type2)
        {
            if (type1 == null || type2 == null)
                throw new Error("Type is missing!");
            if (type1 is VoidType && type2 is VoidType)
                return true;
            if (type1 is AnyType && type2 is AnyType)
                return true;
            if (type1 is GenericsType genType && type2 is GenericsType genType2)
                return genType.typeVarName == genType2.typeVarName;
            if (type1 is EnumType enumType && type2 is EnumType enumType2)
                return enumType.decl == enumType2.decl;
            if (type1 is LambdaType lambdType2 && type2 is LambdaType lambdType3)
                return TypeHelper.equals(lambdType2.returnType, lambdType3.returnType) && lambdType2.parameters.length() == lambdType3.parameters.length() && lambdType2.parameters.every((t, i) => TypeHelper.equals(t.type, lambdType3.parameters.get(i).type));
            if (type1 is ClassType classType2 && type2 is ClassType classType3)
                return classType2.decl == classType3.decl && classType2.typeArguments.length() == classType3.typeArguments.length() && classType2.typeArguments.every((t, i) => TypeHelper.equals(t, classType3.typeArguments.get(i)));
            if (type1 is InterfaceType intType2 && type2 is InterfaceType intType3)
                return intType2.decl == intType3.decl && intType2.typeArguments.length() == intType3.typeArguments.length() && intType2.typeArguments.every((t, i) => TypeHelper.equals(t, intType3.typeArguments.get(i)));
            return false;
        }
        
        public static bool isAssignableTo(IType toBeAssigned, IType whereTo)
        {
            // AnyType can assigned to any type except to void
            if (toBeAssigned is AnyType && !(whereTo is VoidType))
                return true;
            // any type can assigned to AnyType except void
            if (whereTo is AnyType && !(toBeAssigned is VoidType))
                return true;
            // any type can assigned to GenericsType except void
            if (whereTo is GenericsType && !(toBeAssigned is VoidType))
                return true;
            // null can be assigned anywhere
            // TODO: filter out number and boolean types...
            if (toBeAssigned is NullType && !(whereTo is VoidType))
                return true;
            
            if (TypeHelper.equals(toBeAssigned, whereTo))
                return true;
            
            if (toBeAssigned is ClassType classType4 && whereTo is ClassType classType5)
                return (classType4.decl.baseClass != null && TypeHelper.isAssignableTo(classType4.decl.baseClass, classType5)) || classType4.decl == classType5.decl && classType4.typeArguments.every((x, i) => TypeHelper.isAssignableTo(x, classType5.typeArguments.get(i)));
            if (toBeAssigned is ClassType classType6 && whereTo is InterfaceType intType4)
                return (classType6.decl.baseClass != null && TypeHelper.isAssignableTo(classType6.decl.baseClass, intType4)) || classType6.decl.baseInterfaces.some(x => TypeHelper.isAssignableTo(x, intType4));
            if (toBeAssigned is InterfaceType intType5 && whereTo is InterfaceType intType6)
                return intType5.decl.baseInterfaces.some(x => TypeHelper.isAssignableTo(x, intType6)) || intType5.decl == intType6.decl && intType5.typeArguments.every((x, i) => TypeHelper.isAssignableTo(x, intType6.typeArguments.get(i)));
            if (toBeAssigned is LambdaType lambdType4 && whereTo is LambdaType lambdType5)
                return lambdType4.parameters.length() == lambdType5.parameters.length() && lambdType4.parameters.every((p, i) => TypeHelper.isAssignableTo(p.type, lambdType5.parameters.get(i).type)) && (TypeHelper.isAssignableTo(lambdType4.returnType, lambdType5.returnType) || lambdType5.returnType is GenericsType);
            
            return false;
        }
    }
    
    public class VoidType : IPrimitiveType {
        public static VoidType instance;
        
        static VoidType()
        {
            VoidType.instance = new VoidType();
        }
        
        public string repr()
        {
            return "Void";
        }
    }
    
    public class AnyType : IPrimitiveType {
        public static AnyType instance;
        
        static AnyType()
        {
            AnyType.instance = new AnyType();
        }
        
        public string repr()
        {
            return "Any";
        }
    }
    
    public class NullType : IPrimitiveType {
        public static NullType instance;
        
        static NullType()
        {
            NullType.instance = new NullType();
        }
        
        public string repr()
        {
            return "Null";
        }
    }
    
    public class GenericsType : IType {
        public string typeVarName;
        
        public GenericsType(string typeVarName)
        {
            this.typeVarName = typeVarName;
        }
        
        public string repr()
        {
            return $"G:{this.typeVarName}";
        }
    }
    
    public class EnumType : IType {
        public Enum_ decl;
        
        public EnumType(Enum_ decl)
        {
            this.decl = decl;
        }
        
        public string repr()
        {
            return $"E:{this.decl.name}";
        }
    }
    
    public class InterfaceType : IType, IHasTypeArguments, IInterfaceType {
        public Interface decl;
        public IType[] typeArguments { get; set; }
        
        public InterfaceType(Interface decl, IType[] typeArguments)
        {
            this.decl = decl;
            this.typeArguments = typeArguments;
        }
        
        public IInterface getDecl()
        {
            return this.decl;
        }
        
        public string repr()
        {
            return $"I:{this.decl.name}{TypeHelper.argsRepr(this.typeArguments)}";
        }
    }
    
    public class ClassType : IType, IHasTypeArguments, IInterfaceType {
        public Class decl;
        public IType[] typeArguments { get; set; }
        
        public ClassType(Class decl, IType[] typeArguments)
        {
            this.decl = decl;
            this.typeArguments = typeArguments;
        }
        
        public IInterface getDecl()
        {
            return this.decl;
        }
        
        public string repr()
        {
            return $"C:{this.decl.name}{TypeHelper.argsRepr(this.typeArguments)}";
        }
    }
    
    public class UnresolvedType : IType, IHasTypeArguments {
        public string typeName;
        public IType[] typeArguments { get; set; }
        
        public UnresolvedType(string typeName, IType[] typeArguments)
        {
            this.typeName = typeName;
            this.typeArguments = typeArguments;
        }
        
        public string repr()
        {
            return $"X:{this.typeName}{TypeHelper.argsRepr(this.typeArguments)}";
        }
    }
    
    public class LambdaType : IType {
        public MethodParameter[] parameters;
        public IType returnType;
        
        public LambdaType(MethodParameter[] parameters, IType returnType)
        {
            this.parameters = parameters;
            this.returnType = returnType;
        }
        
        public string repr()
        {
            return $"L:({this.parameters.map(x => x.type.repr()).join(", ")})=>{this.returnType.repr()}";
        }
    }
}