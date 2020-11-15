package OneLang.Generator.JavaPlugins.JsToJava;

import OneLang.Generator.IGeneratorPlugin.IGeneratorPlugin;
import OneLang.One.Ast.Expressions.InstanceMethodCallExpression;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.StaticMethodCallExpression;
import OneLang.One.Ast.Expressions.RegexLiteral;
import OneLang.One.Ast.Expressions.ElementAccessExpression;
import OneLang.One.Ast.Expressions.ArrayLiteral;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.InterfaceType;
import OneLang.One.Ast.AstTypes.LambdaType;
import OneLang.One.Ast.AstTypes.TypeHelper;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.References.InstanceFieldReference;
import OneLang.One.Ast.References.InstancePropertyReference;
import OneLang.One.Ast.References.VariableDeclarationReference;
import OneLang.One.Ast.References.VariableReference;
import OneLang.One.Ast.Interfaces.IExpression;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.Generator.JavaGenerator.JavaGenerator;

import OneLang.Generator.IGeneratorPlugin.IGeneratorPlugin;
import java.util.Set;
import OneLang.Generator.JavaGenerator.JavaGenerator;
import java.util.LinkedHashSet;
import OneLang.One.Ast.References.VariableReference;
import OneLang.One.Ast.Expressions.StaticMethodCallExpression;
import OneLang.One.Ast.Expressions.InstanceMethodCallExpression;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.Interfaces.IType;
import java.util.Arrays;
import OneStd.Objects;
import OneLang.One.Ast.Expressions.RegexLiteral;
import OneStd.JSON;
import java.util.List;
import java.util.ArrayList;
import OneStd.console;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.References.InstancePropertyReference;
import OneLang.One.Ast.References.InstanceFieldReference;
import OneLang.One.Ast.Interfaces.IExpression;
import OneLang.One.Ast.Statements.Statement;

public class JsToJava implements IGeneratorPlugin {
    public Set<String> unhandledMethods;
    public JavaGenerator main;
    
    public JsToJava(JavaGenerator main)
    {
        this.main = main;
        this.unhandledMethods = new LinkedHashSet<String>();
    }
    
    public Boolean isArray(Expression arrayExpr) {
        // TODO: InstanceMethodCallExpression is a hack, we should introduce real stream handling
        return arrayExpr instanceof VariableReference && !((VariableReference)arrayExpr).getVariable().getMutability().mutated || arrayExpr instanceof StaticMethodCallExpression || arrayExpr instanceof InstanceMethodCallExpression;
    }
    
    public String arrayStream(Expression arrayExpr) {
        var isArray = this.isArray(arrayExpr);
        var objR = this.main.expr(arrayExpr);
        if (isArray)
            this.main.imports.add("java.util.Arrays");
        return isArray ? "Arrays.stream(" + objR + ")" : objR + ".stream()";
    }
    
    public String toArray(IType arrayType, Integer typeArgIdx) {
        var type = (((ClassType)arrayType)).getTypeArguments()[typeArgIdx];
        return "toArray(" + this.main.type(type, true, false) + "[]::new)";
    }
    
