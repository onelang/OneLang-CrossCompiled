package OneLang.Generator.JavaGenerator;

import OneLang.One.Ast.Expressions.NewExpression;
import OneLang.One.Ast.Expressions.Identifier;
import OneLang.One.Ast.Expressions.TemplateString;
import OneLang.One.Ast.Expressions.ArrayLiteral;
import OneLang.One.Ast.Expressions.CastExpression;
import OneLang.One.Ast.Expressions.BooleanLiteral;
import OneLang.One.Ast.Expressions.StringLiteral;
import OneLang.One.Ast.Expressions.NumericLiteral;
import OneLang.One.Ast.Expressions.CharacterLiteral;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.ElementAccessExpression;
import OneLang.One.Ast.Expressions.BinaryExpression;
import OneLang.One.Ast.Expressions.UnresolvedCallExpression;
import OneLang.One.Ast.Expressions.ConditionalExpression;
import OneLang.One.Ast.Expressions.InstanceOfExpression;
import OneLang.One.Ast.Expressions.ParenthesizedExpression;
import OneLang.One.Ast.Expressions.RegexLiteral;
import OneLang.One.Ast.Expressions.UnaryExpression;
import OneLang.One.Ast.Expressions.UnaryType;
import OneLang.One.Ast.Expressions.MapLiteral;
import OneLang.One.Ast.Expressions.NullLiteral;
import OneLang.One.Ast.Expressions.AwaitExpression;
import OneLang.One.Ast.Expressions.UnresolvedNewExpression;
import OneLang.One.Ast.Expressions.UnresolvedMethodCallExpression;
import OneLang.One.Ast.Expressions.InstanceMethodCallExpression;
import OneLang.One.Ast.Expressions.NullCoalesceExpression;
import OneLang.One.Ast.Expressions.GlobalFunctionCallExpression;
import OneLang.One.Ast.Expressions.StaticMethodCallExpression;
import OneLang.One.Ast.Expressions.LambdaCallExpression;
import OneLang.One.Ast.Expressions.IMethodCallExpression;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.Statements.ReturnStatement;
import OneLang.One.Ast.Statements.UnsetStatement;
import OneLang.One.Ast.Statements.ThrowStatement;
import OneLang.One.Ast.Statements.ExpressionStatement;
import OneLang.One.Ast.Statements.VariableDeclaration;
import OneLang.One.Ast.Statements.BreakStatement;
import OneLang.One.Ast.Statements.ForeachStatement;
import OneLang.One.Ast.Statements.IfStatement;
import OneLang.One.Ast.Statements.WhileStatement;
import OneLang.One.Ast.Statements.ForStatement;
import OneLang.One.Ast.Statements.DoStatement;
import OneLang.One.Ast.Statements.ContinueStatement;
import OneLang.One.Ast.Statements.TryStatement;
import OneLang.One.Ast.Statements.Block;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.SourceFile;
import OneLang.One.Ast.Types.IVariable;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.Ast.Types.Interface;
import OneLang.One.Ast.Types.IInterface;
import OneLang.One.Ast.Types.MethodParameter;
import OneLang.One.Ast.Types.IVariableWithInitializer;
import OneLang.One.Ast.Types.Visibility;
import OneLang.One.Ast.Types.Package;
import OneLang.One.Ast.Types.IHasAttributesAndTrivia;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Types.ExportScopeRef;
import OneLang.One.Ast.AstTypes.VoidType;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.InterfaceType;
import OneLang.One.Ast.AstTypes.EnumType;
import OneLang.One.Ast.AstTypes.AnyType;
import OneLang.One.Ast.AstTypes.LambdaType;
import OneLang.One.Ast.AstTypes.NullType;
import OneLang.One.Ast.AstTypes.GenericsType;
import OneLang.One.Ast.AstTypes.TypeHelper;
import OneLang.One.Ast.AstTypes.IInterfaceType;
import OneLang.One.Ast.References.ThisReference;
import OneLang.One.Ast.References.EnumReference;
import OneLang.One.Ast.References.ClassReference;
import OneLang.One.Ast.References.MethodParameterReference;
import OneLang.One.Ast.References.VariableDeclarationReference;
import OneLang.One.Ast.References.ForVariableReference;
import OneLang.One.Ast.References.ForeachVariableReference;
import OneLang.One.Ast.References.SuperReference;
import OneLang.One.Ast.References.StaticFieldReference;
import OneLang.One.Ast.References.StaticPropertyReference;
import OneLang.One.Ast.References.InstanceFieldReference;
import OneLang.One.Ast.References.InstancePropertyReference;
import OneLang.One.Ast.References.EnumMemberReference;
import OneLang.One.Ast.References.CatchVariableReference;
import OneLang.One.Ast.References.GlobalFunctionReference;
import OneLang.One.Ast.References.StaticThisReference;
import OneLang.One.Ast.References.VariableReference;
import OneLang.Generator.GeneratedFile.GeneratedFile;
import OneLang.Generator.NameUtils.NameUtils;
import OneLang.Generator.IGenerator.IGenerator;
import OneLang.One.Ast.Interfaces.IExpression;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.Generator.IGeneratorPlugin.IGeneratorPlugin;
import OneLang.Generator.JavaPlugins.JsToJava.JsToJava;
import OneLang.One.ITransformer.ITransformer;
import OneLang.One.Transforms.ConvertNullCoalesce.ConvertNullCoalesce;
import OneLang.One.Transforms.UseDefaultCallArgsExplicitly.UseDefaultCallArgsExplicitly;
import OneLang.Generator.TemplateFileGeneratorPlugin.ExpressionValue;
import OneLang.Generator.TemplateFileGeneratorPlugin.LambdaValue;
import OneLang.Generator.TemplateFileGeneratorPlugin.TemplateFileGeneratorPlugin;
import OneLang.VM.Values.StringValue;

