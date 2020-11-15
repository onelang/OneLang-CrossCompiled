package OneLang.Generator.PhpGenerator;

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
import OneLang.One.Ast.Types.Enum;
import OneLang.One.Ast.AstTypes.VoidType;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.InterfaceType;
import OneLang.One.Ast.AstTypes.EnumType;
import OneLang.One.Ast.AstTypes.AnyType;
import OneLang.One.Ast.AstTypes.LambdaType;
import OneLang.One.Ast.AstTypes.NullType;
import OneLang.One.Ast.AstTypes.GenericsType;
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
import OneLang.Generator.PhpPlugins.JsToPhp.JsToPhp;
import OneLang.One.ITransformer.ITransformer;

import OneLang.Generator.IGenerator.IGenerator;
import java.util.Set;
import OneLang.One.Ast.Types.IInterface;
import java.util.List;
import OneLang.Generator.IGeneratorPlugin.IGeneratorPlugin;
import java.util.ArrayList;
import OneLang.Generator.PhpPlugins.JsToPhp.JsToPhp;
import OneLang.One.ITransformer.ITransformer;
import java.util.Arrays;
import OneStd.RegExp;
import java.util.stream.Collectors;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.AstTypes.ClassType;
import OneStd.Objects;
import OneLang.One.Ast.AstTypes.InterfaceType;
import OneLang.One.Ast.AstTypes.VoidType;
import OneLang.One.Ast.AstTypes.EnumType;
import OneLang.One.Ast.AstTypes.AnyType;
import OneLang.One.Ast.AstTypes.NullType;
import OneLang.One.Ast.AstTypes.GenericsType;
import OneLang.One.Ast.AstTypes.LambdaType;
import OneLang.One.Ast.Types.IVariable;
import OneLang.One.Ast.Types.IHasAttributesAndTrivia;
import OneLang.One.Ast.Types.IVariableWithInitializer;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.References.VariableReference;
import OneLang.One.Ast.Types.MethodParameter;
import OneLang.One.Ast.Expressions.IMethodCallExpression;
import OneStd.StdArrayHelper;
import OneLang.One.Ast.Expressions.NewExpression;
import OneLang.One.Ast.Expressions.UnresolvedNewExpression;
import OneLang.One.Ast.Expressions.Identifier;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Ast.Expressions.UnresolvedCallExpression;
import OneLang.One.Ast.Expressions.UnresolvedMethodCallExpression;
import OneLang.One.Ast.Expressions.InstanceMethodCallExpression;
import OneLang.One.Ast.References.SuperReference;
import OneLang.One.Ast.Expressions.StaticMethodCallExpression;
import OneLang.One.Ast.Expressions.GlobalFunctionCallExpression;
import OneLang.One.Ast.Expressions.LambdaCallExpression;
import OneLang.One.Ast.Expressions.BooleanLiteral;
import OneLang.One.Ast.Expressions.StringLiteral;
import OneStd.JSON;
import java.util.regex.Pattern;
import OneLang.One.Ast.Expressions.NumericLiteral;
import OneLang.One.Ast.Expressions.CharacterLiteral;
import OneLang.One.Ast.Expressions.ElementAccessExpression;
import OneLang.One.Ast.Expressions.TemplateString;
import OneLang.One.Ast.Expressions.ConditionalExpression;
import OneLang.One.Ast.Expressions.BinaryExpression;
import OneLang.One.Ast.Expressions.ArrayLiteral;
import OneLang.One.Ast.Expressions.CastExpression;
import OneLang.One.Ast.Expressions.InstanceOfExpression;
import OneLang.One.Ast.Expressions.ParenthesizedExpression;
import OneLang.One.Ast.Expressions.RegexLiteral;
import OneLang.One.Ast.Types.Lambda;
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
import OneLang.One.Ast.References.StaticFieldReference;
import OneLang.One.Ast.References.StaticPropertyReference;
import OneLang.One.Ast.References.InstanceFieldReference;
import OneLang.One.Ast.References.InstancePropertyReference;
import OneLang.One.Ast.References.EnumMemberReference;
import OneLang.One.Ast.Expressions.NullCoalesceExpression;
import OneLang.One.Ast.Interfaces.IExpression;
import OneLang.One.Ast.Statements.IfStatement;
import OneLang.One.Ast.Statements.Block;
import OneLang.One.Ast.Statements.BreakStatement;
import OneLang.One.Ast.Statements.ReturnStatement;
import OneLang.One.Ast.Statements.UnsetStatement;
import OneLang.One.Ast.Statements.ThrowStatement;
import OneLang.One.Ast.Statements.ExpressionStatement;
import OneLang.One.Ast.Statements.VariableDeclaration;
import OneLang.One.Ast.Statements.ForeachStatement;
import OneLang.One.Ast.Statements.WhileStatement;
import OneLang.One.Ast.Statements.ForStatement;
import OneLang.One.Ast.Statements.DoStatement;
import OneLang.One.Ast.Statements.TryStatement;
import OneLang.One.Ast.Statements.ContinueStatement;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.Field;
import java.util.stream.Stream;
import OneLang.One.Ast.Types.Interface;
import OneLang.One.Ast.Types.Enum;
import java.util.LinkedHashSet;
import OneLang.One.Ast.Types.SourceFile;
import OneLang.Generator.GeneratedFile.GeneratedFile;
import OneLang.One.Ast.Types.Package;