    public String convertMethod(Class cls, Expression obj, Method method, Expression[] args, IType returnType) {
        var objR = obj == null ? null : this.main.expr(obj);
        var argsR = Arrays.stream(args).map(x -> this.main.expr(x)).toArray(String[]::new);
        if (Objects.equals(cls.getName(), "TsArray")) {
            if (Objects.equals(method.name, "includes"))
                return this.arrayStream(obj) + ".anyMatch(" + argsR[0] + "::equals)";
            else if (Objects.equals(method.name, "set")) {
                if (this.isArray(obj))
                    return objR + "[" + argsR[0] + "] = " + argsR[1];
                else
                    return objR + ".set(" + argsR[0] + ", " + argsR[1] + ")";
            }
            else if (Objects.equals(method.name, "get"))
                return this.isArray(obj) ? objR + "[" + argsR[0] + "]" : objR + ".get(" + argsR[0] + ")";
            else if (Objects.equals(method.name, "join")) {
                this.main.imports.add("java.util.stream.Collectors");
                return this.arrayStream(obj) + ".collect(Collectors.joining(" + argsR[0] + "))";
            }
            else if (Objects.equals(method.name, "map"))
                //if (returnType.repr() !== "C:TsArray<C:TsString>") debugger;
                return this.arrayStream(obj) + ".map(" + argsR[0] + ")." + this.toArray(returnType, 0);
            else if (Objects.equals(method.name, "push"))
                return objR + ".add(" + argsR[0] + ")";
            else if (Objects.equals(method.name, "pop"))
                return objR + ".remove(" + objR + ".size() - 1)";
            else if (Objects.equals(method.name, "filter"))
                return this.arrayStream(obj) + ".filter(" + argsR[0] + ")." + this.toArray(returnType, 0);
            else if (Objects.equals(method.name, "every")) {
                this.main.imports.add("OneStd.StdArrayHelper");
                return "StdArrayHelper.allMatch(" + objR + ", " + argsR[0] + ")";
            }
            else if (Objects.equals(method.name, "some"))
                return this.arrayStream(obj) + ".anyMatch(" + argsR[0] + ")";
            else if (Objects.equals(method.name, "concat")) {
                this.main.imports.add("java.util.stream.Stream");
                return "Stream.of(" + objR + ", " + argsR[0] + ").flatMap(Stream::of)." + this.toArray(obj.getType(), 0);
            }
            else if (Objects.equals(method.name, "shift"))
                return objR + ".remove(0)";
            else if (Objects.equals(method.name, "find"))
                return this.arrayStream(obj) + ".filter(" + argsR[0] + ").findFirst().orElse(null)";
            else if (Objects.equals(method.name, "sort")) {
                this.main.imports.add("java.util.Collections");
                return "Collections.sort(" + objR + ")";
            }
        }
        else if (Objects.equals(cls.getName(), "TsString")) {
            if (Objects.equals(method.name, "replace")) {
                if (args[0] instanceof RegexLiteral) {
                    this.main.imports.add("java.util.regex.Pattern");
                    return objR + ".replaceAll(" + JSON.stringify((((RegexLiteral)args[0])).pattern) + ", " + argsR[1] + ")";
                }
                
                return argsR[0] + ".replace(" + objR + ", " + argsR[1] + ")";
            }
            else if (Objects.equals(method.name, "charCodeAt"))
                return "(int)" + objR + ".charAt(" + argsR[0] + ")";
            else if (Objects.equals(method.name, "includes"))
                return objR + ".contains(" + argsR[0] + ")";
            else if (Objects.equals(method.name, "get"))
                return objR + ".substring(" + argsR[0] + ", " + argsR[0] + " + 1)";
            else if (Objects.equals(method.name, "substr"))
                return argsR.length == 1 ? objR + ".substring(" + argsR[0] + ")" : objR + ".substring(" + argsR[0] + ", " + argsR[0] + " + " + argsR[1] + ")";
            else if (Objects.equals(method.name, "substring"))
                return objR + ".substring(" + argsR[0] + ", " + argsR[1] + ")";
            
            if (Objects.equals(method.name, "split") && args[0] instanceof RegexLiteral) {
                var pattern = (((RegexLiteral)args[0])).pattern;
                return objR + ".split(" + JSON.stringify(pattern) + ", -1)";
            }
        }
        else if (Objects.equals(cls.getName(), "TsMap") || Objects.equals(cls.getName(), "Map")) {
            if (Objects.equals(method.name, "set"))
                return objR + ".put(" + argsR[0] + ", " + argsR[1] + ")";
            else if (Objects.equals(method.name, "get"))
                return objR + ".get(" + argsR[0] + ")";
            else if (Objects.equals(method.name, "hasKey") || Objects.equals(method.name, "has"))
                return objR + ".containsKey(" + argsR[0] + ")";
            else if (Objects.equals(method.name, "delete"))
                return objR + ".remove(" + argsR[0] + ")";
            else if (Objects.equals(method.name, "values"))
                return objR + ".values()." + this.toArray(obj.getType(), 1);
        }
        else if (Objects.equals(cls.getName(), "Object")) {
            if (Objects.equals(method.name, "keys"))
                return argsR[0] + ".keySet().toArray(String[]::new)";
            else if (Objects.equals(method.name, "values"))
                return argsR[0] + ".values()." + this.toArray(args[0].getType(), 0);
        }
        else if (Objects.equals(cls.getName(), "Set")) {
            if (Objects.equals(method.name, "values"))
                return objR + "." + this.toArray(obj.getType(), 0);
            else if (Objects.equals(method.name, "has"))
                return objR + ".contains(" + argsR[0] + ")";
            else if (Objects.equals(method.name, "add"))
                return objR + ".add(" + argsR[0] + ")";
        }
        else if (Objects.equals(cls.getName(), "ArrayHelper")) { }
        else if (Objects.equals(cls.getName(), "Array")) {
            if (Objects.equals(method.name, "from"))
                return argsR[0];
        }
        else if (Objects.equals(cls.getName(), "Promise")) {
            if (Objects.equals(method.name, "resolve"))
                return argsR[0];
        }
        else if (Objects.equals(cls.getName(), "RegExpExecArray")) {
            if (Objects.equals(method.name, "get"))
                return objR + "[" + argsR[0] + "]";
        }
        else if (new ArrayList<>(List.of("JSON", "console", "RegExp")).stream().anyMatch(cls.getName()::equals)) {
            this.main.imports.add("OneStd." + cls.getName());
            return null;
        }
        else
            return null;
        
        var methodName = cls.getName() + "." + method.name;
        if (!this.unhandledMethods.contains(methodName)) {
            console.error("[JsToJava] Method was not handled: " + cls.getName() + "." + method.name);
            this.unhandledMethods.add(methodName);
        }
        //debugger;
        return null;
    }
    