import OneLang.Generator.IGenerator.IGenerator;
import java.util.Set;
import OneLang.One.Ast.Types.IInterface;
import java.util.List;
import OneLang.Generator.IGeneratorPlugin.IGeneratorPlugin;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import OneLang.Generator.JavaPlugins.JsToJava.JsToJava;
import OneLang.One.ITransformer.ITransformer;
import OneLang.One.Transforms.ConvertNullCoalesce.ConvertNullCoalesce;
import OneLang.One.Transforms.UseDefaultCallArgsExplicitly.UseDefaultCallArgsExplicitly;
import OneLang.One.Ast.References.VariableReference;
import OneLang.One.Ast.Expressions.StaticMethodCallExpression;
import OneLang.One.Ast.Expressions.InstanceMethodCallExpression;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.Generator.TemplateFileGeneratorPlugin.TemplateFileGeneratorPlugin;
import OneLang.Generator.TemplateFileGeneratorPlugin.LambdaValue;
import OneLang.VM.Values.StringValue;
import OneLang.Generator.TemplateFileGeneratorPlugin.ExpressionValue;
import java.util.Arrays;
import io.onelang.std.core.RegExp;
import java.util.stream.Collectors;
import io.onelang.std.core.Objects;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.AstTypes.InterfaceType;
import OneLang.One.Ast.AstTypes.IInterfaceType;
import OneLang.One.Ast.AstTypes.VoidType;
import OneLang.One.Ast.AstTypes.EnumType;
import OneLang.One.Ast.AstTypes.AnyType;
import OneLang.One.Ast.AstTypes.NullType;
import OneLang.One.Ast.AstTypes.GenericsType;
import OneLang.One.Ast.AstTypes.LambdaType;
import OneLang.One.Ast.Types.IVariable;
import OneLang.One.Ast.Types.IHasAttributesAndTrivia;
import OneLang.One.Ast.Types.IVariableWithInitializer;
import OneLang.One.Ast.Expressions.ArrayLiteral;
import OneLang.One.Ast.Types.MethodParameter;
import OneLang.One.Ast.Expressions.IMethodCallExpression;
import io.onelang.std.core.StdArrayHelper;
import OneLang.One.Ast.Expressions.BinaryExpression;
import OneLang.One.Ast.Expressions.NewExpression;
import OneLang.One.Ast.Expressions.UnresolvedNewExpression;
import OneLang.One.Ast.Expressions.Identifier;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Ast.Expressions.UnresolvedCallExpression;
import OneLang.One.Ast.Expressions.UnresolvedMethodCallExpression;
import OneLang.One.Ast.Expressions.GlobalFunctionCallExpression;
import OneLang.One.Ast.Expressions.LambdaCallExpression;
import OneLang.One.Ast.Expressions.BooleanLiteral;
import OneLang.One.Ast.Expressions.StringLiteral;
import io.onelang.std.json.JSON;
import OneLang.One.Ast.Expressions.NumericLiteral;
import OneLang.One.Ast.Expressions.CharacterLiteral;
import OneLang.One.Ast.Expressions.ElementAccessExpression;
import OneLang.One.Ast.Expressions.TemplateString;
import OneLang.One.Ast.Expressions.ConditionalExpression;
import OneLang.One.Ast.References.InstanceFieldReference;
import OneLang.One.Ast.Expressions.CastExpression;
import OneLang.One.Ast.Expressions.InstanceOfExpression;
import OneLang.One.Ast.Expressions.ParenthesizedExpression;
import OneLang.One.Ast.Expressions.RegexLiteral;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.Ast.Statements.ReturnStatement;
import OneLang.One.Ast.Expressions.UnaryExpression;
import OneLang.One.Ast.Expressions.MapLiteral;
import OneLang.One.Ast.Expressions.NullLiteral;
import OneLang.One.Ast.Expressions.AwaitExpression;
import OneLang.One.Ast.References.ThisReference;
import OneLang.One.Ast.References.StaticThisReference;
import OneLang.One.Ast.References.EnumReference;
import OneLang.One.Ast.References.ClassReference;
import OneLang.One.Ast.References.MethodParameterReference;
import OneLang.One.Ast.References.VariableDeclarationReference;
import OneLang.One.Ast.References.ForVariableReference;
import OneLang.One.Ast.References.ForeachVariableReference;
import OneLang.One.Ast.References.CatchVariableReference;
import OneLang.One.Ast.References.GlobalFunctionReference;
import OneLang.One.Ast.References.SuperReference;
import OneLang.One.Ast.References.StaticFieldReference;
import OneLang.One.Ast.References.StaticPropertyReference;
import OneLang.One.Ast.References.InstancePropertyReference;
import OneLang.One.Ast.References.EnumMemberReference;
import OneLang.One.Ast.Expressions.NullCoalesceExpression;
import OneLang.One.Ast.Interfaces.IExpression;
import OneLang.One.Ast.Statements.IfStatement;
import OneLang.One.Ast.Statements.VariableDeclaration;
import OneLang.One.Ast.Statements.Block;
import OneLang.One.Ast.Statements.BreakStatement;
import OneLang.One.Ast.Statements.UnsetStatement;
import OneLang.One.Ast.Statements.ThrowStatement;
import OneLang.One.Ast.Statements.ExpressionStatement;
import OneLang.One.Ast.Statements.ForeachStatement;
import OneLang.One.Ast.Statements.WhileStatement;
import OneLang.One.Ast.Statements.ForStatement;
import OneLang.One.Ast.Statements.DoStatement;
import OneLang.One.Ast.Statements.TryStatement;
import OneLang.One.Ast.Statements.ContinueStatement;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Types.Field;
import java.util.stream.Stream;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.Interface;
import java.util.regex.Pattern;
import OneLang.One.Ast.Types.ExportScopeRef;
import OneLang.Generator.GeneratedFile.GeneratedFile;
import OneLang.One.Ast.Types.Package;

public class JavaGenerator implements IGenerator {
    public Set<String> imports;
    public IInterface currentClass;
    public String[] reservedWords;
    public String[] fieldToMethodHack;
    public List<IGeneratorPlugin> plugins;
    
    public JavaGenerator()
    {
        this.imports = new LinkedHashSet<String>();
        this.reservedWords = new String[] { "class", "interface", "throws", "package", "throw", "boolean" };
        this.fieldToMethodHack = new String[0];
        this.plugins = new ArrayList<IGeneratorPlugin>();
        this.plugins.add(new JsToJava(this));
    }
    
    public String getLangName() {
        return "Java";
    }
    
    public String getExtension() {
        return "java";
    }
    
    public ITransformer[] getTransforms() {
        return new ITransformer[] { ((ITransformer)new ConvertNullCoalesce()), ((ITransformer)new UseDefaultCallArgsExplicitly()) };
    }
    
    public void addInclude(String include) {
        this.imports.add(include);
    }
    
    public Boolean isArray(Expression arrayExpr) {
        // TODO: InstanceMethodCallExpression is a hack, we should introduce real stream handling
        return arrayExpr instanceof VariableReference && !((VariableReference)arrayExpr).getVariable().getMutability().mutated || arrayExpr instanceof StaticMethodCallExpression || arrayExpr instanceof InstanceMethodCallExpression;
    }
    
    public String arrayStream(Expression arrayExpr) {
        var isArray = this.isArray(arrayExpr);
        var objR = this.expr(arrayExpr);
        if (isArray)
            this.imports.add("java.util.Arrays");
        return isArray ? "Arrays.stream(" + objR + ")" : objR + ".stream()";
    }
    
    public String toArray(IType arrayType, Integer typeArgIdx) {
        var type = (((ClassType)arrayType)).getTypeArguments()[typeArgIdx];
        return "toArray(" + this.type(type, true, false) + "[]::new)";
    }
    
