package OneLang.Generator.PythonGenerator;

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
import OneLang.One.Ast.Statements.ForVariable;
import OneLang.One.Ast.Statements.TryStatement;
import OneLang.One.Ast.Statements.Block;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.IClassMember;
import OneLang.One.Ast.Types.SourceFile;
import OneLang.One.Ast.Types.IMethodBase;
import OneLang.One.Ast.Types.Constructor;
import OneLang.One.Ast.Types.IVariable;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.Ast.Types.IImportable;
import OneLang.One.Ast.Types.UnresolvedImport;
import OneLang.One.Ast.Types.Interface;
import OneLang.One.Ast.Types.Enum;
import OneLang.One.Ast.Types.IInterface;
import OneLang.One.Ast.Types.Field;
import OneLang.One.Ast.Types.Property;
import OneLang.One.Ast.Types.MethodParameter;
import OneLang.One.Ast.Types.IVariableWithInitializer;
import OneLang.One.Ast.Types.Visibility;
import OneLang.One.Ast.Types.IAstNode;
import OneLang.One.Ast.Types.GlobalFunction;
import OneLang.One.Ast.Types.Package;
import OneLang.One.Ast.Types.SourcePath;
import OneLang.One.Ast.Types.IHasAttributesAndTrivia;
import OneLang.One.Ast.Types.ExportedScope;
import OneLang.One.Ast.Types.ExportScopeRef;
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
import OneLang.One.Ast.References.Reference;
import OneLang.One.Ast.References.VariableReference;
import OneLang.Generator.GeneratedFile.GeneratedFile;
import OneLang.Utils.TSOverviewGenerator.TSOverviewGenerator;
import OneLang.Generator.IGeneratorPlugin.IGeneratorPlugin;
import OneLang.Generator.PythonPlugins.JsToPython.JsToPython;
import OneLang.Generator.NameUtils.NameUtils;
import OneLang.One.Ast.Interfaces.IExpression;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.Generator.IGenerator.IGenerator;
import OneLang.One.ITransformer.ITransformer;

import OneLang.Generator.IGenerator.IGenerator;
import OneLang.One.Ast.Types.Package;
import OneLang.One.Ast.Types.SourceFile;
import java.util.Set;
import OneLang.One.Ast.Types.IInterface;
import java.util.List;
import OneLang.Generator.IGeneratorPlugin.IGeneratorPlugin;
import java.util.ArrayList;
import OneLang.Generator.PythonPlugins.JsToPython.JsToPython;
import OneLang.One.ITransformer.ITransformer;
import OneLang.One.Ast.AstTypes.ClassType;
import OneStd.Objects;
import OneLang.One.Ast.Interfaces.IType;
import java.util.Arrays;
import java.util.stream.Collectors;
import OneLang.One.Ast.Types.ExportScopeRef;
import OneLang.One.Ast.Types.Enum;
import OneStd.RegExp;
import java.util.regex.Pattern;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.Types.IVariable;
import OneLang.One.Ast.Types.IHasAttributesAndTrivia;
import OneLang.One.Ast.Types.IVariableWithInitializer;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.IMethodCallExpression;
import OneLang.One.Ast.Expressions.NewExpression;
import OneLang.One.Ast.Expressions.UnresolvedNewExpression;
import OneLang.One.Ast.Expressions.Identifier;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Ast.Expressions.UnresolvedCallExpression;
import OneLang.One.Ast.Expressions.UnresolvedMethodCallExpression;
import OneLang.One.Ast.Expressions.InstanceMethodCallExpression;
import OneLang.One.Ast.Expressions.StaticMethodCallExpression;
import OneLang.One.Ast.Expressions.GlobalFunctionCallExpression;
import OneLang.One.Ast.Expressions.LambdaCallExpression;
import OneLang.One.Ast.Expressions.BooleanLiteral;
import OneLang.One.Ast.Expressions.StringLiteral;
import OneStd.JSON;
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
import OneLang.One.Ast.Statements.ReturnStatement;
import OneStd.console;
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
import OneLang.One.Ast.References.InstanceFieldReference;
import OneLang.One.Ast.References.InstancePropertyReference;
import OneLang.One.Ast.References.EnumMemberReference;
import OneLang.One.Ast.Expressions.NullCoalesceExpression;
import OneLang.One.Ast.Interfaces.IExpression;
import OneLang.One.Ast.Statements.BreakStatement;
import OneLang.One.Ast.Statements.UnsetStatement;
import OneLang.One.Ast.Statements.ThrowStatement;
import OneLang.One.Ast.Statements.ExpressionStatement;
import OneLang.One.Ast.Statements.VariableDeclaration;
import OneLang.One.Ast.Statements.ForeachStatement;
import OneLang.One.Ast.Statements.IfStatement;
import OneLang.One.Ast.Statements.WhileStatement;
import OneLang.One.Ast.Statements.ForStatement;
import OneLang.One.Ast.Statements.DoStatement;
import OneLang.One.Ast.Statements.TryStatement;
import OneLang.One.Ast.Statements.ContinueStatement;
import OneLang.One.Ast.Statements.Block;
import OneLang.One.Ast.Types.Field;
import OneLang.One.Ast.Types.Class;
import java.util.LinkedHashSet;
import OneLang.One.Ast.Types.Import;
import OneLang.Generator.GeneratedFile.GeneratedFile;

