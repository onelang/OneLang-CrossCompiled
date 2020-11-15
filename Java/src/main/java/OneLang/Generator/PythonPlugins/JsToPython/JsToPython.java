package OneLang.Generator.PythonPlugins.JsToPython;

import OneLang.Generator.IGeneratorPlugin.IGeneratorPlugin;
import OneLang.One.Ast.Expressions.InstanceMethodCallExpression;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.StaticMethodCallExpression;
import OneLang.One.Ast.Expressions.RegexLiteral;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.InterfaceType;
import OneLang.Generator.PythonGenerator.PythonGenerator;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.References.InstanceFieldReference;
import OneLang.One.Ast.References.InstancePropertyReference;
import OneLang.One.Ast.Interfaces.IExpression;

import OneLang.Generator.IGeneratorPlugin.IGeneratorPlugin;
import java.util.Set;
import OneLang.Generator.PythonGenerator.PythonGenerator;
import java.util.LinkedHashSet;
import OneStd.Objects;
import java.util.Arrays;
import OneLang.One.Ast.Expressions.RegexLiteral;
import OneStd.JSON;
import java.util.stream.Collectors;
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

public class JsToPython implements IGeneratorPlugin {
    public Set<String> unhandledMethods;
    public PythonGenerator main;
    
    public JsToPython(PythonGenerator main)
    {
        this.main = main;
        this.unhandledMethods = new LinkedHashSet<String>();
    }
    