    public void addPlugin(IGeneratorPlugin plugin) {
        this.plugins.add(plugin);
        
        // TODO: hack?
        if (plugin instanceof TemplateFileGeneratorPlugin)
            ((TemplateFileGeneratorPlugin)plugin).modelGlobals.put("toStream", new LambdaValue(args -> new StringValue(this.arrayStream((((ExpressionValue)args[0])).value))));
    }
    
    public String name_(String name) {
        if (Arrays.stream(this.reservedWords).anyMatch(name::equals))
            name += "_";
        if (Arrays.stream(this.fieldToMethodHack).anyMatch(name::equals))
            name += "()";
        var nameParts = name.split("-", -1);
        for (Integer i = 1; i < nameParts.length; i++)
            nameParts[i] = nameParts[i].substring(0, 0 + 1).toUpperCase() + nameParts[i].substring(1);
        name = Arrays.stream(nameParts).collect(Collectors.joining(""));
        if (Objects.equals(name, "_"))
            name = "unused";
        return name;
    }
    
    public String leading(Statement item) {
        var result = "";
        if (item.getLeadingTrivia() != null && item.getLeadingTrivia().length() > 0)
            result += item.getLeadingTrivia();
        //if (item.attributes !== null)
        //    result += Object.keys(item.attributes).map(x => `// @${x} ${item.attributes[x]}\n`).join("");
        return result;
    }
    
    public String preArr(String prefix, String[] value) {
        return value.length > 0 ? prefix + Arrays.stream(value).collect(Collectors.joining(", ")) : "";
    }
    
    public String preIf(String prefix, Boolean condition) {
        return condition ? prefix : "";
    }
    
    public String pre(String prefix, String value) {
        return value != null ? prefix + value : "";
    }
    
    public String typeArgs(String[] args) {
        return args != null && args.length > 0 ? "<" + Arrays.stream(args).collect(Collectors.joining(", ")) + ">" : "";
    }
    
    public String typeArgs2(IType[] args) {
        return this.typeArgs(Arrays.stream(args).map(x -> this.type(x, true, false)).toArray(String[]::new));
    }
    
    public String type(IType t, Boolean mutates, Boolean isNew) {
        if (t instanceof ClassType || t instanceof InterfaceType) {
            var decl = (((IInterfaceType)t)).getDecl();
            if (decl.getParentFile().exportScope != null)
                this.imports.add(this.toImport(decl.getParentFile().exportScope) + "." + decl.getName());
        }
        
        if (t instanceof ClassType) {
            var typeArgs = this.typeArgs(Arrays.stream(((ClassType)t).getTypeArguments()).map(x -> this.type(x, true, false)).toArray(String[]::new));
            if (Objects.equals(((ClassType)t).decl.getName(), "TsString"))
                return "String";
            else if (Objects.equals(((ClassType)t).decl.getName(), "TsBoolean"))
                return "Boolean";
            else if (Objects.equals(((ClassType)t).decl.getName(), "TsNumber"))
                return "Integer";
            else if (Objects.equals(((ClassType)t).decl.getName(), "TsArray")) {
                var realType = isNew ? "ArrayList" : "List";
                if (mutates) {
                    this.imports.add("java.util." + realType);
                    return realType + "<" + this.type(((ClassType)t).getTypeArguments()[0], true, false) + ">";
                }
                else
                    return this.type(((ClassType)t).getTypeArguments()[0], true, false) + "[]";
            }
            else if (Objects.equals(((ClassType)t).decl.getName(), "Map")) {
                var realType = isNew ? "LinkedHashMap" : "Map";
                this.imports.add("java.util." + realType);
                return realType + "<" + this.type(((ClassType)t).getTypeArguments()[0], true, false) + ", " + this.type(((ClassType)t).getTypeArguments()[1], true, false) + ">";
            }
            else if (Objects.equals(((ClassType)t).decl.getName(), "Set")) {
                var realType = isNew ? "LinkedHashSet" : "Set";
                this.imports.add("java.util." + realType);
                return realType + "<" + this.type(((ClassType)t).getTypeArguments()[0], true, false) + ">";
            }
            else if (Objects.equals(((ClassType)t).decl.getName(), "Promise"))
                return ((ClassType)t).getTypeArguments()[0] instanceof VoidType ? "void" : this.type(((ClassType)t).getTypeArguments()[0], true, false);
            else if (Objects.equals(((ClassType)t).decl.getName(), "Object"))
                //this.imports.add("System");
                return "Object";
            else if (Objects.equals(((ClassType)t).decl.getName(), "TsMap")) {
                var realType = isNew ? "LinkedHashMap" : "Map";
                this.imports.add("java.util." + realType);
                return realType + "<String, " + this.type(((ClassType)t).getTypeArguments()[0], true, false) + ">";
            }
            return this.name_(((ClassType)t).decl.getName()) + typeArgs;
        }
        else if (t instanceof InterfaceType)
            return this.name_(((InterfaceType)t).decl.getName()) + this.typeArgs(Arrays.stream(((InterfaceType)t).getTypeArguments()).map(x -> this.type(x, true, false)).toArray(String[]::new));
        else if (t instanceof VoidType)
            return "void";
        else if (t instanceof EnumType)
            return this.name_(((EnumType)t).decl.getName());
        else if (t instanceof AnyType)
            return "Object";
        else if (t instanceof NullType)
            return "null";
        else if (t instanceof GenericsType)
            return ((GenericsType)t).typeVarName;
        else if (t instanceof LambdaType) {
            var isFunc = !(((LambdaType)t).returnType instanceof VoidType);
            var paramTypes = new ArrayList<>(Arrays.asList(Arrays.stream(((LambdaType)t).parameters).map(x -> this.type(x.getType(), false, false)).toArray(String[]::new)));
            if (isFunc)
                paramTypes.add(this.type(((LambdaType)t).returnType, false, false));
            this.imports.add("java.util.function." + (isFunc ? "Function" : "Consumer"));
            return (isFunc ? "Function" : "Consumer") + "<" + paramTypes.stream().collect(Collectors.joining(", ")) + ">";
        }
        else if (t == null)
            return "/* TODO */ object";
        else
            return "/* MISSING */";
    }
    
    public Boolean isTsArray(IType type) {
        return type instanceof ClassType && Objects.equals(((ClassType)type).decl.getName(), "TsArray");
    }
    
    public String vis(Visibility v) {
        return v == Visibility.Private ? "private" : v == Visibility.Protected ? "protected" : v == Visibility.Public ? "public" : "/* TODO: not set */public";
    }
    
