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
import io.onelang.std.core.Objects;
import java.util.Arrays;
import OneLang.One.Ast.Expressions.RegexLiteral;
import java.util.regex.Pattern;
import io.onelang.std.json.JSON;
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
            if (Objects.equals(method.getName(), "includes"))
                return "in_array(" + argsR[0] + ", " + objR + ")";
            else if (Objects.equals(method.getName(), "set"))
                return objR + "[" + argsR[0] + "] = " + argsR[1];
            else if (Objects.equals(method.getName(), "get"))
                return objR + "[" + argsR[0] + "]";
            else if (Objects.equals(method.getName(), "join"))
                return "implode(" + argsR[0] + ", " + objR + ")";
            else if (Objects.equals(method.getName(), "map"))
                return "array_map(" + argsR[0] + ", " + objR + ")";
            else if (Objects.equals(method.getName(), "push"))
                return objR + "[] = " + argsR[0];
            else if (Objects.equals(method.getName(), "pop"))
                return "array_pop(" + objR + ")";
            else if (Objects.equals(method.getName(), "filter"))
                return "array_values(array_filter(" + objR + ", " + argsR[0] + "))";
            else if (Objects.equals(method.getName(), "every"))
                return "\\OneLang\\Core\\ArrayHelper::every(" + objR + ", " + argsR[0] + ")";
            else if (Objects.equals(method.getName(), "some"))
                return "\\OneLang\\Core\\ArrayHelper::some(" + objR + ", " + argsR[0] + ")";
            else if (Objects.equals(method.getName(), "concat"))
                return "array_merge(" + objR + ", " + argsR[0] + ")";
            else if (Objects.equals(method.getName(), "shift"))
                return "array_shift(" + objR + ")";
            else if (Objects.equals(method.getName(), "find"))
                return "\\OneLang\\Core\\ArrayHelper::find(" + objR + ", " + argsR[0] + ")";
            else if (Objects.equals(method.getName(), "sort"))
                return "sort(" + objR + ")";
        }
        else if (Objects.equals(cls.getName(), "TsString")) {
            var objR = this.main.expr(obj);
            var argsR = Arrays.stream(args).map(x -> this.main.expr(x)).toArray(String[]::new);
            if (Objects.equals(method.getName(), "split")) {
                if (args[0] instanceof RegexLiteral) {
                    var pattern = (((RegexLiteral)args[0])).pattern;
                    var modPattern = "/" + pattern.replaceAll("/", "\\/") + "/";
                    return "preg_split(" + JSON.stringify(modPattern) + ", " + objR + ")";
                }
                
                return "explode(" + argsR[0] + ", " + objR + ")";
            }
            else if (Objects.equals(method.getName(), "replace")) {
                if (args[0] instanceof RegexLiteral)
                    return "preg_replace(" + JSON.stringify("/" + (((RegexLiteral)args[0])).pattern + "/") + ", " + argsR[1] + ", " + objR + ")";
                
                return argsR[0] + ".replace(" + objR + ", " + argsR[1] + ")";
            }
            else if (Objects.equals(method.getName(), "includes"))
                return "strpos(" + objR + ", " + argsR[0] + ") !== false";
            else if (Objects.equals(method.getName(), "startsWith")) {
                if (argsR.length > 1)
                    return "substr_compare(" + objR + ", " + argsR[0] + ", " + argsR[1] + ", strlen(" + argsR[0] + ")) === 0";
                else
                    return "substr_compare(" + objR + ", " + argsR[0] + ", 0, strlen(" + argsR[0] + ")) === 0";
            }
            else if (Objects.equals(method.getName(), "endsWith")) {
                if (argsR.length > 1)
                    return "substr_compare(" + objR + ", " + argsR[0] + ", " + argsR[1] + " - strlen(" + argsR[0] + "), strlen(" + argsR[0] + ")) === 0";
                else
                    return "substr_compare(" + objR + ", " + argsR[0] + ", strlen(" + objR + ") - strlen(" + argsR[0] + "), strlen(" + argsR[0] + ")) === 0";
            }
            else if (Objects.equals(method.getName(), "indexOf"))
                return "strpos(" + objR + ", " + argsR[0] + ", " + argsR[1] + ")";
            else if (Objects.equals(method.getName(), "lastIndexOf"))
                return "strrpos(" + objR + ", " + argsR[0] + ", " + argsR[1] + " - strlen(" + objR + "))";
            else if (Objects.equals(method.getName(), "substr")) {
                if (argsR.length > 1)
                    return "substr(" + objR + ", " + argsR[0] + ", " + argsR[1] + ")";
                else
                    return "substr(" + objR + ", " + argsR[0] + ")";
            }
            else if (Objects.equals(method.getName(), "substring"))
                return "substr(" + objR + ", " + argsR[0] + ", " + argsR[1] + " - (" + argsR[0] + "))";
            else if (Objects.equals(method.getName(), "repeat"))
                return "str_repeat(" + objR + ", " + argsR[0] + ")";
            else if (Objects.equals(method.getName(), "toUpperCase"))
                return "strtoupper(" + objR + ")";
            else if (Objects.equals(method.getName(), "toLowerCase"))
                return "strtolower(" + objR + ")";
            else if (Objects.equals(method.getName(), "get"))
                return objR + "[" + argsR[0] + "]";
            else if (Objects.equals(method.getName(), "charCodeAt"))
                return "ord(" + objR + "[" + argsR[0] + "])";
        }
        else if (Objects.equals(cls.getName(), "TsMap")) {
            var objR = this.main.expr(obj);
            var argsR = Arrays.stream(args).map(x -> this.main.expr(x)).toArray(String[]::new);
            if (Objects.equals(method.getName(), "set"))
                return objR + "[" + argsR[0] + "] = " + argsR[1];
            else if (Objects.equals(method.getName(), "get"))
                return "@" + objR + "[" + argsR[0] + "] ?? null";
            else if (Objects.equals(method.getName(), "hasKey"))
                return "array_key_exists(" + argsR[0] + ", " + objR + ")";
        }
        else if (Objects.equals(cls.getName(), "Object")) {
            var argsR = Arrays.stream(args).map(x -> this.main.expr(x)).toArray(String[]::new);
            if (Objects.equals(method.getName(), "keys"))
                return "array_keys(" + argsR[0] + ")";
            else if (Objects.equals(method.getName(), "values"))
                return "array_values(" + argsR[0] + ")";
        }
        else if (Objects.equals(cls.getName(), "ArrayHelper")) {
            var argsR = Arrays.stream(args).map(x -> this.main.expr(x)).toArray(String[]::new);
            if (Objects.equals(method.getName(), "sortBy"))
                return "\\OneLang\\Core\\ArrayHelper::sortBy(" + argsR[0] + ", " + argsR[1] + ")";
            else if (Objects.equals(method.getName(), "removeLastN"))
                return "array_splice(" + argsR[0] + ", -" + argsR[1] + ")";
        }
        else if (Objects.equals(cls.getName(), "Math")) {
            var argsR = Arrays.stream(args).map(x -> this.main.expr(x)).toArray(String[]::new);
            if (Objects.equals(method.getName(), "floor"))
                return "floor(" + argsR[0] + ")";
        }
        else if (Objects.equals(cls.getName(), "JSON")) {
            var argsR = Arrays.stream(args).map(x -> this.main.expr(x)).toArray(String[]::new);
            if (Objects.equals(method.getName(), "stringify"))
                return "json_encode(" + argsR[0] + ", JSON_UNESCAPED_SLASHES)";
        }
        else if (Objects.equals(cls.getName(), "RegExpExecArray")) {
            var objR = this.main.expr(obj);
            var argsR = Arrays.stream(args).map(x -> this.main.expr(x)).toArray(String[]::new);
            return objR + "[" + argsR[0] + "]";
        }
        else
            return null;
        
        var methodName = cls.getName() + "." + method.getName();
        if (!this.unhandledMethods.contains(methodName)) {
            System.err.println("[JsToPython] Method was not handled: " + cls.getName() + "." + method.getName());
            this.unhandledMethods.add(methodName);
        }
        //debugger;
        return null;
    }
    
    public String expr(IExpression expr) {
        if (expr instanceof InstanceMethodCallExpression && ((InstanceMethodCallExpression)expr).object.actualType instanceof ClassType)
            return this.convertMethod(((ClassType)((InstanceMethodCallExpression)expr).object.actualType).decl, ((InstanceMethodCallExpression)expr).object, ((InstanceMethodCallExpression)expr).getMethod(), ((InstanceMethodCallExpression)expr).getArgs());
        else if (expr instanceof InstancePropertyReference && ((InstancePropertyReference)expr).getObject().actualType instanceof ClassType) {
            if (Objects.equals(((InstancePropertyReference)expr).property.parentClass.getName(), "TsString") && Objects.equals(((InstancePropertyReference)expr).property.getName(), "length"))
                return "strlen(" + this.main.expr(((InstancePropertyReference)expr).getObject()) + ")";
            if (Objects.equals(((InstancePropertyReference)expr).property.parentClass.getName(), "TsArray") && Objects.equals(((InstancePropertyReference)expr).property.getName(), "length"))
                return "count(" + this.main.expr(((InstancePropertyReference)expr).getObject()) + ")";
        }
        else if (expr instanceof InstanceFieldReference && ((InstanceFieldReference)expr).getObject().actualType instanceof ClassType) {
            if (Objects.equals(((InstanceFieldReference)expr).field.parentInterface.getName(), "RegExpExecArray") && Objects.equals(((InstanceFieldReference)expr).field.getName(), "length"))
                return "count(" + this.main.expr(((InstanceFieldReference)expr).getObject()) + ")";
        }
        else if (expr instanceof StaticMethodCallExpression && ((StaticMethodCallExpression)expr).getMethod().parentInterface instanceof Class)
            return this.convertMethod(((Class)((StaticMethodCallExpression)expr).getMethod().parentInterface), null, ((StaticMethodCallExpression)expr).getMethod(), ((StaticMethodCallExpression)expr).getArgs());
        return null;
    }
    
    public String stmt(Statement stmt) {
        return null;
    }
}