public class PhpGenerator implements IGenerator {
    public Set<String> usings;
    public IInterface currentClass;
    public String[] reservedWords;
    public String[] fieldToMethodHack;
    public List<IGeneratorPlugin> plugins;
    
    public PhpGenerator()
    {
        this.reservedWords = new String[] { "Generator", "Array", "List", "Interface", "Class" };
        this.fieldToMethodHack = new String[] { "length" };
        this.plugins = new ArrayList<IGeneratorPlugin>();
        this.plugins.add(new JsToPhp(this));
    }
    
    public String getLangName() {
        return "PHP";
    }
    
    public String getExtension() {
        return "php";
    }
    
    public ITransformer[] getTransforms() {
        return new ITransformer[0];
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
        return this.typeArgs(Arrays.stream(args).map(x -> this.type(x, true)).toArray(String[]::new));
    }
    
    public String type(IType t, Boolean mutates) {
        if (t instanceof ClassType) {
            //const typeArgs = this.typeArgs(t.typeArguments.map(x => this.type(x)));
            if (Objects.equals(((ClassType)t).decl.getName(), "TsString"))
                return "string";
            else if (Objects.equals(((ClassType)t).decl.getName(), "TsBoolean"))
                return "bool";
            else if (Objects.equals(((ClassType)t).decl.getName(), "TsNumber"))
                return "int";
            else if (Objects.equals(((ClassType)t).decl.getName(), "TsArray")) {
                if (mutates)
                    return "List_";
                else
                    return this.type(((ClassType)t).getTypeArguments()[0], true) + "[]";
            }
            else if (Objects.equals(((ClassType)t).decl.getName(), "Promise"))
                return this.type(((ClassType)t).getTypeArguments()[0], true);
            else if (Objects.equals(((ClassType)t).decl.getName(), "Object"))
                //this.usings.add("System");
                return "object";
            else if (Objects.equals(((ClassType)t).decl.getName(), "TsMap"))
                return "Dictionary";
            
            if (((ClassType)t).decl.getParentFile().exportScope == null)
                return "\\OneLang\\" + this.name_(((ClassType)t).decl.getName());
            else
                return this.name_(((ClassType)t).decl.getName());
        }
        else if (t instanceof InterfaceType)
            return this.name_(((InterfaceType)t).decl.getName()) + this.typeArgs(Arrays.stream(((InterfaceType)t).getTypeArguments()).map(x -> this.type(x, true)).toArray(String[]::new));
        else if (t instanceof VoidType)
            return "void";
        else if (t instanceof EnumType)
            return this.name_(((EnumType)t).decl.getName());
        else if (t instanceof AnyType)
            return "object";
        else if (t instanceof NullType)
            return "null";
        else if (t instanceof GenericsType)
            return ((GenericsType)t).typeVarName;
        else if (t instanceof LambdaType) {
            var isFunc = !(((LambdaType)t).returnType instanceof VoidType);
            var paramTypes = new ArrayList<>(Arrays.asList(Arrays.stream(((LambdaType)t).parameters).map(x -> this.type(x.getType(), true)).toArray(String[]::new)));
            if (isFunc)
                paramTypes.add(this.type(((LambdaType)t).returnType, true));
            return (isFunc ? "Func" : "Action") + "<" + paramTypes.stream().collect(Collectors.joining(", ")) + ">";
        }
        else if (t == null)
            return "/* TODO */ object";
        else
            return "/* MISSING */";
    }
    