    public String varType(IVariable v, IHasAttributesAndTrivia attr) {
        String type;
        if (attr != null && attr.getAttributes() != null && attr.getAttributes().containsKey("java-type"))
            type = attr.getAttributes().get("java-type");
        else if (v.getType() instanceof ClassType && Objects.equals(((ClassType)v.getType()).decl.getName(), "TsArray")) {
            if (v.getMutability().mutated) {
                this.imports.add("java.util.List");
                type = "List<" + this.type(((ClassType)v.getType()).getTypeArguments()[0], true, false) + ">";
            }
            else
                type = this.type(((ClassType)v.getType()).getTypeArguments()[0], true, false) + "[]";
        }
        else
            type = this.type(v.getType(), true, false);
        return type;
    }
    
    public String varWoInit(IVariable v, IHasAttributesAndTrivia attr) {
        return this.varType(v, attr) + " " + this.name_(v.getName());
    }
    
    public String var(IVariableWithInitializer v, IHasAttributesAndTrivia attrs) {
        return this.varWoInit(v, attrs) + (v.getInitializer() != null ? " = " + this.expr(v.getInitializer()) : "");
    }
    
    public String exprCall(IType[] typeArgs, Expression[] args) {
        return this.typeArgs2(typeArgs) + "(" + Arrays.stream(Arrays.stream(args).map(x -> this.expr(x)).toArray(String[]::new)).collect(Collectors.joining(", ")) + ")";
    }
    
    public String mutateArg(Expression arg, Boolean shouldBeMutable) {
        if (this.isTsArray(arg.actualType)) {
            var itemType = (((ClassType)arg.actualType)).getTypeArguments()[0];
            if (arg instanceof ArrayLiteral && !shouldBeMutable)
                return ((ArrayLiteral)arg).items.length == 0 && !this.isTsArray(itemType) ? "new " + this.type(itemType, true, false) + "[0]" : "new " + this.type(itemType, true, false) + "[] { " + Arrays.stream(Arrays.stream(((ArrayLiteral)arg).items).map(x -> this.expr(x)).toArray(String[]::new)).collect(Collectors.joining(", ")) + " }";
            
            var currentlyMutable = shouldBeMutable;
            if (arg instanceof VariableReference)
                currentlyMutable = ((VariableReference)arg).getVariable().getMutability().mutated;
            else if (arg instanceof InstanceMethodCallExpression || arg instanceof StaticMethodCallExpression)
                currentlyMutable = false;
            
            if (currentlyMutable && !shouldBeMutable)
                return this.expr(arg) + ".toArray(" + this.type(itemType, true, false) + "[]::new)";
            else if (!currentlyMutable && shouldBeMutable) {
                this.imports.add("java.util.Arrays");
                this.imports.add("java.util.ArrayList");
                return "new ArrayList<>(Arrays.asList(" + this.expr(arg) + "))";
            }
        }
        return this.expr(arg);
    }
    
    public String mutatedExpr(Expression expr, Expression toWhere) {
        if (toWhere instanceof VariableReference) {
            var v = ((VariableReference)toWhere).getVariable();
            if (this.isTsArray(v.getType()))
                return this.mutateArg(expr, v.getMutability().mutated);
        }
        return this.expr(expr);
    }
    
    public String callParams(Expression[] args, MethodParameter[] params) {
        var argReprs = new ArrayList<String>();
        for (Integer i = 0; i < args.length; i++)
            argReprs.add(this.isTsArray(params[i].getType()) ? this.mutateArg(args[i], params[i].getMutability().mutated) : this.expr(args[i]));
        return "(" + argReprs.stream().collect(Collectors.joining(", ")) + ")";
    }
    
    public String methodCall(IMethodCallExpression expr) {
        return this.name_(expr.getMethod().getName()) + this.typeArgs2(expr.getTypeArgs()) + this.callParams(expr.getArgs(), expr.getMethod().getParameters());
    }
    
    public String inferExprNameForType(IType type) {
        if (type instanceof ClassType && StdArrayHelper.allMatch(((ClassType)type).getTypeArguments(), (x, unused) -> x instanceof ClassType)) {
            var fullName = Arrays.stream(Arrays.stream(((ClassType)type).getTypeArguments()).map(x -> (((ClassType)x)).decl.getName()).toArray(String[]::new)).collect(Collectors.joining("")) + ((ClassType)type).decl.getName();
            return NameUtils.shortName(fullName);
        }
        return null;
    }
    
    public Boolean isSetExpr(VariableReference varRef) {
        return varRef.parentNode instanceof BinaryExpression && ((BinaryExpression)varRef.parentNode).left == varRef && new ArrayList<>(List.of("=", "+=", "-=")).stream().anyMatch(((BinaryExpression)varRef.parentNode).operator::equals);
    }
    
