using One.Ast;

namespace One.Transforms.InferTypesPlugins.Helpers
{
    public class GenericsResolver {
        public Map<string, IType> resolutionMap;
        
        public GenericsResolver()
        {
            this.resolutionMap = new Map<string, IType>();
        }
        
        public static GenericsResolver fromObject(Expression object_)
        {
            var resolver = new GenericsResolver();
            resolver.collectClassGenericsFromObject(object_);
            return resolver;
        }
        
        public void addResolution(string typeVarName, IType actualType)
        {
            var prevRes = this.resolutionMap.get(typeVarName);
            if (prevRes != null && !TypeHelper.equals(prevRes, actualType))
                throw new Error($"Resolving '{typeVarName}' is ambiguous, {prevRes.repr()} <> {actualType.repr()}");
            this.resolutionMap.set(typeVarName, actualType);
        }
        
        public void collectFromMethodCall(IMethodCallExpression methodCall)
        {
            if (methodCall.typeArgs.length() == 0)
                return;
            if (methodCall.typeArgs.length() != methodCall.method.typeArguments.length())
                throw new Error($"Expected {methodCall.method.typeArguments.length()} type argument(s) for method call, but got {methodCall.typeArgs.length()}");
            for (int i = 0; i < methodCall.typeArgs.length(); i++)
                this.addResolution(methodCall.method.typeArguments.get(i), methodCall.typeArgs.get(i));
        }
        
        public void collectClassGenericsFromObject(Expression actualObject)
        {
            var actualType = actualObject.getType();
            if (actualType is ClassType classType) {
                if (!this.collectResolutionsFromActualType(classType.decl.type, classType)) { }
            }
            else if (actualType is InterfaceType intType) {
                if (!this.collectResolutionsFromActualType(intType.decl.type, intType)) { }
            }
            else
                throw new Error($"Expected ClassType or InterfaceType, got {(actualType != null ? actualType.repr() : "<null>")}");
        }
        
        public bool collectResolutionsFromActualType(IType genericType, IType actualType)
        {
            if (!TypeHelper.isGeneric(genericType))
                return true;
            if (genericType is GenericsType genType) {
                this.addResolution(genType.typeVarName, actualType);
                return true;
            }
            else if (genericType is ClassType classType2 && actualType is ClassType classType3 && classType2.decl == classType3.decl) {
                if (classType2.typeArguments.length() != classType3.typeArguments.length())
                    throw new Error($"Same class ({classType2.repr()}) used with different number of type arguments ({classType2.typeArguments.length()} <> {classType3.typeArguments.length()})");
                return classType2.typeArguments.every((x, i) => this.collectResolutionsFromActualType(x, classType3.typeArguments.get(i)));
            }
            else if (genericType is InterfaceType intType2 && actualType is InterfaceType intType3 && intType2.decl == intType3.decl) {
                if (intType2.typeArguments.length() != intType3.typeArguments.length())
                    throw new Error($"Same class ({intType2.repr()}) used with different number of type arguments ({intType2.typeArguments.length()} <> {intType3.typeArguments.length()})");
                return intType2.typeArguments.every((x, i) => this.collectResolutionsFromActualType(x, intType3.typeArguments.get(i)));
            }
            else if (genericType is LambdaType lambdType && actualType is LambdaType lambdType2) {
                if (lambdType.parameters.length() != lambdType2.parameters.length())
                    throw new Error($"Generic lambda type has {lambdType.parameters.length()} parameters while the actual type has {lambdType2.parameters.length()}");
                var paramsOk = lambdType.parameters.every((x, i) => this.collectResolutionsFromActualType(x.type, lambdType2.parameters.get(i).type));
                var resultOk = this.collectResolutionsFromActualType(lambdType.returnType, lambdType2.returnType);
                return paramsOk && resultOk;
            }
            else if (genericType is EnumType enumType && actualType is EnumType enumType2 && enumType.decl == enumType2.decl) { }
            else if (genericType is AnyType || actualType is AnyType) { }
            else
                throw new Error($"Generic type {genericType.repr()} is not compatible with actual type {actualType.repr()}");
            return false;
        }
        
        public IType resolveType(IType type, bool mustResolveAllGenerics)
        {
            if (type is GenericsType genType2) {
                var resolvedType = this.resolutionMap.get(genType2.typeVarName);
                if (resolvedType == null && mustResolveAllGenerics)
                    throw new Error($"Could not resolve generics type: {genType2.repr()}");
                return resolvedType != null ? resolvedType : genType2;
            }
            else if (type is ClassType classType4)
                return new ClassType(classType4.decl, classType4.typeArguments.map(x => this.resolveType(x, mustResolveAllGenerics)));
            else if (type is InterfaceType intType4)
                return new InterfaceType(intType4.decl, intType4.typeArguments.map(x => this.resolveType(x, mustResolveAllGenerics)));
            else if (type is LambdaType lambdType3)
                return new LambdaType(lambdType3.parameters.map(x => new MethodParameter(x.name, this.resolveType(x.type, mustResolveAllGenerics), x.initializer, null)), this.resolveType(lambdType3.returnType, mustResolveAllGenerics));
            else
                return type;
        }
    }
}