public class PythonGenerator implements IGenerator {
    public Integer tmplStrLevel = 0;
    public Package package_;
    public SourceFile currentFile;
    public Set<String> imports;
    public Set<String> importAllScopes;
    public IInterface currentClass;
    public String[] reservedWords;
    public String[] fieldToMethodHack;
    public List<IGeneratorPlugin> plugins;
    
    public PythonGenerator()
    {
        this.reservedWords = new String[] { "from", "async", "global", "lambda", "cls", "import", "pass" };
        this.fieldToMethodHack = new String[0];
        this.plugins = new ArrayList<IGeneratorPlugin>();
        this.plugins.add(new JsToPython(this));
    }
    
    public String getLangName() {
        return "Python";
    }
    
    public String getExtension() {
        return "py";
    }
    
    public ITransformer[] getTransforms() {
        return new ITransformer[0];
    }
    
    public String type(IType type) {
        if (type instanceof ClassType) {
            if (Objects.equals(((ClassType)type).decl.getName(), "TsString"))
                return "str";
            else if (Objects.equals(((ClassType)type).decl.getName(), "TsBoolean"))
                return "bool";
            else if (Objects.equals(((ClassType)type).decl.getName(), "TsNumber"))
                return "int";
            else
                return this.clsName(((ClassType)type).decl, false);
        }
        else
            return "NOT-HANDLED-TYPE";
    }
    
    public String[] splitName(String name) {
        var nameParts = new ArrayList<String>();
        var partStartIdx = 0;
        for (Integer i = 1; i < name.length(); i++) {
            var prevChrCode = (int)name.charAt(i - 1);
            var chrCode = (int)name.charAt(i);
            if (65 <= chrCode && chrCode <= 90 && !(65 <= prevChrCode && prevChrCode <= 90)) {
                // 'A' .. 'Z'
                nameParts.add(name.substring(partStartIdx, i).toLowerCase());
                partStartIdx = i;
            }
            else if (chrCode == 95) {
                // '-'
                nameParts.add(name.substring(partStartIdx, i));
                partStartIdx = i + 1;
            }
        }
        nameParts.add(name.substring(partStartIdx).toLowerCase());
        return nameParts.toArray(String[]::new);
    }
    
    public String name_(String name) {
        if (Arrays.stream(this.reservedWords).anyMatch(name::equals))
            name += "_";
        if (Arrays.stream(this.fieldToMethodHack).anyMatch(name::equals))
            name += "()";
        return Arrays.stream(this.splitName(name)).collect(Collectors.joining("_"));
    }
    
    public String calcImportedName(ExportScopeRef exportScope, String name) {
        if (this.importAllScopes.contains(exportScope.getId()))
            return name;
        else
            return this.calcImportAlias(exportScope) + "." + name;
    }
    