    public String expr(IExpression expr) {
        for (var plugin : this.plugins) {
            var result = plugin.expr(expr);
            if (result != null)
                return result;
        }
        
        var res = "UNKNOWN-EXPR";
        if (expr instanceof NewExpression)
            res = "new " + this.type(((NewExpression)expr).cls, true, true) + this.callParams(((NewExpression)expr).args, ((NewExpression)expr).cls.decl.constructor_ != null ? ((NewExpression)expr).cls.decl.constructor_.getParameters() : new MethodParameter[0]);
        else if (expr instanceof UnresolvedNewExpression)
            res = "/* TODO: UnresolvedNewExpression */ new " + this.type(((UnresolvedNewExpression)expr).cls, true, false) + "(" + Arrays.stream(Arrays.stream(((UnresolvedNewExpression)expr).args).map(x -> this.expr(x)).toArray(String[]::new)).collect(Collectors.joining(", ")) + ")";
        else if (expr instanceof Identifier)
            res = "/* TODO: Identifier */ " + ((Identifier)expr).text;
        else if (expr instanceof PropertyAccessExpression)
            res = "/* TODO: PropertyAccessExpression */ " + this.expr(((PropertyAccessExpression)expr).object) + "." + ((PropertyAccessExpression)expr).propertyName;
        else if (expr instanceof UnresolvedCallExpression)
            res = "/* TODO: UnresolvedCallExpression */ " + this.expr(((UnresolvedCallExpression)expr).func) + this.exprCall(((UnresolvedCallExpression)expr).typeArgs, ((UnresolvedCallExpression)expr).args);
        else if (expr instanceof UnresolvedMethodCallExpression)
            res = "/* TODO: UnresolvedMethodCallExpression */ " + this.expr(((UnresolvedMethodCallExpression)expr).object) + "." + ((UnresolvedMethodCallExpression)expr).methodName + this.exprCall(((UnresolvedMethodCallExpression)expr).typeArgs, ((UnresolvedMethodCallExpression)expr).args);
        else if (expr instanceof InstanceMethodCallExpression)
            res = this.expr(((InstanceMethodCallExpression)expr).object) + "." + this.methodCall(((InstanceMethodCallExpression)expr));
        else if (expr instanceof StaticMethodCallExpression)
            res = this.name_(((StaticMethodCallExpression)expr).getMethod().parentInterface.getName()) + "." + this.methodCall(((StaticMethodCallExpression)expr));
        else if (expr instanceof GlobalFunctionCallExpression)
            res = "Global." + this.name_(((GlobalFunctionCallExpression)expr).func.getName()) + this.exprCall(new IType[0], ((GlobalFunctionCallExpression)expr).args);
        else if (expr instanceof LambdaCallExpression)
            res = this.expr(((LambdaCallExpression)expr).method) + ".apply(" + Arrays.stream(Arrays.stream(((LambdaCallExpression)expr).args).map(x -> this.expr(x)).toArray(String[]::new)).collect(Collectors.joining(", ")) + ")";
        else if (expr instanceof BooleanLiteral)
            res = (((BooleanLiteral)expr).boolValue ? "true" : "false");
        else if (expr instanceof StringLiteral)
            res = JSON.stringify(((StringLiteral)expr).stringValue);
        else if (expr instanceof NumericLiteral)
            res = ((NumericLiteral)expr).valueAsText;
        else if (expr instanceof CharacterLiteral)
            res = "'" + ((CharacterLiteral)expr).charValue + "'";
        else if (expr instanceof ElementAccessExpression)
            res = this.expr(((ElementAccessExpression)expr).object) + ".get(" + this.expr(((ElementAccessExpression)expr).elementExpr) + ")";
        else if (expr instanceof TemplateString) {
            var parts = new ArrayList<String>();
            for (var part : ((TemplateString)expr).parts) {
                if (part.isLiteral) {
                    var lit = "";
                    for (Integer i = 0; i < part.literalText.length(); i++) {
                        var chr = part.literalText.substring(i, i + 1);
                        if (Objects.equals(chr, "\n"))
                            lit += "\\n";
                        else if (Objects.equals(chr, "\r"))
                            lit += "\\r";
                        else if (Objects.equals(chr, "\t"))
                            lit += "\\t";
                        else if (Objects.equals(chr, "\\"))
                            lit += "\\\\";
                        else if (Objects.equals(chr, "\""))
                            lit += "\\\"";
                        else {
                            var chrCode = (int)chr.charAt(0);
                            if (32 <= chrCode && chrCode <= 126)
                                lit += chr;
                            else
                                throw new Error("invalid char in template string (code=" + chrCode + ")");
                        }
                    }
                    parts.add("\"" + lit + "\"");
                }
                else {
                    var repr = this.expr(part.expression);
                    parts.add(part.expression instanceof ConditionalExpression ? "(" + repr + ")" : repr);
                }
            }
            res = parts.stream().collect(Collectors.joining(" + "));
        }
        else if (expr instanceof BinaryExpression) {
            var modifies = new ArrayList<>(List.of("=", "+=", "-=")).stream().anyMatch(((BinaryExpression)expr).operator::equals);
            if (modifies && ((BinaryExpression)expr).left instanceof InstanceFieldReference && this.useGetterSetter(((InstanceFieldReference)((BinaryExpression)expr).left)))
                res = this.expr(((InstanceFieldReference)((BinaryExpression)expr).left).getObject()) + ".set" + this.ucFirst(((InstanceFieldReference)((BinaryExpression)expr).left).field.getName()) + "(" + this.mutatedExpr(((BinaryExpression)expr).right, Objects.equals(((BinaryExpression)expr).operator, "=") ? ((InstanceFieldReference)((BinaryExpression)expr).left) : null) + ")";
            else if (new ArrayList<>(List.of("==", "!=")).stream().anyMatch(((BinaryExpression)expr).operator::equals)) {
                var lit = this.currentClass.getParentFile().literalTypes;
                var leftType = ((BinaryExpression)expr).left.getType();
                var rightType = ((BinaryExpression)expr).right.getType();
                var useEquals = TypeHelper.equals(leftType, lit.string) && rightType != null && TypeHelper.equals(rightType, lit.string);
                if (useEquals) {
                    this.imports.add("io.onelang.std.core.Objects");
                    res = (Objects.equals(((BinaryExpression)expr).operator, "!=") ? "!" : "") + "Objects.equals(" + this.expr(((BinaryExpression)expr).left) + ", " + this.expr(((BinaryExpression)expr).right) + ")";
                }
                else
                    res = this.expr(((BinaryExpression)expr).left) + " " + ((BinaryExpression)expr).operator + " " + this.expr(((BinaryExpression)expr).right);
            }
            else
                res = this.expr(((BinaryExpression)expr).left) + " " + ((BinaryExpression)expr).operator + " " + this.mutatedExpr(((BinaryExpression)expr).right, Objects.equals(((BinaryExpression)expr).operator, "=") ? ((BinaryExpression)expr).left : null);
        }
        else if (expr instanceof ArrayLiteral) {
            if (((ArrayLiteral)expr).items.length == 0)
                res = "new " + this.type(((ArrayLiteral)expr).actualType, true, true) + "()";
            else {
                this.imports.add("java.util.List");
                this.imports.add("java.util.ArrayList");
                res = "new ArrayList<>(List.of(" + Arrays.stream(Arrays.stream(((ArrayLiteral)expr).items).map(x -> this.expr(x)).toArray(String[]::new)).collect(Collectors.joining(", ")) + "))";
            }
        }
        else if (expr instanceof CastExpression)
            res = "((" + this.type(((CastExpression)expr).newType, true, false) + ")" + this.expr(((CastExpression)expr).expression) + ")";
        else if (expr instanceof ConditionalExpression)
            res = this.expr(((ConditionalExpression)expr).condition) + " ? " + this.expr(((ConditionalExpression)expr).whenTrue) + " : " + this.mutatedExpr(((ConditionalExpression)expr).whenFalse, ((ConditionalExpression)expr).whenTrue);
        else if (expr instanceof InstanceOfExpression)
            res = this.expr(((InstanceOfExpression)expr).expr) + " instanceof " + this.type(((InstanceOfExpression)expr).checkType, true, false);
        else if (expr instanceof ParenthesizedExpression)
            res = "(" + this.expr(((ParenthesizedExpression)expr).expression) + ")";
        else if (expr instanceof RegexLiteral) {
            this.imports.add("io.onelang.std.core.RegExp");
            res = "new RegExp(" + JSON.stringify(((RegexLiteral)expr).pattern) + ")";
        }
        else if (expr instanceof Lambda) {
            String body;
            if (((Lambda)expr).getBody().statements.size() == 1 && ((Lambda)expr).getBody().statements.get(0) instanceof ReturnStatement)
                body = " " + this.expr((((ReturnStatement)((Lambda)expr).getBody().statements.get(0))).expression);
            else
                body = this.block(((Lambda)expr).getBody(), false);
            
            var params = Arrays.stream(((Lambda)expr).getParameters()).map(x -> this.name_(x.getName())).toArray(String[]::new);
            
            res = (params.length == 1 ? params[0] : "(" + Arrays.stream(params).collect(Collectors.joining(", ")) + ")") + " ->" + body;
        }
        else if (expr instanceof UnaryExpression && ((UnaryExpression)expr).unaryType == UnaryType.Prefix)
            res = ((UnaryExpression)expr).operator + this.expr(((UnaryExpression)expr).operand);
        else if (expr instanceof UnaryExpression && ((UnaryExpression)expr).unaryType == UnaryType.Postfix)
            res = this.expr(((UnaryExpression)expr).operand) + ((UnaryExpression)expr).operator;
        else if (expr instanceof MapLiteral) {
            if (((MapLiteral)expr).items.length > 10)
                throw new Error("MapLiteral is only supported with maximum of 10 items");
            if (((MapLiteral)expr).items.length == 0)
                res = "new " + this.type(((MapLiteral)expr).actualType, true, true) + "()";
            else {
                this.imports.add("java.util.Map");
                var repr = Arrays.stream(Arrays.stream(((MapLiteral)expr).items).map(item -> JSON.stringify(item.key) + ", " + this.expr(item.value)).toArray(String[]::new)).collect(Collectors.joining(", "));
                res = "Map.of(" + repr + ")";
            }
        }
        else if (expr instanceof NullLiteral)
            res = "null";
        else if (expr instanceof AwaitExpression)
            res = this.expr(((AwaitExpression)expr).expr);
        else if (expr instanceof ThisReference)
            res = "this";
        else if (expr instanceof StaticThisReference)
            res = this.currentClass.getName();
        else if (expr instanceof EnumReference)
            res = this.name_(((EnumReference)expr).decl.getName());
        else if (expr instanceof ClassReference)
            res = this.name_(((ClassReference)expr).decl.getName());
        else if (expr instanceof MethodParameterReference)
            res = this.name_(((MethodParameterReference)expr).decl.getName());
        else if (expr instanceof VariableDeclarationReference)
            res = this.name_(((VariableDeclarationReference)expr).decl.getName());
        else if (expr instanceof ForVariableReference)
            res = this.name_(((ForVariableReference)expr).decl.getName());
        else if (expr instanceof ForeachVariableReference)
            res = this.name_(((ForeachVariableReference)expr).decl.getName());
        else if (expr instanceof CatchVariableReference)
            res = this.name_(((CatchVariableReference)expr).decl.getName());
        else if (expr instanceof GlobalFunctionReference)
            res = this.name_(((GlobalFunctionReference)expr).decl.getName());
        else if (expr instanceof SuperReference)
            res = "super";
        else if (expr instanceof StaticFieldReference)
            res = this.name_(((StaticFieldReference)expr).decl.parentInterface.getName()) + "." + this.name_(((StaticFieldReference)expr).decl.getName());
        else if (expr instanceof StaticPropertyReference)
            res = this.name_(((StaticPropertyReference)expr).decl.parentClass.getName()) + "." + this.name_(((StaticPropertyReference)expr).decl.getName());
        else if (expr instanceof InstanceFieldReference) {
            // TODO: unified handling of field -> property conversion?
            if (this.useGetterSetter(((InstanceFieldReference)expr)))
                res = this.expr(((InstanceFieldReference)expr).getObject()) + ".get" + this.ucFirst(((InstanceFieldReference)expr).field.getName()) + "()";
            else
                res = this.expr(((InstanceFieldReference)expr).getObject()) + "." + this.name_(((InstanceFieldReference)expr).field.getName());
        }
        else if (expr instanceof InstancePropertyReference)
            res = this.expr(((InstancePropertyReference)expr).getObject()) + "." + (this.isSetExpr(((InstancePropertyReference)expr)) ? "set" : "get") + this.ucFirst(((InstancePropertyReference)expr).property.getName()) + "()";
        else if (expr instanceof EnumMemberReference)
            res = this.name_(((EnumMemberReference)expr).decl.parentEnum.getName()) + "." + this.name_(((EnumMemberReference)expr).decl.name);
        else if (expr instanceof NullCoalesceExpression)
            res = this.expr(((NullCoalesceExpression)expr).defaultExpr) + " != null ? " + this.expr(((NullCoalesceExpression)expr).defaultExpr) + " : " + this.mutatedExpr(((NullCoalesceExpression)expr).exprIfNull, ((NullCoalesceExpression)expr).defaultExpr);
        else { }
        return res;
    }
    
