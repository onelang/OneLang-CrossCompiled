package OneLang.One.Transforms.InferTypesPlugins.ResolveMethodCalls;

import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.UnresolvedMethodCallExpression;
import OneLang.One.Ast.Expressions.InstanceMethodCallExpression;
import OneLang.One.Ast.Expressions.StaticMethodCallExpression;
import OneLang.One.Ast.Expressions.IMethodCallExpression;
import OneLang.One.Ast.Expressions.LambdaCallExpression;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.InterfaceType;
import OneLang.One.Ast.AstTypes.AnyType;
import OneLang.One.Ast.AstTypes.TypeHelper;
import OneLang.One.Ast.AstTypes.LambdaType;
import OneLang.One.Transforms.InferTypesPlugins.Helpers.GenericsResolver.GenericsResolver;
import OneLang.One.Ast.References.ClassReference;
import OneLang.One.Ast.References.InstanceFieldReference;
import OneLang.One.Ast.References.StaticThisReference;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.IInterface;
import OneLang.One.Ast.Types.Method;

import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Types.Class;
import java.util.Arrays;
import OneLang.One.Ast.Types.IInterface;
import java.util.ArrayList;
import OneLang.One.Ast.Types.MethodParameter;
import io.onelang.std.core.Objects;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.InstanceMethodCallExpression;
import OneLang.One.Ast.Expressions.IMethodCallExpression;
import OneLang.One.Transforms.InferTypesPlugins.Helpers.GenericsResolver.GenericsResolver;
import OneLang.One.Ast.References.ClassReference;
import OneLang.One.Ast.References.StaticThisReference;
import OneLang.One.Ast.Expressions.StaticMethodCallExpression;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.InterfaceType;
import OneLang.One.Ast.AstTypes.LambdaType;
import OneLang.One.Ast.Expressions.LambdaCallExpression;
import OneLang.One.Ast.References.InstanceFieldReference;
import OneLang.One.Ast.AstTypes.AnyType;
import OneLang.One.Ast.Expressions.UnresolvedMethodCallExpression;

public class ResolveMethodCalls extends InferTypesPlugin {
    public ResolveMethodCalls()
    {
        super("ResolveMethodCalls");
        
    }
    
    protected Method findMethod(IInterface cls, String methodName, Boolean isStatic, Expression[] args) {
        var allBases = cls instanceof Class ? Arrays.stream(((Class)cls).getAllBaseInterfaces()).filter(x -> x instanceof Class).toArray(IInterface[]::new) : cls.getAllBaseInterfaces();
        
        var methods = new ArrayList<Method>();
        for (var base : allBases)
            for (var m : base.getMethods()) {
                var minLen = Arrays.stream(m.getParameters()).filter(p -> p.getInitializer() == null).toArray(MethodParameter[]::new).length;
                var maxLen = m.getParameters().length;
                var match = Objects.equals(m.getName(), methodName) && m.getIsStatic() == isStatic && minLen <= args.length && args.length <= maxLen;
                if (match)
                    methods.add(m);
            }
        
        if (methods.size() == 0)
            throw new Error("Method '" + methodName + "' was not found on type '" + cls.getName() + "' with " + args.length + " arguments");
        else if (methods.size() > 1) {
            // TODO: actually we should implement proper method shadowing here...
            var thisMethods = methods.stream().filter(x -> x.parentInterface == cls).toArray(Method[]::new);
            if (thisMethods.length == 1)
                return thisMethods[0];
            throw new Error("Multiple methods found with name '" + methodName + "' and " + args.length + " arguments on type '" + cls.getName() + "'");
        }
        return methods.get(0);
    }
    
    protected void resolveReturnType(IMethodCallExpression expr, GenericsResolver genericsResolver) {
        genericsResolver.collectFromMethodCall(expr);
        
        for (Integer i = 0; i < expr.getArgs().length; i++) {
            // actually doesn't have to resolve, but must check if generic type confirm the previous argument with the same generic type
            var paramType = genericsResolver.resolveType(expr.getMethod().getParameters()[i].getType(), false);
            if (paramType != null)
                expr.getArgs()[i].setExpectedType(paramType, false);
            expr.getArgs()[i] = this.main.runPluginsOn(expr.getArgs()[i]);
            genericsResolver.collectResolutionsFromActualType(paramType, expr.getArgs()[i].actualType);
        }
        
        if (expr.getMethod().returns == null) {
            this.errorMan.throw_("Method (" + expr.getMethod().parentInterface.getName() + "::" + expr.getMethod().getName() + ") return type was not specified or infered before the call.");
            return;
        }
        
        expr.setActualType(genericsResolver.resolveType(expr.getMethod().returns, true), true, expr instanceof InstanceMethodCallExpression && TypeHelper.isGeneric(((InstanceMethodCallExpression)expr).object.getType()));
    }
    
    protected Expression transformMethodCall(UnresolvedMethodCallExpression expr) {
        if (expr.object instanceof ClassReference || expr.object instanceof StaticThisReference) {
            var cls = expr.object instanceof ClassReference ? ((ClassReference)expr.object).decl : expr.object instanceof StaticThisReference ? ((StaticThisReference)expr.object).cls : null;
            var method = this.findMethod(cls, expr.methodName, true, expr.args);
            var result = new StaticMethodCallExpression(method, expr.typeArgs, expr.args, expr.object instanceof StaticThisReference);
            this.resolveReturnType(result, new GenericsResolver());
            return result;
        }
        else {
            var resolvedObject = expr.object.actualType != null ? expr.object : this.main.runPluginsOn(expr.object);
            var objectType = resolvedObject.getType();
            var intfType = objectType instanceof ClassType ? ((IInterface)((ClassType)objectType).decl) : objectType instanceof InterfaceType ? ((InterfaceType)objectType).decl : null;
            
            if (intfType != null) {
                var lambdaField = Arrays.stream(intfType.getFields()).filter(x -> Objects.equals(x.getName(), expr.methodName) && x.getType() instanceof LambdaType && ((LambdaType)x.getType()).parameters.length == expr.args.length).findFirst().orElse(null);
                if (lambdaField != null) {
                    var lambdaCall = new LambdaCallExpression(new InstanceFieldReference(expr.object, lambdaField), expr.args);
                    lambdaCall.setActualType((((LambdaType)lambdaField.getType())).returnType, false, false);
                    return lambdaCall;
                }
                
                var method = this.findMethod(intfType, expr.methodName, false, expr.args);
                var result = new InstanceMethodCallExpression(resolvedObject, method, expr.typeArgs, expr.args);
                this.resolveReturnType(result, GenericsResolver.fromObject(resolvedObject));
                return result;
            }
            else if (objectType instanceof AnyType) {
                expr.setActualType(AnyType.instance, false, false);
                return expr;
            }
            else { }
            return resolvedObject;
        }
    }
    
    public Boolean canTransform(Expression expr) {
        return expr instanceof UnresolvedMethodCallExpression && !(((UnresolvedMethodCallExpression)expr).actualType instanceof AnyType);
    }
    
    public Expression transform(Expression expr) {
        return this.transformMethodCall(((UnresolvedMethodCallExpression)expr));
    }
}