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
import io.onelang.std.core.Objects;
import OneLang.One.Ast.Expressions.RegexLiteral;
import io.onelang.std.json.JSON;
import java.util.List;
import java.util.ArrayList;
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
        if (Objects.equals(cls.getName(), "TsString")) {
            if (Objects.equals(method.getName(), "replace")) {
                if (args[0] instanceof RegexLiteral) {
                    this.main.imports.add("java.util.regex.Pattern");
                    return objR + ".replaceAll(" + JSON.stringify((((RegexLiteral)args[0])).pattern) + ", " + argsR[1] + ")";
                }
                
                return argsR[0] + ".replace(" + objR + ", " + argsR[1] + ")";
            }
        }
        else if (new ArrayList<>(List.of("console", "RegExp")).stream().anyMatch(cls.getName()::equals)) {
            this.main.imports.add("io.onelang.std.core." + cls.getName());
            return null;
        }
        else if (new ArrayList<>(List.of("JSON")).stream().anyMatch(cls.getName()::equals)) {
            this.main.imports.add("io.onelang.std.json." + cls.getName());
            return null;
        }
        else
            return null;
        
        return null;
    }
    
    public String expr(IExpression expr) {
        if (expr instanceof InstanceMethodCallExpression && ((InstanceMethodCallExpression)expr).object.actualType instanceof ClassType)
            return this.convertMethod(((ClassType)((InstanceMethodCallExpression)expr).object.actualType).decl, ((InstanceMethodCallExpression)expr).object, ((InstanceMethodCallExpression)expr).getMethod(), ((InstanceMethodCallExpression)expr).getArgs(), ((InstanceMethodCallExpression)expr).actualType);
        else if (expr instanceof InstancePropertyReference && ((InstancePropertyReference)expr).getObject().actualType instanceof ClassType) {
            if (Objects.equals(((InstancePropertyReference)expr).property.parentClass.getName(), "TsString") && Objects.equals(((InstancePropertyReference)expr).property.getName(), "length"))
                return this.main.expr(((InstancePropertyReference)expr).getObject()) + ".length()";
            if (Objects.equals(((InstancePropertyReference)expr).property.parentClass.getName(), "TsArray") && Objects.equals(((InstancePropertyReference)expr).property.getName(), "length"))
                return this.main.expr(((InstancePropertyReference)expr).getObject()) + "." + (this.isArray(((InstancePropertyReference)expr).getObject()) ? "length" : "size()");
        }
        else if (expr instanceof InstanceFieldReference && ((InstanceFieldReference)expr).getObject().actualType instanceof ClassType) {
            if (Objects.equals(((InstanceFieldReference)expr).field.parentInterface.getName(), "RegExpExecArray") && Objects.equals(((InstanceFieldReference)expr).field.getName(), "length"))
                return this.main.expr(((InstanceFieldReference)expr).getObject()) + ".length";
            if (Objects.equals(((InstanceFieldReference)expr).field.parentInterface.getName(), "Map") && Objects.equals(((InstanceFieldReference)expr).field.getName(), "size"))
                return this.main.expr(((InstanceFieldReference)expr).getObject()) + ".size()";
        }
        else if (expr instanceof StaticMethodCallExpression && ((StaticMethodCallExpression)expr).getMethod().parentInterface instanceof Class)
            return this.convertMethod(((Class)((StaticMethodCallExpression)expr).getMethod().parentInterface), null, ((StaticMethodCallExpression)expr).getMethod(), ((StaticMethodCallExpression)expr).getArgs(), ((StaticMethodCallExpression)expr).actualType);
        return null;
    }
    
    public String stmt(Statement stmt) {
        return null;
    }
}