    public Boolean useGetterSetter(InstanceFieldReference fieldRef) {
        return fieldRef.getObject().actualType instanceof InterfaceType || (fieldRef.field.interfaceDeclarations != null && fieldRef.field.interfaceDeclarations.length > 0);
    }
    
    public String block(Block block, Boolean allowOneLiner) {
        var stmtLen = block.statements.size();
        return stmtLen == 0 ? " { }" : allowOneLiner && stmtLen == 1 && !(block.statements.get(0) instanceof IfStatement) && !(block.statements.get(0) instanceof VariableDeclaration) ? "\n" + this.pad(this.rawBlock(block)) : " {\n" + this.pad(this.rawBlock(block)) + "\n}";
    }
    
    public String stmtDefault(Statement stmt) {
        var res = "UNKNOWN-STATEMENT";
        if (stmt instanceof BreakStatement)
            res = "break;";
        else if (stmt instanceof ReturnStatement)
            res = ((ReturnStatement)stmt).expression == null ? "return;" : "return " + this.mutateArg(((ReturnStatement)stmt).expression, false) + ";";
        else if (stmt instanceof UnsetStatement)
            res = "/* unset " + this.expr(((UnsetStatement)stmt).expression) + "; */";
        else if (stmt instanceof ThrowStatement)
            res = "throw " + this.expr(((ThrowStatement)stmt).expression) + ";";
        else if (stmt instanceof ExpressionStatement)
            res = this.expr(((ExpressionStatement)stmt).expression) + ";";
        else if (stmt instanceof VariableDeclaration) {
            if (((VariableDeclaration)stmt).getInitializer() instanceof NullLiteral)
                res = this.type(((VariableDeclaration)stmt).getType(), ((VariableDeclaration)stmt).getMutability().mutated, false) + " " + this.name_(((VariableDeclaration)stmt).getName()) + " = null;";
            else if (((VariableDeclaration)stmt).getInitializer() != null)
                res = "var " + this.name_(((VariableDeclaration)stmt).getName()) + " = " + this.mutateArg(((VariableDeclaration)stmt).getInitializer(), ((VariableDeclaration)stmt).getMutability().mutated) + ";";
            else
                res = this.type(((VariableDeclaration)stmt).getType(), true, false) + " " + this.name_(((VariableDeclaration)stmt).getName()) + ";";
        }
        else if (stmt instanceof ForeachStatement)
            res = "for (var " + this.name_(((ForeachStatement)stmt).itemVar.getName()) + " : " + this.expr(((ForeachStatement)stmt).items) + ")" + this.block(((ForeachStatement)stmt).body, true);
        else if (stmt instanceof IfStatement) {
            var elseIf = ((IfStatement)stmt).else_ != null && ((IfStatement)stmt).else_.statements.size() == 1 && ((IfStatement)stmt).else_.statements.get(0) instanceof IfStatement;
            res = "if (" + this.expr(((IfStatement)stmt).condition) + ")" + this.block(((IfStatement)stmt).then, true);
            res += (elseIf ? "\nelse " + this.stmt(((IfStatement)stmt).else_.statements.get(0)) : "") + (!elseIf && ((IfStatement)stmt).else_ != null ? "\nelse" + this.block(((IfStatement)stmt).else_, true) : "");
        }
        else if (stmt instanceof WhileStatement)
            res = "while (" + this.expr(((WhileStatement)stmt).condition) + ")" + this.block(((WhileStatement)stmt).body, true);
        else if (stmt instanceof ForStatement)
            res = "for (" + (((ForStatement)stmt).itemVar != null ? this.var(((ForStatement)stmt).itemVar, null) : "") + "; " + this.expr(((ForStatement)stmt).condition) + "; " + this.expr(((ForStatement)stmt).incrementor) + ")" + this.block(((ForStatement)stmt).body, true);
        else if (stmt instanceof DoStatement)
            res = "do" + this.block(((DoStatement)stmt).body, true) + " while (" + this.expr(((DoStatement)stmt).condition) + ");";
        else if (stmt instanceof TryStatement) {
            res = "try" + this.block(((TryStatement)stmt).tryBody, false);
            if (((TryStatement)stmt).catchBody != null)
                //this.imports.add("System");
                res += " catch (Exception " + this.name_(((TryStatement)stmt).catchVar.getName()) + ") " + this.block(((TryStatement)stmt).catchBody, false);
            if (((TryStatement)stmt).finallyBody != null)
                res += "finally" + this.block(((TryStatement)stmt).finallyBody, true);
        }
        else if (stmt instanceof ContinueStatement)
            res = "continue;";
        else { }
        return res;
    }
    