    public String convertMethod(Class cls, Expression obj, Method method, Expression[] args) {
        if (Objects.equals(cls.getName(), "TsArray")) {
            var objR = this.main.expr(obj);
            var argsR = Arrays.stream(args).map(x -> this.main.expr(x)).toArray(String[]::new);
            if (Objects.equals(method.name, "includes"))
                return argsR[0] + " in " + objR;
            else if (Objects.equals(method.name, "set"))
                return objR + "[" + argsR[0] + "] = " + argsR[1];
            else if (Objects.equals(method.name, "get"))
                return objR + "[" + argsR[0] + "]";
            else if (Objects.equals(method.name, "join"))
                return argsR[0] + ".join(" + objR + ")";
            else if (Objects.equals(method.name, "map"))
                return "list(map(" + argsR[0] + ", " + objR + "))";
            else if (Objects.equals(method.name, "push"))
                return objR + ".append(" + argsR[0] + ")";
            else if (Objects.equals(method.name, "pop"))
                return objR + ".pop()";
            else if (Objects.equals(method.name, "filter"))
                return "list(filter(" + argsR[0] + ", " + objR + "))";
            else if (Objects.equals(method.name, "every"))
                return "ArrayHelper.every(" + argsR[0] + ", " + objR + ")";
            else if (Objects.equals(method.name, "some"))
                return "ArrayHelper.some(" + argsR[0] + ", " + objR + ")";
            else if (Objects.equals(method.name, "concat"))
                return objR + " + " + argsR[0];
            else if (Objects.equals(method.name, "shift"))
                return objR + ".pop(0)";
            else if (Objects.equals(method.name, "find"))
                return "next(filter(" + argsR[0] + ", " + objR + "), None)";
        }
        else if (Objects.equals(cls.getName(), "TsString")) {
            var objR = this.main.expr(obj);
            var argsR = Arrays.stream(args).map(x -> this.main.expr(x)).toArray(String[]::new);
            if (Objects.equals(method.name, "split")) {
                if (args[0] instanceof RegexLiteral) {
                    var pattern = (((RegexLiteral)args[0])).pattern;
                    if (!pattern.startsWith("^")) {
                        //return `${objR}.split(${JSON.stringify(pattern)})`;
                        this.main.imports.add("import re");
                        return "re.split(" + JSON.stringify(pattern) + ", " + objR + ")";
                    }
                }
                
                return argsR[0] + ".split(" + objR + ")";
            }
            else if (Objects.equals(method.name, "replace")) {
                if (args[0] instanceof RegexLiteral) {
                    this.main.imports.add("import re");
                    return "re.sub(" + JSON.stringify((((RegexLiteral)args[0])).pattern) + ", " + argsR[1] + ", " + objR + ")";
                }
                
                return argsR[0] + ".replace(" + objR + ", " + argsR[1] + ")";
            }
            else if (Objects.equals(method.name, "includes"))
                return argsR[0] + " in " + objR;
            else if (Objects.equals(method.name, "startsWith"))
                return objR + ".startswith(" + Arrays.stream(argsR).collect(Collectors.joining(", ")) + ")";
            else if (Objects.equals(method.name, "indexOf"))
                return objR + ".find(" + argsR[0] + ", " + argsR[1] + ")";
            else if (Objects.equals(method.name, "lastIndexOf"))
                return objR + ".rfind(" + argsR[0] + ", 0, " + argsR[1] + ")";
            else if (Objects.equals(method.name, "substr"))
                return argsR.length == 1 ? objR + "[" + argsR[0] + ":]" : objR + "[" + argsR[0] + ":" + argsR[0] + " + " + argsR[1] + "]";
            else if (Objects.equals(method.name, "substring"))
                return objR + "[" + argsR[0] + ":" + argsR[1] + "]";
            else if (Objects.equals(method.name, "repeat"))
                return objR + " * (" + argsR[0] + ")";
            else if (Objects.equals(method.name, "toUpperCase"))
                return objR + ".upper()";
            else if (Objects.equals(method.name, "toLowerCase"))
                return objR + ".lower()";
            else if (Objects.equals(method.name, "endsWith"))
                return objR + ".endswith(" + argsR[0] + ")";
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
                return objR + ".get(" + argsR[0] + ")";
            else if (Objects.equals(method.name, "hasKey"))
                return argsR[0] + " in " + objR;
        }
        else if (Objects.equals(cls.getName(), "Object")) {
            var argsR = Arrays.stream(args).map(x -> this.main.expr(x)).toArray(String[]::new);
            if (Objects.equals(method.name, "keys"))
                return argsR[0] + ".keys()";
            else if (Objects.equals(method.name, "values"))
                return argsR[0] + ".values()";
        }
        else if (Objects.equals(cls.getName(), "Set")) {
            var objR = this.main.expr(obj);
            var argsR = Arrays.stream(args).map(x -> this.main.expr(x)).toArray(String[]::new);
            if (Objects.equals(method.name, "values"))
                return objR + ".keys()";
            else if (Objects.equals(method.name, "has"))
                return argsR[0] + " in " + objR;
            else if (Objects.equals(method.name, "add"))
                return objR + "[" + argsR[0] + "] = None";
        }
        else if (Objects.equals(cls.getName(), "ArrayHelper")) {
            var argsR = Arrays.stream(args).map(x -> this.main.expr(x)).toArray(String[]::new);
            if (Objects.equals(method.name, "sortBy"))
                return "sorted(" + argsR[0] + ", key=" + argsR[1] + ")";
            else if (Objects.equals(method.name, "removeLastN"))
                return "del " + argsR[0] + "[-" + argsR[1] + ":]";
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
                return "len(" + this.main.expr(((InstancePropertyReference)expr).object) + ")";
            if (Objects.equals(((InstancePropertyReference)expr).property.parentClass.getName(), "TsArray") && Objects.equals(((InstancePropertyReference)expr).property.getName(), "length"))
                return "len(" + this.main.expr(((InstancePropertyReference)expr).object) + ")";
        }
        else if (expr instanceof InstanceFieldReference && ((InstanceFieldReference)expr).object.actualType instanceof ClassType) {
            if (Objects.equals(((InstanceFieldReference)expr).field.parentInterface.getName(), "RegExpExecArray") && Objects.equals(((InstanceFieldReference)expr).field.getName(), "length"))
                return "len(" + this.main.expr(((InstanceFieldReference)expr).object) + ")";
        }
        else if (expr instanceof StaticMethodCallExpression && ((StaticMethodCallExpression)expr).getMethod().parentInterface instanceof Class)
            return this.convertMethod(((Class)((StaticMethodCallExpression)expr).getMethod().parentInterface), null, ((StaticMethodCallExpression)expr).getMethod(), ((StaticMethodCallExpression)expr).getArgs());
        return null;
    }
    
    public String stmt(Statement stmt) {
        return null;
    }
}