package OneLang.Generator.PhpPlugins.JsToPhp;

import OneLang.Generator.IGeneratorPlugin.IGeneratorPlugin;
import OneLang.One.Ast.Expressions.InstanceMethodCallExpression;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.StaticMethodCallExpression;
import OneLang.One.Ast.Expressions.RegexLiteral;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.InterfaceType;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.References.InstanceFieldReference;
import OneLang.One.Ast.References.InstancePropertyReference;
import OneLang.One.Ast.Interfaces.IExpression;
import OneLang.Generator.PhpGenerator.PhpGenerator;

import OneLang.Generator.IGeneratorPlugin.IGeneratorPlugin;
import java.util.Set;
import OneLang.Generator.PhpGenerator.PhpGenerator;
import java.util.LinkedHashSet;
import OneStd.Objects;
import java.util.Arrays;
import OneLang.One.Ast.Expressions.RegexLiteral;
import OneStd.RegExp;
import java.util.regex.Pattern;
import OneStd.JSON;
import OneStd.console;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Expressions.InstanceMethodCallExpression;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.References.InstancePropertyReference;
import OneLang.One.Ast.References.InstanceFieldReference;
import OneLang.One.Ast.Expressions.StaticMethodCallExpression;
import OneLang.One.Ast.Interfaces.IExpression;
import OneLang.One.Ast.Statements.Statement;

public class JsToPhp implements IGeneratorPlugin {
    public Set<String> unhandledMethods;
    public PhpGenerator main;
    
    public JsToPhp(PhpGenerator main)
    {
        this.main = main;
        this.unhandledMethods = new LinkedHashSet<String>();
    }
    