    public String stmt(Statement stmt) {
        String res = null;
        
        if (stmt.getAttributes() != null && stmt.getAttributes().containsKey("java-import"))
            this.imports.add(stmt.getAttributes().get("java-import"));
        
        if (stmt.getAttributes() != null && stmt.getAttributes().containsKey("java"))
            res = stmt.getAttributes().get("java");
        else {
            for (var plugin : this.plugins) {
                res = plugin.stmt(stmt);
                if (res != null)
                    break;
            }
            
            if (res == null)
                res = this.stmtDefault(stmt);
        }
        
        return this.leading(stmt) + res;
    }
    
    public String stmts(Statement[] stmts) {
        return Arrays.stream(Arrays.stream(stmts).map(stmt -> this.stmt(stmt)).toArray(String[]::new)).collect(Collectors.joining("\n"));
    }
    
    public String rawBlock(Block block) {
        return this.stmts(block.statements.toArray(Statement[]::new));
    }
    
    public String methodGen(String prefix, MethodParameter[] params, String body) {
        return prefix + "(" + Arrays.stream(Arrays.stream(params).map(p -> this.varWoInit(p, p)).toArray(String[]::new)).collect(Collectors.joining(", ")) + ")" + body;
    }
    
    public String method(Method method, Boolean isCls) {
        // TODO: final
        var prefix = (isCls ? this.vis(method.getVisibility()) + " " : "") + this.preIf("static ", method.getIsStatic()) + this.preIf("/* throws */ ", method.getThrows()) + (method.typeArguments.length > 0 ? "<" + Arrays.stream(method.typeArguments).collect(Collectors.joining(", ")) + "> " : "") + this.type(method.returns, false, false) + " " + this.name_(method.getName());
        
        return this.methodGen(prefix, method.getParameters(), method.getBody() == null ? ";" : " {\n" + this.pad(this.stmts(method.getBody().statements.toArray(Statement[]::new))) + "\n}");
    }
    
    public String class_(Class cls) {
        this.currentClass = cls;
        var resList = new ArrayList<String>();
        
        var staticConstructorStmts = new ArrayList<Statement>();
        var complexFieldInits = new ArrayList<Statement>();
        var fieldReprs = new ArrayList<String>();
        var propReprs = new ArrayList<String>();
        for (var field : cls.getFields()) {
            var isInitializerComplex = field.getInitializer() != null && !(field.getInitializer() instanceof StringLiteral) && !(field.getInitializer() instanceof BooleanLiteral) && !(field.getInitializer() instanceof NumericLiteral);
            
            var prefix = this.vis(field.getVisibility()) + " " + this.preIf("static ", field.getIsStatic());
            if (field.interfaceDeclarations.length > 0) {
                var varType = this.varType(field, field);
                var name = this.name_(field.getName());
                var pname = this.ucFirst(field.getName());
                var setToFalse = TypeHelper.equals(field.getType(), this.currentClass.getParentFile().literalTypes.boolean_);
                propReprs.add(varType + " " + name + (setToFalse ? " = false" : field.getInitializer() != null ? " = " + this.expr(field.getInitializer()) : "") + ";\n" + prefix + varType + " get" + pname + "() { return this." + name + "; }\n" + prefix + "void set" + pname + "(" + varType + " value) { this." + name + " = value; }");
            }
            else if (isInitializerComplex) {
                if (field.getIsStatic())
                    staticConstructorStmts.add(new ExpressionStatement(new BinaryExpression(new StaticFieldReference(field), "=", field.getInitializer())));
                else
                    complexFieldInits.add(new ExpressionStatement(new BinaryExpression(new InstanceFieldReference(new ThisReference(cls), field), "=", field.getInitializer())));
                
                fieldReprs.add(prefix + this.varWoInit(field, field) + ";");
            }
            else
                fieldReprs.add(prefix + this.var(field, field) + ";");
        }
        resList.add(fieldReprs.stream().collect(Collectors.joining("\n")));
        resList.add(propReprs.stream().collect(Collectors.joining("\n\n")));
        
        for (var prop : cls.properties) {
            var prefix = this.vis(prop.getVisibility()) + " " + this.preIf("static ", prop.getIsStatic());
            if (prop.getter != null)
                resList.add(prefix + this.type(prop.getType(), true, false) + " get" + this.ucFirst(prop.getName()) + "()" + this.block(prop.getter, false));
            
            if (prop.setter != null)
                resList.add(prefix + "void set" + this.ucFirst(prop.getName()) + "(" + this.type(prop.getType(), true, false) + " value)" + this.block(prop.setter, false));
        }
        
        if (staticConstructorStmts.size() > 0)
            resList.add("static {\n" + this.pad(this.stmts(staticConstructorStmts.toArray(Statement[]::new))) + "\n}");
        
        if (cls.constructor_ != null) {
            var constrFieldInits = new ArrayList<Statement>();
            for (var field : Arrays.stream(cls.getFields()).filter(x -> x.constructorParam != null).toArray(Field[]::new)) {
                var fieldRef = new InstanceFieldReference(new ThisReference(cls), field);
                var mpRef = new MethodParameterReference(field.constructorParam);
                // TODO: decide what to do with "after-TypeEngine" transformations
                mpRef.setActualType(field.getType(), false, false);
                constrFieldInits.add(new ExpressionStatement(new BinaryExpression(fieldRef, "=", mpRef)));
            }
            
            var superCall = cls.constructor_.superCallArgs != null ? "super(" + Arrays.stream(Arrays.stream(cls.constructor_.superCallArgs).map(x -> this.expr(x)).toArray(String[]::new)).collect(Collectors.joining(", ")) + ");\n" : "";
            
            // TODO: super calls
            resList.add(this.methodGen("public " + this.preIf("/* throws */ ", cls.constructor_.getThrows()) + this.name_(cls.getName()), cls.constructor_.getParameters(), "\n{\n" + this.pad(superCall + this.stmts(Stream.of(Stream.of(constrFieldInits, complexFieldInits).flatMap(Stream::of).toArray(Statement[]::new), cls.constructor_.getBody().statements).flatMap(Stream::of).toArray(Statement[]::new))) + "\n}"));
        }
        else if (complexFieldInits.size() > 0)
            resList.add("public " + this.name_(cls.getName()) + "()\n{\n" + this.pad(this.stmts(complexFieldInits.toArray(Statement[]::new))) + "\n}");
        
        var methods = new ArrayList<String>();
        for (var method : cls.getMethods()) {
            if (method.getBody() == null)
                continue;
            // declaration only
            methods.add(this.method(method, true));
        }
        resList.add(methods.stream().collect(Collectors.joining("\n\n")));
        return this.pad(Arrays.stream(resList.stream().filter(x -> !Objects.equals(x, "")).toArray(String[]::new)).collect(Collectors.joining("\n\n")));
    }
    
