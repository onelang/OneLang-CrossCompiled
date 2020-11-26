package OneLang.Generator.JavaPlugins.JsToJava;

import OneLang.Generator.IGeneratorPlugin.IGeneratorPlugin;
import OneLang.One.Ast.Expressions.InstanceMethodCallExpression;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.StaticMethodCallExpression;
import OneLang.One.Ast.Expressions.RegexLiteral;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Interfaces.IExpression;
import OneLang.Generator.JavaGenerator.JavaGenerator;

import OneLang.Generator.IGeneratorPlugin.IGeneratorPlugin;
import java.util.Set;
import OneLang.Generator.JavaGenerator.JavaGenerator;
import java.util.LinkedHashSet;
import java.util.Arrays;
import io.onelang.std.core.Objects;
import OneLang.One.Ast.Expressions.RegexLiteral;
import io.onelang.std.json.JSON;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Expressions.InstanceMethodCallExpression;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.Expressions.StaticMethodCallExpression;
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
    
    public String convertMethod(Class cls, Expression obj, Method method, Expression[] args) {
        var objR = obj == null ? null : this.main.expr(obj);
        var argsR = Arrays.stream(args).map(x -> this.main.expr(x)).toArray(String[]::new);
        if (Objects.equals(cls.getName(), "TsString")) {
            if (Objects.equals(method.getName(), "replace")) {
                if (args[0] instanceof RegexLiteral) {
                    this.main.imports.add("java.util.regex.Pattern");
                    return objR + ".replaceAll(" + JSON.stringify((((RegexLiteral)args[0])).pattern) + ", " + argsR[1] + ")";
                }
                
                return argsR[0] + ".replace(" + objR + ", " + argsR[1] + ")";
            }
        }
        else
            return null;
        
        return null;
    }
    
    public String expr(IExpression expr) {
        if (expr instanceof InstanceMethodCallExpression && ((InstanceMethodCallExpression)expr).object.actualType instanceof ClassType)
            return this.convertMethod(((ClassType)((InstanceMethodCallExpression)expr).object.actualType).decl, ((InstanceMethodCallExpression)expr).object, ((InstanceMethodCallExpression)expr).getMethod(), ((InstanceMethodCallExpression)expr).getArgs());
        else if (expr instanceof StaticMethodCallExpression && ((StaticMethodCallExpression)expr).getMethod().parentInterface instanceof Class)
            return this.convertMethod(((Class)((StaticMethodCallExpression)expr).getMethod().parentInterface), null, ((StaticMethodCallExpression)expr).getMethod(), ((StaticMethodCallExpression)expr).getArgs());
        return null;
    }
    
    public String stmt(Statement stmt) {
        return null;
    }
}