    public Boolean isTsArray(IType type) {
        return type instanceof ClassType && Objects.equals(((ClassType)type).decl.getName(), "TsArray");
    }
    
    public String vis(Visibility v, Boolean isProperty) {
        return v == Visibility.Private ? "private " : v == Visibility.Protected ? "protected " : v == Visibility.Public ? (isProperty ? "public " : "") : "/* TODO: not set */" + (isProperty ? "public " : "");
    }
    
    public String varWoInit(IVariable v, IHasAttributesAndTrivia attr) {
        // let type: string;
        // if (attr !== null && attr.attributes !== null && "php-type" in attr.attributes)
        //     type = attr.attributes["php-type"];
        // else if (v.type instanceof ClassType && v.type.decl.name === "TsArray") {
        //     if (v.mutability.mutated) {
        //         type = `List<${this.type(v.type.typeArguments[0])}>`;
        //     } else {
        //         type = `${this.type(v.type.typeArguments[0])}[]`;
        //     }
        // } else {
        //     type = this.type(v.type);
        // }
        return "$" + this.name_(v.getName());
    }
    
    public String var(IVariableWithInitializer v, IHasAttributesAndTrivia attrs) {
        return this.varWoInit(v, attrs) + (v.getInitializer() != null ? " = " + this.expr(v.getInitializer()) : "");
    }
    
    public String exprCall(IType[] typeArgs, Expression[] args) {
        return this.typeArgs2(typeArgs) + "(" + Arrays.stream(Arrays.stream(args).map(x -> this.expr(x)).toArray(String[]::new)).collect(Collectors.joining(", ")) + ")";
    }
    
    public String mutateArg(Expression arg, Boolean shouldBeMutable) {
        // if (this.isTsArray(arg.actualType)) {
        //     if (arg instanceof ArrayLiteral && !shouldBeMutable) {
        //         return `Array(${arg.items.map(x => this.expr(x)).join(', ')})`;
        //     }
        
        //     let currentlyMutable = shouldBeMutable;
        //     if (arg instanceof VariableReference)
        //         currentlyMutable = arg.getVariable().mutability.mutated;
        //     else if (arg instanceof InstanceMethodCallExpression || arg instanceof StaticMethodCallExpression)
        //         currentlyMutable = false;
            
        //     if (currentlyMutable && !shouldBeMutable)
        //         return `${this.expr(arg)}.ToArray()`;
        //     else if (!currentlyMutable && shouldBeMutable) {
        //         return `${this.expr(arg)}.ToList()`;
        //     }
        // }
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
        return this.name_(expr.getMethod().name) + this.typeArgs2(expr.getTypeArgs()) + this.callParams(expr.getArgs(), expr.getMethod().getParameters());
    }
    
    public String inferExprNameForType(IType type) {
        if (type instanceof ClassType && StdArrayHelper.allMatch(((ClassType)type).getTypeArguments(), (x, unused) -> x instanceof ClassType)) {
            var fullName = Arrays.stream(Arrays.stream(((ClassType)type).getTypeArguments()).map(x -> (((ClassType)x)).decl.getName()).toArray(String[]::new)).collect(Collectors.joining("")) + ((ClassType)type).decl.getName();
            return NameUtils.shortName(fullName);
        }
        return null;
    }
    