    public String ucFirst(String str) {
        return str.substring(0, 0 + 1).toUpperCase() + str.substring(1);
    }
    
    public String interface_(Interface intf) {
        this.currentClass = intf;
        
        var resList = new ArrayList<String>();
        for (var field : intf.getFields()) {
            var varType = this.varType(field, field);
            var name = this.ucFirst(field.getName());
            resList.add(varType + " get" + name + "();\nvoid set" + name + "(" + varType + " value);");
        }
        
        resList.add(Arrays.stream(Arrays.stream(intf.getMethods()).map(method -> this.method(method, false)).toArray(String[]::new)).collect(Collectors.joining("\n")));
        return this.pad(Arrays.stream(resList.stream().filter(x -> !Objects.equals(x, "")).toArray(String[]::new)).collect(Collectors.joining("\n\n")));
    }
    
    public String pad(String str) {
        return Arrays.stream(Arrays.stream(str.split("\\n", -1)).map(x -> "    " + x).toArray(String[]::new)).collect(Collectors.joining("\n"));
    }
    
    public String pathToNs(String path) {
        // Generator/ExprLang/ExprLangAst.ts -> Generator.ExprLang
        var parts = new ArrayList<>(Arrays.asList(path.split("/", -1)));
        parts.remove(parts.size() - 1);
        return parts.stream().collect(Collectors.joining("."));
    }
    
    public String importsHead() {
        var imports = new ArrayList<String>();
        for (var imp : this.imports.toArray(String[]::new))
            imports.add(imp);
        this.imports = new LinkedHashSet<String>();
        return imports.size() == 0 ? "" : Arrays.stream(imports.stream().map(x -> "import " + x + ";").toArray(String[]::new)).collect(Collectors.joining("\n")) + "\n\n";
    }
    
    public String toImport(ExportScopeRef scope) {
        // TODO: hack
        if (Objects.equals(scope.scopeName, "index"))
            return "io.onelang.std." + scope.packageName.split("-", -1)[0].replaceAll("One\\.", "").toLowerCase();
        return scope.packageName + "." + scope.scopeName.replaceAll("/", ".");
    }
    
    public GeneratedFile[] generate(Package pkg) {
        var result = new ArrayList<GeneratedFile>();
        for (var path : pkg.files.keySet().toArray(String[]::new)) {
            var file = pkg.files.get(path);
            var packagePath = pkg.name + "/" + file.sourcePath.path;
            var dstDir = "src/main/java/" + packagePath;
            var packageName = packagePath.replaceAll("/", ".");
            
            var imports = new LinkedHashSet<String>();
            for (var impList : file.imports) {
                var impPkg = this.toImport(impList.exportScope);
                for (var imp : impList.imports)
                    imports.add(impPkg + "." + imp.getName());
            }
            
            var head = "package " + packageName + ";\n\n" + Arrays.stream(Arrays.stream(imports.toArray(String[]::new)).map(x -> "import " + x + ";").toArray(String[]::new)).collect(Collectors.joining("\n")) + "\n\n";
            
            for (var enum_ : file.enums)
                result.add(new GeneratedFile(dstDir + "/" + enum_.getName() + ".java", head + "public enum " + this.name_(enum_.getName()) + " { " + Arrays.stream(Arrays.stream(enum_.values).map(x -> this.name_(x.name)).toArray(String[]::new)).collect(Collectors.joining(", ")) + " }"));
            
            for (var intf : file.interfaces) {
                var res = "public interface " + this.name_(intf.getName()) + this.typeArgs(intf.getTypeArguments()) + this.preArr(" extends ", Arrays.stream(intf.getBaseInterfaces()).map(x -> this.type(x, true, false)).toArray(String[]::new)) + " {\n" + this.interface_(intf) + "\n}";
                result.add(new GeneratedFile(dstDir + "/" + intf.getName() + ".java", head + this.importsHead() + res));
            }
            
            for (var cls : file.classes) {
                var res = "public class " + this.name_(cls.getName()) + this.typeArgs(cls.getTypeArguments()) + (cls.baseClass != null ? " extends " + this.type(cls.baseClass, true, false) : "") + this.preArr(" implements ", Arrays.stream(cls.getBaseInterfaces()).map(x -> this.type(x, true, false)).toArray(String[]::new)) + " {\n" + this.class_(cls) + "\n}";
                result.add(new GeneratedFile(dstDir + "/" + cls.getName() + ".java", head + this.importsHead() + res));
            }
        }
        return result.toArray(GeneratedFile[]::new);
    }
}