    public String expr(IExpression expr) {
        if (expr instanceof InstanceMethodCallExpression && ((InstanceMethodCallExpression)expr).object.actualType instanceof ClassType)
            return this.convertMethod(((ClassType)((InstanceMethodCallExpression)expr).object.actualType).decl, ((InstanceMethodCallExpression)expr).object, ((InstanceMethodCallExpression)expr).getMethod(), ((InstanceMethodCallExpression)expr).getArgs(), ((InstanceMethodCallExpression)expr).actualType);
        else if (expr instanceof InstancePropertyReference && ((InstancePropertyReference)expr).object.actualType instanceof ClassType) {
            if (Objects.equals(((InstancePropertyReference)expr).property.parentClass.getName(), "TsString") && Objects.equals(((InstancePropertyReference)expr).property.getName(), "length"))
                return this.main.expr(((InstancePropertyReference)expr).object) + ".length()";
            if (Objects.equals(((InstancePropertyReference)expr).property.parentClass.getName(), "TsArray") && Objects.equals(((InstancePropertyReference)expr).property.getName(), "length"))
                return this.main.expr(((InstancePropertyReference)expr).object) + "." + (this.isArray(((InstancePropertyReference)expr).object) ? "length" : "size()");
        }
        else if (expr instanceof InstanceFieldReference && ((InstanceFieldReference)expr).object.actualType instanceof ClassType) {
            if (Objects.equals(((InstanceFieldReference)expr).field.parentInterface.getName(), "RegExpExecArray") && Objects.equals(((InstanceFieldReference)expr).field.getName(), "length"))
                return this.main.expr(((InstanceFieldReference)expr).object) + ".length";
            if (Objects.equals(((InstanceFieldReference)expr).field.parentInterface.getName(), "Map") && Objects.equals(((InstanceFieldReference)expr).field.getName(), "size"))
                return this.main.expr(((InstanceFieldReference)expr).object) + ".size()";
        }
        else if (expr instanceof StaticMethodCallExpression && ((StaticMethodCallExpression)expr).getMethod().parentInterface instanceof Class)
            return this.convertMethod(((Class)((StaticMethodCallExpression)expr).getMethod().parentInterface), null, ((StaticMethodCallExpression)expr).getMethod(), ((StaticMethodCallExpression)expr).getArgs(), ((StaticMethodCallExpression)expr).actualType);
        return null;
    }
    
    public String stmt(Statement stmt) {
        return null;
    }
}