    public String expr(IExpression expr) {
        for (var plugin : this.plugins) {
            var result = plugin.expr(expr);
            if (result != null)
                return result;
        }
        
        var res = "UNKNOWN-EXPR";
        if (expr instanceof NewExpression)
            res = "new " + this.type(((NewExpression)expr).cls, true) + this.callParams(((NewExpression)expr).args, ((NewExpression)expr).cls.decl.constructor_ != null ? ((NewExpression)expr).cls.decl.constructor_.getParameters() : new MethodParameter[0]);
        else if (expr instanceof UnresolvedNewExpression)
            res = "/* TODO: UnresolvedNewExpression */ new " + this.type(((UnresolvedNewExpression)expr).cls, true) + "(" + Arrays.stream(Arrays.stream(((UnresolvedNewExpression)expr).args).map(x -> this.expr(x)).toArray(String[]::new)).collect(Collectors.joining(", ")) + ")";
        else if (expr instanceof Identifier)
            res = "/* TODO: Identifier */ " + ((Identifier)expr).text;
        else if (expr instanceof PropertyAccessExpression)
            res = "/* TODO: PropertyAccessExpression */ " + this.expr(((PropertyAccessExpression)expr).object) + "." + ((PropertyAccessExpression)expr).propertyName;
        else if (expr instanceof UnresolvedCallExpression)
            res = "/* TODO: UnresolvedCallExpression */ " + this.expr(((UnresolvedCallExpression)expr).func) + this.exprCall(((UnresolvedCallExpression)expr).typeArgs, ((UnresolvedCallExpression)expr).args);
        else if (expr instanceof UnresolvedMethodCallExpression)
            res = "/* TODO: UnresolvedMethodCallExpression */ " + this.expr(((UnresolvedMethodCallExpression)expr).object) + "->" + ((UnresolvedMethodCallExpression)expr).methodName + this.exprCall(((UnresolvedMethodCallExpression)expr).typeArgs, ((UnresolvedMethodCallExpression)expr).args);
        else if (expr instanceof InstanceMethodCallExpression) {
            if (((InstanceMethodCallExpression)expr).object instanceof SuperReference)
                res = "parent::" + this.methodCall(((InstanceMethodCallExpression)expr));
            else if (((InstanceMethodCallExpression)expr).object instanceof NewExpression)
                res = "(" + this.expr(((NewExpression)((InstanceMethodCallExpression)expr).object)) + ")->" + this.methodCall(((InstanceMethodCallExpression)expr));
            else
                res = this.expr(((InstanceMethodCallExpression)expr).object) + "->" + this.methodCall(((InstanceMethodCallExpression)expr));
        }
        else if (expr instanceof StaticMethodCallExpression) {
            res = this.name_(((StaticMethodCallExpression)expr).getMethod().parentInterface.getName()) + "::" + this.methodCall(((StaticMethodCallExpression)expr));
            if (((StaticMethodCallExpression)expr).getMethod().parentInterface.getParentFile().exportScope == null)
                res = "\\OneLang\\" + res;
        }
        else if (expr instanceof GlobalFunctionCallExpression)
            res = "Global." + this.name_(((GlobalFunctionCallExpression)expr).func.getName()) + this.exprCall(new IType[0], ((GlobalFunctionCallExpression)expr).args);
        else if (expr instanceof LambdaCallExpression)
            res = this.expr(((LambdaCallExpression)expr).method) + "(" + Arrays.stream(Arrays.stream(((LambdaCallExpression)expr).args).map(x -> this.expr(x)).toArray(String[]::new)).collect(Collectors.joining(", ")) + ")";
        else if (expr instanceof BooleanLiteral)
            res = (((BooleanLiteral)expr).boolValue ? "true" : "false");
        else if (expr instanceof StringLiteral)
            res = JSON.stringify(((StringLiteral)expr).stringValue).replaceAll("\\$", "\\$");
        else if (expr instanceof NumericLiteral)
            res = ((NumericLiteral)expr).valueAsText;
        else if (expr instanceof CharacterLiteral)
            res = "'" + ((CharacterLiteral)expr).charValue + "'";
        else if (expr instanceof ElementAccessExpression)
            res = this.expr(((ElementAccessExpression)expr).object) + "[" + this.expr(((ElementAccessExpression)expr).elementExpr) + "]";
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
            res = parts.stream().collect(Collectors.joining(" . "));
        }
        else if (expr instanceof BinaryExpression) {
            var op = ((BinaryExpression)expr).operator;
            if (Objects.equals(op, "=="))
                op = "===";
            else if (Objects.equals(op, "!="))
                op = "!==";
            
            if (((BinaryExpression)expr).left.actualType != null && Objects.equals(((BinaryExpression)expr).left.actualType.repr(), "C:TsString")) {
                if (Objects.equals(op, "+"))
                    op = ".";
                else if (Objects.equals(op, "+="))
                    op = ".=";
            }
            
            // const useParen = expr.left instanceof BinaryExpression && expr.left.operator !== expr.operator;
            // const leftExpr = this.expr(expr.left);
            
            res = this.expr(((BinaryExpression)expr).left) + " " + op + " " + this.mutatedExpr(((BinaryExpression)expr).right, Objects.equals(((BinaryExpression)expr).operator, "=") ? ((BinaryExpression)expr).left : null);
        }
        else if (expr instanceof ArrayLiteral)
            res = "array(" + Arrays.stream(Arrays.stream(((ArrayLiteral)expr).items).map(x -> this.expr(x)).toArray(String[]::new)).collect(Collectors.joining(", ")) + ")";
        else if (expr instanceof CastExpression)
            res = this.expr(((CastExpression)expr).expression);
        else if (expr instanceof ConditionalExpression) {
            var whenFalseExpr = this.expr(((ConditionalExpression)expr).whenFalse);
            if (((ConditionalExpression)expr).whenFalse instanceof ConditionalExpression)
                whenFalseExpr = "(" + whenFalseExpr + ")";
            res = this.expr(((ConditionalExpression)expr).condition) + " ? " + this.expr(((ConditionalExpression)expr).whenTrue) + " : " + whenFalseExpr;
        }
        else if (expr instanceof InstanceOfExpression)
            res = this.expr(((InstanceOfExpression)expr).expr) + " instanceof " + this.type(((InstanceOfExpression)expr).checkType, true);
        else if (expr instanceof ParenthesizedExpression)
            res = "(" + this.expr(((ParenthesizedExpression)expr).expression) + ")";
        else if (expr instanceof RegexLiteral)
            res = "new \\OneLang\\RegExp(" + JSON.stringify(((RegexLiteral)expr).pattern) + ")";
        else if (expr instanceof Lambda) {
            var params = Arrays.stream(((Lambda)expr).getParameters()).map(x -> "$" + this.name_(x.getName())).toArray(String[]::new);
            // TODO: captures should not be null
            var uses = ((Lambda)expr).captures != null && ((Lambda)expr).captures.size() > 0 ? " use (" + Arrays.stream(((Lambda)expr).captures.stream().map(x -> "$" + x.getName()).toArray(String[]::new)).collect(Collectors.joining(", ")) + ")" : "";
            res = "function (" + Arrays.stream(params).collect(Collectors.joining(", ")) + ")" + uses + " { " + this.rawBlock(((Lambda)expr).getBody()) + " }";
        }
        else if (expr instanceof UnaryExpression && ((UnaryExpression)expr).unaryType == UnaryType.Prefix)
            res = ((UnaryExpression)expr).operator + this.expr(((UnaryExpression)expr).operand);
        else if (expr instanceof UnaryExpression && ((UnaryExpression)expr).unaryType == UnaryType.Postfix)
            res = this.expr(((UnaryExpression)expr).operand) + ((UnaryExpression)expr).operator;
        else if (expr instanceof MapLiteral) {
            var repr = Arrays.stream(Arrays.stream(((MapLiteral)expr).items).map(item -> JSON.stringify(item.key) + " => " + this.expr(item.value)).toArray(String[]::new)).collect(Collectors.joining(",\n"));
            res = "Array(" + (Objects.equals(repr, "") ? "" : repr.contains("\n") ? "\n" + this.pad(repr) + "\n" : "(" + repr) + ")";
        }
        else if (expr instanceof NullLiteral)
            res = "null";
        else if (expr instanceof AwaitExpression)
            res = this.expr(((AwaitExpression)expr).expr);
        else if (expr instanceof ThisReference)
            res = "$this";
        else if (expr instanceof StaticThisReference)
            res = this.currentClass.getName();
        else if (expr instanceof EnumReference)
            res = this.name_(((EnumReference)expr).decl.getName());
        else if (expr instanceof ClassReference)
            res = this.name_(((ClassReference)expr).decl.getName());
        else if (expr instanceof MethodParameterReference)
            res = "$" + this.name_(((MethodParameterReference)expr).decl.getName());
        else if (expr instanceof VariableDeclarationReference)
            res = "$" + this.name_(((VariableDeclarationReference)expr).decl.getName());
        else if (expr instanceof ForVariableReference)
            res = "$" + this.name_(((ForVariableReference)expr).decl.getName());
        else if (expr instanceof ForeachVariableReference)
            res = "$" + this.name_(((ForeachVariableReference)expr).decl.getName());
        else if (expr instanceof CatchVariableReference)
            res = "$" + this.name_(((CatchVariableReference)expr).decl.getName());
        else if (expr instanceof GlobalFunctionReference)
            res = this.name_(((GlobalFunctionReference)expr).decl.getName());
        else if (expr instanceof SuperReference)
            res = "parent";
        else if (expr instanceof StaticFieldReference)
            res = this.name_(((StaticFieldReference)expr).decl.parentInterface.getName()) + "::$" + this.name_(((StaticFieldReference)expr).decl.getName());
        else if (expr instanceof StaticPropertyReference)
            res = this.name_(((StaticPropertyReference)expr).decl.parentClass.getName()) + "::get_" + this.name_(((StaticPropertyReference)expr).decl.getName()) + "()";
        else if (expr instanceof InstanceFieldReference)
            res = this.expr(((InstanceFieldReference)expr).object) + "->" + this.name_(((InstanceFieldReference)expr).field.getName());
        else if (expr instanceof InstancePropertyReference)
            res = this.expr(((InstancePropertyReference)expr).object) + "->get_" + this.name_(((InstancePropertyReference)expr).property.getName()) + "()";
        else if (expr instanceof EnumMemberReference)
            res = this.name_(((EnumMemberReference)expr).decl.parentEnum.getName()) + "::" + this.enumMemberName(((EnumMemberReference)expr).decl.name);
        else if (expr instanceof NullCoalesceExpression)
            res = this.expr(((NullCoalesceExpression)expr).defaultExpr) + " ?? " + this.mutatedExpr(((NullCoalesceExpression)expr).exprIfNull, ((NullCoalesceExpression)expr).defaultExpr);
        else { }
        return res;
    }
    
    public String block(Block block, Boolean allowOneLiner) {
        var stmtLen = block.statements.size();
        return stmtLen == 0 ? " { }" : allowOneLiner && stmtLen == 1 && !(block.statements.get(0) instanceof IfStatement) ? "\n" + this.pad(this.rawBlock(block)) : " {\n" + this.pad(this.rawBlock(block)) + "\n}";
    }
    
    public String stmtDefault(Statement stmt) {
        var res = "UNKNOWN-STATEMENT";
        if (stmt.getAttributes() != null && stmt.getAttributes().containsKey("csharp"))
            res = stmt.getAttributes().get("csharp");
        else if (stmt instanceof BreakStatement)
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
                res = "$" + this.name_(((VariableDeclaration)stmt).getName()) + " = null;";
            else if (((VariableDeclaration)stmt).getInitializer() != null)
                res = "$" + this.name_(((VariableDeclaration)stmt).getName()) + " = " + this.mutateArg(((VariableDeclaration)stmt).getInitializer(), ((VariableDeclaration)stmt).getMutability().mutated) + ";";
            else
                res = "/* @var $" + this.name_(((VariableDeclaration)stmt).getName()) + " */";
        }
        else if (stmt instanceof ForeachStatement)
            res = "foreach (" + this.expr(((ForeachStatement)stmt).items) + " as $" + this.name_(((ForeachStatement)stmt).itemVar.getName()) + ")" + this.block(((ForeachStatement)stmt).body, true);
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
                //                this.usings.add("System");
                res += " catch (Exception $" + this.name_(((TryStatement)stmt).catchVar.getName()) + ")" + this.block(((TryStatement)stmt).catchBody, false);
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
        
        if (stmt.getAttributes() != null && stmt.getAttributes().containsKey("php"))
            res = stmt.getAttributes().get("php");
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
    
    public String classLike(IInterface cls) {
        this.currentClass = cls;
        var resList = new ArrayList<String>();
        
        var staticConstructorStmts = new ArrayList<Statement>();
        var complexFieldInits = new ArrayList<Statement>();
        if (cls instanceof Class) {
            var fieldReprs = new ArrayList<String>();
            for (var field : ((Class)cls).getFields()) {
                var isInitializerComplex = field.getInitializer() != null && !(field.getInitializer() instanceof StringLiteral) && !(field.getInitializer() instanceof BooleanLiteral) && !(field.getInitializer() instanceof NumericLiteral);
                
                var prefix = this.vis(field.getVisibility(), true) + this.preIf("static ", field.getIsStatic());
                if (field.interfaceDeclarations.length > 0)
                    fieldReprs.add(prefix + this.varWoInit(field, field) + ";");
                else if (isInitializerComplex) {
                    if (field.getIsStatic())
                        staticConstructorStmts.add(new ExpressionStatement(new BinaryExpression(new StaticFieldReference(field), "=", field.getInitializer())));
                    else
                        complexFieldInits.add(new ExpressionStatement(new BinaryExpression(new InstanceFieldReference(new ThisReference(((Class)cls)), field), "=", field.getInitializer())));
                    
                    fieldReprs.add(prefix + this.varWoInit(field, field) + ";");
                }
                else
                    fieldReprs.add(prefix + this.var(field, field) + ";");
            }
            resList.add(fieldReprs.stream().collect(Collectors.joining("\n")));
            
            for (var prop : ((Class)cls).properties) {
                if (prop.getter != null)
                    resList.add(this.vis(prop.getVisibility(), false) + this.preIf("static ", prop.getIsStatic()) + "function get_" + this.name_(prop.getName()) + "()" + this.block(prop.getter, false));
                if (prop.setter != null)
                    resList.add(this.vis(prop.getVisibility(), false) + this.preIf("static ", prop.getIsStatic()) + "function set_" + this.name_(prop.getName()) + "($value)" + this.block(prop.setter, false));
            }
            
            if (staticConstructorStmts.size() > 0)
                resList.add("static function StaticInit()\n{\n" + this.pad(this.stmts(staticConstructorStmts.toArray(Statement[]::new))) + "\n}");
            
            if (((Class)cls).constructor_ != null) {
                var constrFieldInits = new ArrayList<Statement>();
                for (var field : Arrays.stream(((Class)cls).getFields()).filter(x -> x.constructorParam != null).toArray(Field[]::new)) {
                    var fieldRef = new InstanceFieldReference(new ThisReference(((Class)cls)), field);
                    var mpRef = new MethodParameterReference(field.constructorParam);
                    // TODO: decide what to do with "after-TypeEngine" transformations
                    mpRef.setActualType(field.getType(), false, false);
                    constrFieldInits.add(new ExpressionStatement(new BinaryExpression(fieldRef, "=", mpRef)));
                }
                
                var parentCall = ((Class)cls).constructor_.superCallArgs != null ? "parent::__construct(" + Arrays.stream(Arrays.stream(((Class)cls).constructor_.superCallArgs).map(x -> this.expr(x)).toArray(String[]::new)).collect(Collectors.joining(", ")) + ");\n" : "";
                
                resList.add(this.preIf("/* throws */ ", ((Class)cls).constructor_.getThrows()) + "function __construct" + "(" + Arrays.stream(Arrays.stream(((Class)cls).constructor_.getParameters()).map(p -> this.var(p, p)).toArray(String[]::new)).collect(Collectors.joining(", ")) + ")" + " {\n" + this.pad(parentCall + this.stmts(Stream.of(Stream.of(constrFieldInits, complexFieldInits).flatMap(Stream::of).toArray(Statement[]::new), ((Class)cls).constructor_.getBody().statements).flatMap(Stream::of).toArray(Statement[]::new))) + "\n}");
            }
            else if (complexFieldInits.size() > 0)
                resList.add("function __construct()\n{\n" + this.pad(this.stmts(complexFieldInits.toArray(Statement[]::new))) + "\n}");
        }
        else if (cls instanceof Interface) { }
        
        var methods = new ArrayList<String>();
        for (var method : cls.getMethods()) {
            if (cls instanceof Class && method.getBody() == null)
                continue;
            // declaration only
            methods.add((method.parentInterface instanceof Interface ? "" : this.vis(method.getVisibility(), false)) + this.preIf("static ", method.getIsStatic()) + this.preIf("/* throws */ ", method.getThrows()) + "function " + this.name_(method.name) + this.typeArgs(method.typeArguments) + "(" + Arrays.stream(Arrays.stream(method.getParameters()).map(p -> this.var(p, null)).toArray(String[]::new)).collect(Collectors.joining(", ")) + ")" + (method.getBody() != null ? " {\n" + this.pad(this.stmts(method.getBody().statements.toArray(Statement[]::new))) + "\n}" : ";"));
        }
        resList.add(methods.stream().collect(Collectors.joining("\n\n")));
        return " {\n" + this.pad(Arrays.stream(resList.stream().filter(x -> !Objects.equals(x, "")).toArray(String[]::new)).collect(Collectors.joining("\n\n"))) + "\n}" + (staticConstructorStmts.size() > 0 ? "\n" + this.name_(cls.getName()) + "::StaticInit();" : "");
    }
    
    public String pad(String str) {
        return Arrays.stream(Arrays.stream(str.split("\\n", -1)).map(x -> "    " + x).toArray(String[]::new)).collect(Collectors.joining("\n"));
    }
    
    public String pathToNs(String path) {
        // Generator/ExprLang/ExprLangAst.ts -> Generator\ExprLang\ExprLangAst
        var parts = path.replaceAll("\\.ts", "").split("/", -1);
        //parts.pop();
        return Arrays.stream(parts).collect(Collectors.joining("\\"));
    }
    
    public String enumName(Enum enum_, Boolean isDecl) {
        return enum_.getName();
    }
    
    public String enumMemberName(String name) {
        return this.name_(name).toUpperCase();
    }
    
    public String genFile(SourceFile sourceFile) {
        this.usings = new LinkedHashSet<String>();
        
        var enums = new ArrayList<String>();
        for (var enum_ : sourceFile.enums) {
            var values = new ArrayList<String>();
            for (Integer i = 0; i < enum_.values.length; i++)
                values.add("const " + this.enumMemberName(enum_.values[i].name) + " = " + i + 1 + ";");
            enums.add("class " + this.enumName(enum_, true) + " {\n" + this.pad(values.stream().collect(Collectors.joining("\n"))) + "\n}");
        }
        
        var intfs = Arrays.stream(sourceFile.interfaces).map(intf -> "interface " + this.name_(intf.getName()) + this.typeArgs(intf.getTypeArguments()) + this.preArr(" extends ", Arrays.stream(intf.getBaseInterfaces()).map(x -> this.type(x, true)).toArray(String[]::new)) + this.classLike(intf)).toArray(String[]::new);
        
        var classes = new ArrayList<String>();
        for (var cls : sourceFile.classes)
            classes.add("class " + this.name_(cls.getName()) + (cls.baseClass != null ? " extends " + this.type(cls.baseClass, true) : "") + this.preArr(" implements ", Arrays.stream(cls.getBaseInterfaces()).map(x -> this.type(x, true)).toArray(String[]::new)) + this.classLike(cls));
        
        var main = this.rawBlock(sourceFile.mainBlock);
        
        var usingsSet = new LinkedHashSet<String>();
        for (var imp : sourceFile.imports) {
            if (imp.getAttributes().containsKey("php-use"))
                usingsSet.add(imp.getAttributes().get("php-use"));
            else {
                var fileNs = this.pathToNs(imp.exportScope.scopeName);
                if (Objects.equals(fileNs, "index"))
                    continue;
                for (var impItem : imp.imports)
                    usingsSet.add(fileNs + "\\" + this.name_(impItem.getName()));
            }
        }
        
        for (var using : this.usings)
            usingsSet.add(using);
        
        var usings = new ArrayList<String>();
        for (var using : usingsSet)
            usings.add("use " + using + ";");
        
        var result = Arrays.stream(new ArrayList<>(List.of(usings.stream().collect(Collectors.joining("\n")), enums.stream().collect(Collectors.joining("\n")), Arrays.stream(intfs).collect(Collectors.joining("\n\n")), classes.stream().collect(Collectors.joining("\n\n")), main)).stream().filter(x -> !Objects.equals(x, "")).toArray(String[]::new)).collect(Collectors.joining("\n\n"));
        var nl = "\n";
        result = "<?php\n\nnamespace " + this.pathToNs(sourceFile.sourcePath.path) + ";\n\n" + result + "\n";
        return result;
    }
    
    public GeneratedFile[] generate(Package pkg) {
        var result = new ArrayList<GeneratedFile>();
        for (var path : pkg.files.keySet().toArray(String[]::new))
            result.add(new GeneratedFile(path, this.genFile(pkg.files.get(path))));
        return result.toArray(GeneratedFile[]::new);
    }
}