    public String convertMethod(Class cls, Expression obj, Method method, Expression[] args) {
        if (Objects.equals(cls.getName(), "TsArray")) {
            var objR = this.main.expr(obj);
            var argsR = Arrays.stream(args).map(x -> this.main.expr(x)).toArray(String[]::new);
            if (Objects.equals(method.name, "includes"))
                return "in_array(" + argsR[0] + ", " + objR + ")";
            else if (Objects.equals(method.name, "set"))
                return objR + "[" + argsR[0] + "] = " + argsR[1];
            else if (Objects.equals(method.name, "get"))
                return objR + "[" + argsR[0] + "]";
            else if (Objects.equals(method.name, "join"))
                return "implode(" + argsR[0] + ", " + objR + ")";
            else if (Objects.equals(method.name, "map"))
                return "array_map(" + argsR[0] + ", " + objR + ")";
            else if (Objects.equals(method.name, "push"))
                return objR + "[] = " + argsR[0];
            else if (Objects.equals(method.name, "pop"))
                return "array_pop(" + objR + ")";
            else if (Objects.equals(method.name, "filter"))
                return "array_values(array_filter(" + objR + ", " + argsR[0] + "))";
            else if (Objects.equals(method.name, "every"))
                return "\\OneLang\\ArrayHelper::every(" + objR + ", " + argsR[0] + ")";
            else if (Objects.equals(method.name, "some"))
                return "\\OneLang\\ArrayHelper::some(" + objR + ", " + argsR[0] + ")";
            else if (Objects.equals(method.name, "concat"))
                return "array_merge(" + objR + ", " + argsR[0] + ")";
            else if (Objects.equals(method.name, "shift"))
                return "array_shift(" + objR + ")";
            else if (Objects.equals(method.name, "find"))
                return "\\OneLang\\ArrayHelper::find(" + objR + ", " + argsR[0] + ")";
            else if (Objects.equals(method.name, "sort"))
                return "sort(" + objR + ")";
        }
        else if (Objects.equals(cls.getName(), "TsString")) {
            var objR = this.main.expr(obj);
            var argsR = Arrays.stream(args).map(x -> this.main.expr(x)).toArray(String[]::new);
            if (Objects.equals(method.name, "split")) {
                if (args[0] instanceof RegexLiteral) {
                    var pattern = (((RegexLiteral)args[0])).pattern;
                    var modPattern = "/" + pattern.replaceAll("/", "\\/") + "/";
                    return "preg_split(" + JSON.stringify(modPattern) + ", " + objR + ")";
                }
                
                return "explode(" + argsR[0] + ", " + objR + ")";
            }
            else if (Objects.equals(method.name, "replace")) {
                if (args[0] instanceof RegexLiteral)
                    return "preg_replace(" + JSON.stringify("/" + (((RegexLiteral)args[0])).pattern + "/") + ", " + argsR[1] + ", " + objR + ")";
                
                return argsR[0] + ".replace(" + objR + ", " + argsR[1] + ")";
            }
            else if (Objects.equals(method.name, "includes"))
                return "strpos(" + objR + ", " + argsR[0] + ") !== false";
            else if (Objects.equals(method.name, "startsWith")) {
                if (argsR.length > 1)
                    return "substr_compare(" + objR + ", " + argsR[0] + ", " + argsR[1] + ", strlen(" + argsR[0] + ")) === 0";
                else
                    return "substr_compare(" + objR + ", " + argsR[0] + ", 0, strlen(" + argsR[0] + ")) === 0";
            }
            else if (Objects.equals(method.name, "endsWith")) {
                if (argsR.length > 1)
                    return "substr_compare(" + objR + ", " + argsR[0] + ", " + argsR[1] + " - strlen(" + argsR[0] + "), strlen(" + argsR[0] + ")) === 0";
                else
                    return "substr_compare(" + objR + ", " + argsR[0] + ", strlen(" + objR + ") - strlen(" + argsR[0] + "), strlen(" + argsR[0] + ")) === 0";
            }
            else if (Objects.equals(method.name, "indexOf"))
                return "strpos(" + objR + ", " + argsR[0] + ", " + argsR[1] + ")";
            else if (Objects.equals(method.name, "lastIndexOf"))
                return "strrpos(" + objR + ", " + argsR[0] + ", " + argsR[1] + " - strlen(" + objR + "))";
            else if (Objects.equals(method.name, "substr")) {
                if (argsR.length > 1)
                    return "substr(" + objR + ", " + argsR[0] + ", " + argsR[1] + ")";
                else
                    return "substr(" + objR + ", " + argsR[0] + ")";
            }
            else if (Objects.equals(method.name, "substring"))
                return "substr(" + objR + ", " + argsR[0] + ", " + argsR[1] + " - (" + argsR[0] + "))";
            else if (Objects.equals(method.name, "repeat"))
                return "str_repeat(" + objR + ", " + argsR[0] + ")";
            else if (Objects.equals(method.name, "toUpperCase"))
                return "strtoupper(" + objR + ")";
            else if (Objects.equals(method.name, "toLowerCase"))
                return "strtolower(" + objR + ")";
            else if (Objects.equals(method.name, "get"))
                return objR + "[" + argsR[0] + "]";
            else if (Objects.equals(method.name, "charCodeAt"))
                return "ord(" + objR + "[" + argsR[0] + "])";
        }
        else if (Objects.equals(cls.getName(), "TsMap")) {
            var objR = this.main.expr(obj);
            var argsR = Arrays.stream(args).map(x -> this.main.expr(x)).toArray(String[]::new);
            if (Objects.equals(method.name, "set"))
                return objR + "[" + argsR[0] + "] = " + argsR[1];
            else if (Objects.equals(method.name, "get"))
                return "@" + objR + "[" + argsR[0] + "] ?? null";
            else if (Objects.equals(method.name, "hasKey"))
                return "array_key_exists(" + argsR[0] + ", " + objR + ")";
        }
        else if (Objects.equals(cls.getName(), "Object")) {
            var argsR = Arrays.stream(args).map(x -> this.main.expr(x)).toArray(String[]::new);
            if (Objects.equals(method.name, "keys"))
                return "array_keys(" + argsR[0] + ")";
            else if (Objects.equals(method.name, "values"))
                return "array_values(" + argsR[0] + ")";
        }
        else if (Objects.equals(cls.getName(), "ArrayHelper")) {
            var argsR = Arrays.stream(args).map(x -> this.main.expr(x)).toArray(String[]::new);
            if (Objects.equals(method.name, "sortBy"))
                return "\\OneLang\\ArrayHelper::sortBy(" + argsR[0] + ", " + argsR[1] + ")";
            else if (Objects.equals(method.name, "removeLastN"))
                return "array_splice(" + argsR[0] + ", -" + argsR[1] + ")";
        }
        else if (Objects.equals(cls.getName(), "Math")) {
            var argsR = Arrays.stream(args).map(x -> this.main.expr(x)).toArray(String[]::new);
            if (Objects.equals(method.name, "floor"))
                return "floor(" + argsR[0] + ")";
        }
        else if (Objects.equals(cls.getName(), "JSON")) {
            var argsR = Arrays.stream(args).map(x -> this.main.expr(x)).toArray(String[]::new);
            if (Objects.equals(method.name, "stringify"))
                return "json_encode(" + argsR[0] + ", JSON_UNESCAPED_SLASHES)";
        }
        else if (Objects.equals(cls.getName(), "RegExpExecArray")) {
            var objR = this.main.expr(obj);
            var argsR = Arrays.stream(args).map(x -> this.main.expr(x)).toArray(String[]::new);
            return objR + "[" + argsR[0] + "]";
        }
        else
            return null;
        
        var methodName = cls.getName() + "." + method.name;
        if (!this.unhandledMethods.contains(methodName)) {
            console.error("[JsToPython] Method was not handled: " + cls.getName() + "." + method.name);
            this.unhandledMethods.add(methodName);
        }
        //debugger;
        return null;
    }
    
