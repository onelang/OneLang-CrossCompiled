package OneLang.One.Transforms.InferTypesPlugins.Helpers.GenericsResolver;

import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.IMethodCallExpression;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.GenericsType;
import OneLang.One.Ast.AstTypes.InterfaceType;
import OneLang.One.Ast.AstTypes.LambdaType;
import OneLang.One.Ast.AstTypes.EnumType;
import OneLang.One.Ast.AstTypes.AnyType;
import OneLang.One.Ast.AstTypes.TypeHelper;
import OneLang.One.Ast.Types.MethodParameter;
import OneLang.One.Ast.Interfaces.IType;

import OneLang.One.Ast.Interfaces.IType;
import java.util.Map;
import java.util.LinkedHashMap;
import OneLang.One.Transforms.InferTypesPlugins.Helpers.GenericsResolver.GenericsResolver;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.IMethodCallExpression;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.InterfaceType;
import OneLang.One.Ast.AstTypes.GenericsType;
import OneStd.StdArrayHelper;
import OneLang.One.Ast.AstTypes.LambdaType;
import OneLang.One.Ast.AstTypes.EnumType;
import OneLang.One.Ast.AstTypes.AnyType;
import java.util.Arrays;
import OneLang.One.Ast.Types.MethodParameter;

public class GenericsResolver {
    public Map<String, IType> resolutionMap;
    
    public GenericsResolver()
    {
        this.resolutionMap = new LinkedHashMap<String, IType>();
    }
    
    public static GenericsResolver fromObject(Expression object) {
        var resolver = new GenericsResolver();
        resolver.collectClassGenericsFromObject(object);
        return resolver;
    }
    
    public void addResolution(String typeVarName, IType actualType) {
        var prevRes = this.resolutionMap.get(typeVarName);
        if (prevRes != null && !TypeHelper.equals(prevRes, actualType))
            throw new Error("Resolving '" + typeVarName + "' is ambiguous, " + prevRes.repr() + " <> " + actualType.repr());
        this.resolutionMap.put(typeVarName, actualType);
    }
    
    public void collectFromMethodCall(IMethodCallExpression methodCall) {
        if (methodCall.getTypeArgs().length == 0)
            return;
        if (methodCall.getTypeArgs().length != methodCall.getMethod().typeArguments.length)
            throw new Error("Expected " + methodCall.getMethod().typeArguments.length + " type argument(s) for method call, but got " + methodCall.getTypeArgs().length);
        for (Integer i = 0; i < methodCall.getTypeArgs().length; i++)
            this.addResolution(methodCall.getMethod().typeArguments[i], methodCall.getTypeArgs()[i]);
    }
    
    public void collectClassGenericsFromObject(Expression actualObject) {
        var actualType = actualObject.getType();
        if (actualType instanceof ClassType) {
            if (!this.collectResolutionsFromActualType(((ClassType)actualType).decl.type, ((ClassType)actualType))) { }
        }
        else if (actualType instanceof InterfaceType) {
            if (!this.collectResolutionsFromActualType(((InterfaceType)actualType).decl.type, ((InterfaceType)actualType))) { }
        }
        else
            throw new Error("Expected ClassType or InterfaceType, got " + (actualType != null ? actualType.repr() : "<null>"));
    }
    
    public Boolean collectResolutionsFromActualType(IType genericType, IType actualType) {
        if (!TypeHelper.isGeneric(genericType))
            return true;
        if (genericType instanceof GenericsType) {
            this.addResolution(((GenericsType)genericType).typeVarName, actualType);
            return true;
        }
        else if (genericType instanceof ClassType && actualType instanceof ClassType && ((ClassType)genericType).decl == ((ClassType)actualType).decl) {
            if (((ClassType)genericType).getTypeArguments().length != ((ClassType)actualType).getTypeArguments().length)
                throw new Error("Same class (" + ((ClassType)genericType).repr() + ") used with different number of type arguments (" + ((ClassType)genericType).getTypeArguments().length + " <> " + ((ClassType)actualType).getTypeArguments().length + ")");
            return StdArrayHelper.allMatch(((ClassType)genericType).getTypeArguments(), (x, i) -> this.collectResolutionsFromActualType(x, ((ClassType)actualType).getTypeArguments()[i]));
        }
        else if (genericType instanceof InterfaceType && actualType instanceof InterfaceType && ((InterfaceType)genericType).decl == ((InterfaceType)actualType).decl) {
            if (((InterfaceType)genericType).getTypeArguments().length != ((InterfaceType)actualType).getTypeArguments().length)
                throw new Error("Same class (" + ((InterfaceType)genericType).repr() + ") used with different number of type arguments (" + ((InterfaceType)genericType).getTypeArguments().length + " <> " + ((InterfaceType)actualType).getTypeArguments().length + ")");
            return StdArrayHelper.allMatch(((InterfaceType)genericType).getTypeArguments(), (x, i) -> this.collectResolutionsFromActualType(x, ((InterfaceType)actualType).getTypeArguments()[i]));
        }
        else if (genericType instanceof LambdaType && actualType instanceof LambdaType) {
            if (((LambdaType)genericType).parameters.length != ((LambdaType)actualType).parameters.length)
                throw new Error("Generic lambda type has " + ((LambdaType)genericType).parameters.length + " parameters while the actual type has " + ((LambdaType)actualType).parameters.length);
            var paramsOk = StdArrayHelper.allMatch(((LambdaType)genericType).parameters, (x, i) -> this.collectResolutionsFromActualType(x.getType(), ((LambdaType)actualType).parameters[i].getType()));
            var resultOk = this.collectResolutionsFromActualType(((LambdaType)genericType).returnType, ((LambdaType)actualType).returnType);
            return paramsOk && resultOk;
        }
        else if (genericType instanceof EnumType && actualType instanceof EnumType && ((EnumType)genericType).decl == ((EnumType)actualType).decl) { }
        else if (genericType instanceof AnyType || actualType instanceof AnyType) { }
        else
            throw new Error("Generic type " + genericType.repr() + " is not compatible with actual type " + actualType.repr());
        return false;
    }
    
    public IType resolveType(IType type, Boolean mustResolveAllGenerics) {
        if (type instanceof GenericsType) {
            var resolvedType = this.resolutionMap.get(((GenericsType)type).typeVarName);
            if (resolvedType == null && mustResolveAllGenerics)
                throw new Error("Could not resolve generics type: " + ((GenericsType)type).repr());
            return resolvedType != null ? resolvedType : ((GenericsType)type);
        }
        else if (type instanceof ClassType)
            return new ClassType(((ClassType)type).decl, Arrays.stream(((ClassType)type).getTypeArguments()).map(x -> this.resolveType(x, mustResolveAllGenerics)).toArray(IType[]::new));
        else if (type instanceof InterfaceType)
            return new InterfaceType(((InterfaceType)type).decl, Arrays.stream(((InterfaceType)type).getTypeArguments()).map(x -> this.resolveType(x, mustResolveAllGenerics)).toArray(IType[]::new));
        else if (type instanceof LambdaType)
            return new LambdaType(Arrays.stream(((LambdaType)type).parameters).map(x -> new MethodParameter(x.getName(), this.resolveType(x.getType(), mustResolveAllGenerics), x.getInitializer(), null)).toArray(MethodParameter[]::new), this.resolveType(((LambdaType)type).returnType, mustResolveAllGenerics));
        else
            return type;
    }
}