    public String enumName(Enum enum_, Boolean isDecl) {
        var name = this.name_(enum_.getName()).toUpperCase();
        if (isDecl || enum_.getParentFile().exportScope == null || enum_.getParentFile() == this.currentFile)
            return name;
        return this.calcImportedName(enum_.getParentFile().exportScope, name);
    }
    
    public String enumMemberName(String name) {
        return this.name_(name).toUpperCase();
    }
    
    public String clsName(IInterface cls, Boolean isDecl) {
        // TODO: hack
        if (Objects.equals(cls.getName(), "Set"))
            return "dict";
        if (isDecl || cls.getParentFile().exportScope == null || cls.getParentFile() == this.currentFile)
            return cls.getName();
        return this.calcImportedName(cls.getParentFile().exportScope, cls.getName());
    }
    
    public String leading(Statement item) {
        var result = "";
        if (item.getLeadingTrivia() != null && item.getLeadingTrivia().length() > 0)
            result += item.getLeadingTrivia().replaceAll("//", "#");
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
    
    public Boolean isTsArray(IType type) {
        return type instanceof ClassType && Objects.equals(((ClassType)type).decl.getName(), "TsArray");
    }
    
    public String vis(Visibility v) {
        return v == Visibility.Private ? "__" : v == Visibility.Protected ? "_" : v == Visibility.Public ? "" : "/* TODO: not set */public";
    }
    
    public String varWoInit(IVariable v, IHasAttributesAndTrivia attr) {
        return this.name_(v.getName());
    }
    
    public String var(IVariableWithInitializer v, IHasAttributesAndTrivia attrs) {
        return this.varWoInit(v, attrs) + (v.getInitializer() != null ? " = " + this.expr(v.getInitializer()) : "");
    }
    
    public String exprCall(Expression[] args) {
        return "(" + Arrays.stream(Arrays.stream(args).map(x -> this.expr(x)).toArray(String[]::new)).collect(Collectors.joining(", ")) + ")";
    }
    
    public String callParams(Expression[] args) {
        var argReprs = new ArrayList<String>();
        for (Integer i = 0; i < args.length; i++)
            argReprs.add(this.expr(args[i]));
        return "(" + argReprs.stream().collect(Collectors.joining(", ")) + ")";
    }
    
    public String methodCall(IMethodCallExpression expr) {
        return this.name_(expr.getMethod().name) + this.callParams(expr.getArgs());
    }
    
    public String expr(IExpression expr) {
        for (var plugin : this.plugins) {
            var result = plugin.expr(expr);
            if (result != null)
                return result;
        }
        
        var res = "UNKNOWN-EXPR";
        if (expr instanceof NewExpression) {
            // TODO: hack
            if (Objects.equals(((NewExpression)expr).cls.decl.getName(), "Set"))
                res = ((NewExpression)expr).args.length == 0 ? "dict()" : "dict.fromkeys" + this.callParams(((NewExpression)expr).args);
            else
                res = this.clsName(((NewExpression)expr).cls.decl, false) + this.callParams(((NewExpression)expr).args);
        }
        else if (expr instanceof UnresolvedNewExpression)
            res = "/* TODO: UnresolvedNewExpression */ " + ((UnresolvedNewExpression)expr).cls.typeName + "(" + Arrays.stream(Arrays.stream(((UnresolvedNewExpression)expr).args).map(x -> this.expr(x)).toArray(String[]::new)).collect(Collectors.joining(", ")) + ")";
        else if (expr instanceof Identifier)
            res = "/* TODO: Identifier */ " + ((Identifier)expr).text;
        else if (expr instanceof PropertyAccessExpression)
            res = "/* TODO: PropertyAccessExpression */ " + this.expr(((PropertyAccessExpression)expr).object) + "." + ((PropertyAccessExpression)expr).propertyName;
        else if (expr instanceof UnresolvedCallExpression)
            res = "/* TODO: UnresolvedCallExpression */ " + this.expr(((UnresolvedCallExpression)expr).func) + this.exprCall(((UnresolvedCallExpression)expr).args);
        else if (expr instanceof UnresolvedMethodCallExpression)
            res = "/* TODO: UnresolvedMethodCallExpression */ " + this.expr(((UnresolvedMethodCallExpression)expr).object) + "." + ((UnresolvedMethodCallExpression)expr).methodName + this.exprCall(((UnresolvedMethodCallExpression)expr).args);
        else if (expr instanceof InstanceMethodCallExpression)
            res = this.expr(((InstanceMethodCallExpression)expr).object) + "." + this.methodCall(((InstanceMethodCallExpression)expr));
        else if (expr instanceof StaticMethodCallExpression) {
            //const parent = expr.method.parentInterface === this.currentClass ? "cls" : this.clsName(expr.method.parentInterface);
            var parent = this.clsName(((StaticMethodCallExpression)expr).getMethod().parentInterface, false);
            res = parent + "." + this.methodCall(((StaticMethodCallExpression)expr));
        }
        else if (expr instanceof GlobalFunctionCallExpression) {
            this.imports.add("from OneLangStdLib import *");
            res = this.name_(((GlobalFunctionCallExpression)expr).func.getName()) + this.exprCall(((GlobalFunctionCallExpression)expr).args);
        }
        else if (expr instanceof LambdaCallExpression)
            res = this.expr(((LambdaCallExpression)expr).method) + "(" + Arrays.stream(Arrays.stream(((LambdaCallExpression)expr).args).map(x -> this.expr(x)).toArray(String[]::new)).collect(Collectors.joining(", ")) + ")";
        else if (expr instanceof BooleanLiteral)
            res = (((BooleanLiteral)expr).boolValue ? "True" : "False");
        else if (expr instanceof StringLiteral)
            res = JSON.stringify(((StringLiteral)expr).stringValue);
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
                        else if (Objects.equals(chr, "'"))
                            lit += "\\'";
                        else if (Objects.equals(chr, "{"))
                            lit += "{{";
                        else if (Objects.equals(chr, "}"))
                            lit += "}}";
                        else {
                            var chrCode = (int)chr.charAt(0);
                            if (32 <= chrCode && chrCode <= 126)
                                lit += chr;
                            else
                                throw new Error("invalid char in template string (code=" + chrCode + ")");
                        }
                    }
                    parts.add(lit);
                }
                else {
                    this.tmplStrLevel++;
                    var repr = this.expr(part.expression);
                    this.tmplStrLevel--;
                    parts.add(part.expression instanceof ConditionalExpression ? "{(" + repr + ")}" : "{" + repr + "}");
                }
            }
            res = this.tmplStrLevel == 1 ? "f'" + parts.stream().collect(Collectors.joining("")) + "'" : "f'''" + parts.stream().collect(Collectors.joining("")) + "'''";
        }
        else if (expr instanceof BinaryExpression) {
            var op = Objects.equals(((BinaryExpression)expr).operator, "&&") ? "and" : Objects.equals(((BinaryExpression)expr).operator, "||") ? "or" : ((BinaryExpression)expr).operator;
            res = this.expr(((BinaryExpression)expr).left) + " " + op + " " + this.expr(((BinaryExpression)expr).right);
        }
        else if (expr instanceof ArrayLiteral)
            res = "[" + Arrays.stream(Arrays.stream(((ArrayLiteral)expr).items).map(x -> this.expr(x)).toArray(String[]::new)).collect(Collectors.joining(", ")) + "]";
        else if (expr instanceof CastExpression)
            res = this.expr(((CastExpression)expr).expression);
        else if (expr instanceof ConditionalExpression)
            res = this.expr(((ConditionalExpression)expr).whenTrue) + " if " + this.expr(((ConditionalExpression)expr).condition) + " else " + this.expr(((ConditionalExpression)expr).whenFalse);
        else if (expr instanceof InstanceOfExpression)
            res = "isinstance(" + this.expr(((InstanceOfExpression)expr).expr) + ", " + this.type(((InstanceOfExpression)expr).checkType) + ")";
        else if (expr instanceof ParenthesizedExpression)
            res = "(" + this.expr(((ParenthesizedExpression)expr).expression) + ")";
        else if (expr instanceof RegexLiteral)
            res = "RegExp(" + JSON.stringify(((RegexLiteral)expr).pattern) + ")";
        else if (expr instanceof Lambda) {
            var body = "INVALID-BODY";
            if (((Lambda)expr).getBody().statements.size() == 1 && ((Lambda)expr).getBody().statements.get(0) instanceof ReturnStatement)
                body = this.expr((((ReturnStatement)((Lambda)expr).getBody().statements.get(0))).expression);
            else
                console.error("Multi-line lambda is not yet supported for Python: " + TSOverviewGenerator.preview.nodeRepr(((Lambda)expr)));
            
            var params = Arrays.stream(((Lambda)expr).getParameters()).map(x -> this.name_(x.getName())).toArray(String[]::new);
            
            res = "lambda " + Arrays.stream(params).collect(Collectors.joining(", ")) + ": " + body;
        }
        else if (expr instanceof UnaryExpression && ((UnaryExpression)expr).unaryType == UnaryType.Prefix) {
            var op = Objects.equals(((UnaryExpression)expr).operator, "!") ? "not " : ((UnaryExpression)expr).operator;
            if (Objects.equals(op, "++"))
                res = this.expr(((UnaryExpression)expr).operand) + " = " + this.expr(((UnaryExpression)expr).operand) + " + 1";
            else if (Objects.equals(op, "--"))
                res = this.expr(((UnaryExpression)expr).operand) + " = " + this.expr(((UnaryExpression)expr).operand) + " - 1";
            else
                res = op + this.expr(((UnaryExpression)expr).operand);
        }
        else if (expr instanceof UnaryExpression && ((UnaryExpression)expr).unaryType == UnaryType.Postfix) {
            if (Objects.equals(((UnaryExpression)expr).operator, "++"))
                res = this.expr(((UnaryExpression)expr).operand) + " = " + this.expr(((UnaryExpression)expr).operand) + " + 1";
            else if (Objects.equals(((UnaryExpression)expr).operator, "--"))
                res = this.expr(((UnaryExpression)expr).operand) + " = " + this.expr(((UnaryExpression)expr).operand) + " - 1";
            else
                res = this.expr(((UnaryExpression)expr).operand) + ((UnaryExpression)expr).operator;
        }
        else if (expr instanceof MapLiteral) {
            var repr = Arrays.stream(Arrays.stream(((MapLiteral)expr).items).map(item -> JSON.stringify(item.key) + ": " + this.expr(item.value)).toArray(String[]::new)).collect(Collectors.joining(",\n"));
            res = ((MapLiteral)expr).items.length == 0 ? "{}" : "{\n" + this.pad(repr) + "\n}";
        }
        else if (expr instanceof NullLiteral)
            res = "None";
        else if (expr instanceof AwaitExpression)
            res = this.expr(((AwaitExpression)expr).expr);
        else if (expr instanceof ThisReference)
            res = "self";
        else if (expr instanceof StaticThisReference)
            res = "cls";
        else if (expr instanceof EnumReference)
            res = this.enumName(((EnumReference)expr).decl, false);
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
            res = "super()";
        else if (expr instanceof StaticFieldReference)
            res = this.clsName(((StaticFieldReference)expr).decl.parentInterface, false) + "." + this.name_(((StaticFieldReference)expr).decl.getName());
        else if (expr instanceof StaticPropertyReference)
            res = this.clsName(((StaticPropertyReference)expr).decl.parentClass, false) + ".get_" + this.name_(((StaticPropertyReference)expr).decl.getName()) + "()";
        else if (expr instanceof InstanceFieldReference)
            res = this.expr(((InstanceFieldReference)expr).object) + "." + this.name_(((InstanceFieldReference)expr).field.getName());
        else if (expr instanceof InstancePropertyReference)
            res = this.expr(((InstancePropertyReference)expr).object) + ".get_" + this.name_(((InstancePropertyReference)expr).property.getName()) + "()";
        else if (expr instanceof EnumMemberReference)
            res = this.enumName(((EnumMemberReference)expr).decl.parentEnum, false) + "." + this.enumMemberName(((EnumMemberReference)expr).decl.name);
        else if (expr instanceof NullCoalesceExpression)
            res = this.expr(((NullCoalesceExpression)expr).defaultExpr) + " or " + this.expr(((NullCoalesceExpression)expr).exprIfNull);
        else { }
        return res;
    }
    
    public String stmtDefault(Statement stmt) {
        var nl = "\n";
        if (stmt instanceof BreakStatement)
            return "break";
        else if (stmt instanceof ReturnStatement)
            return ((ReturnStatement)stmt).expression == null ? "return" : "return " + this.expr(((ReturnStatement)stmt).expression);
        else if (stmt instanceof UnsetStatement)
            return "/* unset " + this.expr(((UnsetStatement)stmt).expression) + "; */";
        else if (stmt instanceof ThrowStatement)
            return "raise " + this.expr(((ThrowStatement)stmt).expression);
        else if (stmt instanceof ExpressionStatement)
            return this.expr(((ExpressionStatement)stmt).expression);
        else if (stmt instanceof VariableDeclaration)
            return ((VariableDeclaration)stmt).getInitializer() != null ? this.name_(((VariableDeclaration)stmt).getName()) + " = " + this.expr(((VariableDeclaration)stmt).getInitializer()) : "";
        else if (stmt instanceof ForeachStatement)
            return "for " + this.name_(((ForeachStatement)stmt).itemVar.getName()) + " in " + this.expr(((ForeachStatement)stmt).items) + ":\n" + this.block(((ForeachStatement)stmt).body, false);
        else if (stmt instanceof IfStatement) {
            var elseIf = ((IfStatement)stmt).else_ != null && ((IfStatement)stmt).else_.statements.size() == 1 && ((IfStatement)stmt).else_.statements.get(0) instanceof IfStatement;
            return "if " + this.expr(((IfStatement)stmt).condition) + ":\n" + this.block(((IfStatement)stmt).then, false) + (elseIf ? "\nel" + this.stmt(((IfStatement)stmt).else_.statements.get(0)) : "") + (!elseIf && ((IfStatement)stmt).else_ != null ? "\nelse:\n" + this.block(((IfStatement)stmt).else_, false) : "");
        }
        else if (stmt instanceof WhileStatement)
            return "while " + this.expr(((WhileStatement)stmt).condition) + ":\n" + this.block(((WhileStatement)stmt).body, false);
        else if (stmt instanceof ForStatement)
            return (((ForStatement)stmt).itemVar != null ? this.var(((ForStatement)stmt).itemVar, null) + "\n" : "") + "\nwhile " + this.expr(((ForStatement)stmt).condition) + ":\n" + this.block(((ForStatement)stmt).body, false) + "\n" + this.pad(this.expr(((ForStatement)stmt).incrementor));
        else if (stmt instanceof DoStatement)
            return "while True:\n" + this.block(((DoStatement)stmt).body, false) + "\n" + this.pad("if not (" + this.expr(((DoStatement)stmt).condition) + "):" + nl + this.pad("break"));
        else if (stmt instanceof TryStatement)
            return "try:\n" + this.block(((TryStatement)stmt).tryBody, false) + (((TryStatement)stmt).catchBody != null ? "\nexcept Exception as " + this.name_(((TryStatement)stmt).catchVar.getName()) + ":\n" + this.block(((TryStatement)stmt).catchBody, false) : "") + (((TryStatement)stmt).finallyBody != null ? "\nfinally:\n" + this.block(((TryStatement)stmt).finallyBody, false) : "");
        else if (stmt instanceof ContinueStatement)
            return "continue";
        else
            return "UNKNOWN-STATEMENT";
    }
    
    public String stmt(Statement stmt) {
        String res = null;
        
        if (stmt.getAttributes() != null && stmt.getAttributes().containsKey("python"))
            res = stmt.getAttributes().get("python");
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
    
    public String stmts(Statement[] stmts, Boolean skipPass) {
        return this.pad(stmts.length == 0 && !skipPass ? "pass" : Arrays.stream(Arrays.stream(stmts).map(stmt -> this.stmt(stmt)).toArray(String[]::new)).collect(Collectors.joining("\n")));
    }
    
    public String block(Block block, Boolean skipPass) {
        return this.stmts(block.statements.toArray(Statement[]::new), skipPass);
    }
    
    public String pass(String str) {
        return Objects.equals(str, "") ? "pass" : str;
    }
    
    public String cls(Class cls) {
        if (Objects.equals(cls.getAttributes().get("external"), "true"))
            return "";
        this.currentClass = cls;
        var resList = new ArrayList<String>();
        var classAttributes = new ArrayList<String>();
        
        var staticFields = Arrays.stream(cls.getFields()).filter(x -> x.getIsStatic()).toArray(Field[]::new);
        
        if (staticFields.length > 0) {
            this.imports.add("import OneLangStdLib as one");
            classAttributes.add("@one.static_init");
            var fieldInits = Arrays.stream(staticFields).map(f -> "cls." + this.vis(f.getVisibility()) + cls.getName().replace(this.var(f, f), "cls")).toArray(String[]::new);
            resList.add("@classmethod\ndef static_init(cls):\n" + this.pad(Arrays.stream(fieldInits).collect(Collectors.joining("\n"))));
        }
        
        var constrStmts = new ArrayList<String>();
        
        for (var field : Arrays.stream(cls.getFields()).filter(x -> !x.getIsStatic()).toArray(Field[]::new)) {
            var init = field.constructorParam != null ? this.name_(field.constructorParam.getName()) : field.getInitializer() != null ? this.expr(field.getInitializer()) : "None";
            constrStmts.add("self." + this.name_(field.getName()) + " = " + init);
        }
        
        if (cls.baseClass != null) {
            if (cls.constructor_ != null && cls.constructor_.superCallArgs != null)
                constrStmts.add("super().__init__(" + Arrays.stream(Arrays.stream(cls.constructor_.superCallArgs).map(x -> this.expr(x)).toArray(String[]::new)).collect(Collectors.joining(", ")) + ")");
            else
                constrStmts.add("super().__init__()");
        }
        
        if (cls.constructor_ != null)
            for (var stmt : cls.constructor_.getBody().statements)
                constrStmts.add(this.stmt(stmt));
        
        resList.add("def __init__(self" + (cls.constructor_ == null ? "" : Arrays.stream(Arrays.stream(cls.constructor_.getParameters()).map(p -> ", " + this.var(p, null)).toArray(String[]::new)).collect(Collectors.joining(""))) + "):\n" + this.pad(this.pass(constrStmts.stream().collect(Collectors.joining("\n")))));
        
        for (var prop : cls.properties) {
            if (prop.getter != null)
                resList.add("def get_" + this.name_(prop.getName()) + "(self):\n" + this.block(prop.getter, false));
        }
        
        var methods = new ArrayList<String>();
        for (var method : cls.getMethods()) {
            if (method.getBody() == null)
                continue;
            // declaration only
            methods.add((method.getIsStatic() ? "@classmethod\n" : "") + "def " + this.name_(method.name) + "(" + (method.getIsStatic() ? "cls" : "self") + Arrays.stream(Arrays.stream(method.getParameters()).map(p -> ", " + this.var(p, null)).toArray(String[]::new)).collect(Collectors.joining("")) + "):" + "\n" + this.block(method.getBody(), false));
        }
        resList.add(methods.stream().collect(Collectors.joining("\n\n")));
        var resList2 = resList.stream().filter(x -> !Objects.equals(x, "")).toArray(String[]::new);
        
        var clsHdr = "class " + this.clsName(cls, true) + (cls.baseClass != null ? "(" + this.clsName((((ClassType)cls.baseClass)).decl, false) + ")" : "") + ":\n";
        return Arrays.stream(classAttributes.stream().map(x -> x + "\n").toArray(String[]::new)).collect(Collectors.joining("")) + clsHdr + this.pad(resList2.length > 0 ? Arrays.stream(resList2).collect(Collectors.joining("\n\n")) : "pass");
    }
    
    public String pad(String str) {
        return Objects.equals(str, "") ? "" : Arrays.stream(Arrays.stream(str.split("\\n", -1)).map(x -> "    " + x).toArray(String[]::new)).collect(Collectors.joining("\n"));
    }
    
    public String calcRelImport(ExportScopeRef targetPath, ExportScopeRef fromPath) {
        var targetParts = targetPath.scopeName.split("/", -1);
        var fromParts = fromPath.scopeName.split("/", -1);
        
        var sameLevel = 0;
        while (sameLevel < targetParts.length && sameLevel < fromParts.length && Objects.equals(targetParts[sameLevel], fromParts[sameLevel]))
            sameLevel++;
        
        var result = "";
        for (Integer i = 1; i < fromParts.length - sameLevel; i++)
            result += ".";
        
        for (Integer i = sameLevel; i < targetParts.length; i++)
            result += "." + targetParts[i];
        
        return result;
    }
    
    public String calcImportAlias(ExportScopeRef targetPath) {
        var parts = targetPath.scopeName.split("/", -1);
        var filename = parts[parts.length - 1];
        return NameUtils.shortName(filename);
    }
    
    public String genFile(SourceFile sourceFile) {
        this.currentFile = sourceFile;
        this.imports = new LinkedHashSet<String>();
        this.importAllScopes = new LinkedHashSet<String>();
        this.imports.add("from OneLangStdLib import *");
        // TODO: do not add this globally, just for nativeResolver methods
               
        if (sourceFile.enums.length > 0)
            this.imports.add("from enum import Enum");
        
        for (var import_ : Arrays.stream(sourceFile.imports).filter(x -> !x.importAll).toArray(Import[]::new)) {
            if (Objects.equals(import_.getAttributes().get("python-ignore"), "true"))
                continue;
            
            if (import_.getAttributes().containsKey("python-import-all")) {
                this.imports.add("from " + import_.getAttributes().get("python-import-all") + " import *");
                this.importAllScopes.add(import_.exportScope.getId());
            }
            else {
                var alias = this.calcImportAlias(import_.exportScope);
                this.imports.add("import " + this.package_.name + "." + import_.exportScope.scopeName.replaceAll("/", ".") + " as " + alias);
            }
        }
        
        var enums = new ArrayList<String>();
        for (var enum_ : sourceFile.enums) {
            var values = new ArrayList<String>();
            for (Integer i = 0; i < enum_.values.length; i++)
                values.add(this.enumMemberName(enum_.values[i].name) + " = " + i + 1);
            enums.add("class " + this.enumName(enum_, true) + "(Enum):\n" + this.pad(values.stream().collect(Collectors.joining("\n"))));
        }
        
        var classes = new ArrayList<String>();
        for (var cls : sourceFile.classes)
            classes.add(this.cls(cls));
        
        var main = sourceFile.mainBlock.statements.size() > 0 ? this.block(sourceFile.mainBlock, false) : "";
        
        var imports = new ArrayList<String>();
        for (var imp : this.imports)
            imports.add(imp);
        
        return Arrays.stream(new ArrayList<>(List.of(imports.stream().collect(Collectors.joining("\n")), enums.stream().collect(Collectors.joining("\n\n")), classes.stream().collect(Collectors.joining("\n\n")), main)).stream().filter(x -> !Objects.equals(x, "")).toArray(String[]::new)).collect(Collectors.joining("\n\n"));
    }
    
    public GeneratedFile[] generate(Package pkg) {
        this.package_ = pkg;
        var result = new ArrayList<GeneratedFile>();
        for (var path : pkg.files.keySet().toArray(String[]::new))
            result.add(new GeneratedFile(pkg.name + "/" + path, this.genFile(pkg.files.get(path))));
        return result.toArray(GeneratedFile[]::new);
    }
}