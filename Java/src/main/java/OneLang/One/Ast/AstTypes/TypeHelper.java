package OneLang.One.Ast.AstTypes;

import OneLang.One.Ast.Types.Enum;
import OneLang.One.Ast.Types.Interface;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.MethodParameter;
import OneLang.One.Ast.Types.IInterface;
import OneLang.One.Ast.Interfaces.IType;

import java.util.Arrays;
import java.util.stream.Collectors;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.AstTypes.GenericsType;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.InterfaceType;
import OneLang.One.Ast.AstTypes.LambdaType;
import OneLang.One.Ast.AstTypes.VoidType;
import OneLang.One.Ast.AstTypes.AnyType;
import OneStd.Objects;
import OneLang.One.Ast.AstTypes.EnumType;
import OneStd.StdArrayHelper;
import OneLang.One.Ast.AstTypes.NullType;

public class TypeHelper {
    public static String argsRepr(IType[] args) {
        return args.length == 0 ? "" : "<" + Arrays.stream(Arrays.stream(args).map(x -> x.repr()).toArray(String[]::new)).collect(Collectors.joining(", ")) + ">";
    }
    
    public static Boolean isGeneric(IType type) {
        if (type instanceof GenericsType)
            return true;
        else if (type instanceof ClassType)
            return Arrays.stream(((ClassType)type).getTypeArguments()).anyMatch(x -> TypeHelper.isGeneric(x));
        else if (type instanceof InterfaceType)
            return Arrays.stream(((InterfaceType)type).getTypeArguments()).anyMatch(x -> TypeHelper.isGeneric(x));
        else if (type instanceof LambdaType)
            return Arrays.stream(((LambdaType)type).parameters).anyMatch(x -> TypeHelper.isGeneric(x.getType())) || TypeHelper.isGeneric(((LambdaType)type).returnType);
        else
            return false;
    }
    
    public static Boolean equals(IType type1, IType type2) {
        if (type1 == null || type2 == null)
            throw new Error("Type is missing!");
        if (type1 instanceof VoidType && type2 instanceof VoidType)
            return true;
        if (type1 instanceof AnyType && type2 instanceof AnyType)
            return true;
        if (type1 instanceof GenericsType && type2 instanceof GenericsType)
            return Objects.equals(((GenericsType)type1).typeVarName, ((GenericsType)type2).typeVarName);
        if (type1 instanceof EnumType && type2 instanceof EnumType)
            return ((EnumType)type1).decl == ((EnumType)type2).decl;
        if (type1 instanceof LambdaType && type2 instanceof LambdaType)
            return TypeHelper.equals(((LambdaType)type1).returnType, ((LambdaType)type2).returnType) && ((LambdaType)type1).parameters.length == ((LambdaType)type2).parameters.length && StdArrayHelper.allMatch(((LambdaType)type1).parameters, (t, i) -> TypeHelper.equals(t.getType(), ((LambdaType)type2).parameters[i].getType()));
        if (type1 instanceof ClassType && type2 instanceof ClassType)
            return ((ClassType)type1).decl == ((ClassType)type2).decl && ((ClassType)type1).getTypeArguments().length == ((ClassType)type2).getTypeArguments().length && StdArrayHelper.allMatch(((ClassType)type1).getTypeArguments(), (t, i) -> TypeHelper.equals(t, ((ClassType)type2).getTypeArguments()[i]));
        if (type1 instanceof InterfaceType && type2 instanceof InterfaceType)
            return ((InterfaceType)type1).decl == ((InterfaceType)type2).decl && ((InterfaceType)type1).getTypeArguments().length == ((InterfaceType)type2).getTypeArguments().length && StdArrayHelper.allMatch(((InterfaceType)type1).getTypeArguments(), (t, i) -> TypeHelper.equals(t, ((InterfaceType)type2).getTypeArguments()[i]));
        return false;
    }
    
    public static Boolean isAssignableTo(IType toBeAssigned, IType whereTo) {
        // AnyType can assigned to any type except to void
        if (toBeAssigned instanceof AnyType && !(whereTo instanceof VoidType))
            return true;
        // any type can assigned to AnyType except void
        if (whereTo instanceof AnyType && !(toBeAssigned instanceof VoidType))
            return true;
        // any type can assigned to GenericsType except void
        if (whereTo instanceof GenericsType && !(toBeAssigned instanceof VoidType))
            return true;
        // null can be assigned anywhere
        // TODO: filter out number and boolean types...
        if (toBeAssigned instanceof NullType && !(whereTo instanceof VoidType))
            return true;
        
        if (TypeHelper.equals(toBeAssigned, whereTo))
            return true;
        
        if (toBeAssigned instanceof ClassType && whereTo instanceof ClassType)
            return (((ClassType)toBeAssigned).decl.baseClass != null && TypeHelper.isAssignableTo(((ClassType)toBeAssigned).decl.baseClass, ((ClassType)whereTo))) || ((ClassType)toBeAssigned).decl == ((ClassType)whereTo).decl && StdArrayHelper.allMatch(((ClassType)toBeAssigned).getTypeArguments(), (x, i) -> TypeHelper.isAssignableTo(x, ((ClassType)whereTo).getTypeArguments()[i]));
        if (toBeAssigned instanceof ClassType && whereTo instanceof InterfaceType)
            return (((ClassType)toBeAssigned).decl.baseClass != null && TypeHelper.isAssignableTo(((ClassType)toBeAssigned).decl.baseClass, ((InterfaceType)whereTo))) || Arrays.stream(((ClassType)toBeAssigned).decl.getBaseInterfaces()).anyMatch(x -> TypeHelper.isAssignableTo(x, ((InterfaceType)whereTo)));
        if (toBeAssigned instanceof InterfaceType && whereTo instanceof InterfaceType)
            return Arrays.stream(((InterfaceType)toBeAssigned).decl.getBaseInterfaces()).anyMatch(x -> TypeHelper.isAssignableTo(x, ((InterfaceType)whereTo))) || ((InterfaceType)toBeAssigned).decl == ((InterfaceType)whereTo).decl && StdArrayHelper.allMatch(((InterfaceType)toBeAssigned).getTypeArguments(), (x, i) -> TypeHelper.isAssignableTo(x, ((InterfaceType)whereTo).getTypeArguments()[i]));
        if (toBeAssigned instanceof LambdaType && whereTo instanceof LambdaType)
            return ((LambdaType)toBeAssigned).parameters.length == ((LambdaType)whereTo).parameters.length && StdArrayHelper.allMatch(((LambdaType)toBeAssigned).parameters, (p, i) -> TypeHelper.isAssignableTo(p.getType(), ((LambdaType)whereTo).parameters[i].getType())) && (TypeHelper.isAssignableTo(((LambdaType)toBeAssigned).returnType, ((LambdaType)whereTo).returnType) || ((LambdaType)whereTo).returnType instanceof GenericsType);
        
        return false;
    }
}