    public String expr(IExpression expr) {
        if (expr instanceof InstanceMethodCallExpression && ((InstanceMethodCallExpression)expr).object.actualType instanceof ClassType)
            return this.convertMethod(((ClassType)((InstanceMethodCallExpression)expr).object.actualType).decl, ((InstanceMethodCallExpression)expr).object, ((InstanceMethodCallExpression)expr).getMethod(), ((InstanceMethodCallExpression)expr).getArgs());
        else if (expr instanceof InstancePropertyReference && ((InstancePropertyReference)expr).object.actualType instanceof ClassType) {
            if (Objects.equals(((InstancePropertyReference)expr).property.parentClass.getName(), "TsString") && Objects.equals(((InstancePropertyReference)expr).property.getName(), "length"))
                return "strlen(" + this.main.expr(((InstancePropertyReference)expr).object) + ")";
            if (Objects.equals(((InstancePropertyReference)expr).property.parentClass.getName(), "TsArray") && Objects.equals(((InstancePropertyReference)expr).property.getName(), "length"))
                return "count(" + this.main.expr(((InstancePropertyReference)expr).object) + ")";
        }
        else if (expr instanceof InstanceFieldReference && ((InstanceFieldReference)expr).object.actualType instanceof ClassType) {
            if (Objects.equals(((InstanceFieldReference)expr).field.parentInterface.getName(), "RegExpExecArray") && Objects.equals(((InstanceFieldReference)expr).field.getName(), "length"))
                return "count(" + this.main.expr(((InstanceFieldReference)expr).object) + ")";
        }
        else if (expr instanceof StaticMethodCallExpression && ((StaticMethodCallExpression)expr).getMethod().parentInterface instanceof Class)
            return this.convertMethod(((Class)((StaticMethodCallExpression)expr).getMethod().parentInterface), null, ((StaticMethodCallExpression)expr).getMethod(), ((StaticMethodCallExpression)expr).getArgs());
        return null;
    }
    
    public String stmt(Statement stmt) {
        return null;
    }
}