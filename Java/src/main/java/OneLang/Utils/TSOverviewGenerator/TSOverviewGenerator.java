package OneLang.Utils.TSOverviewGenerator;

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
import OneLang.One.Ast.Types.Method;
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
import OneLang.One.Ast.Types.IHasAttributesAndTrivia;
import OneLang.One.Ast.AstTypes.VoidType;
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
import OneLang.One.Ast.Interfaces.IExpression;
import OneLang.One.Ast.Interfaces.IType;

import OneLang.Utils.TSOverviewGenerator.TSOverviewGenerator;
import OneStd.JSON;
import java.util.Arrays;
import java.util.stream.Collectors;
import OneLang.One.Ast.Types.IHasAttributesAndTrivia;
import OneStd.Objects;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.Types.Property;
import OneLang.One.Ast.Types.Field;
import OneLang.One.Ast.Types.IClassMember;
import OneLang.One.Ast.Statements.VariableDeclaration;
import OneLang.One.Ast.Statements.ForVariable;
import OneLang.One.Ast.Types.MethodParameter;
import OneLang.One.Ast.Types.IVariableWithInitializer;
import OneLang.One.Ast.Types.IVariable;
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
import OneLang.One.Ast.Expressions.NumericLiteral;
import OneLang.One.Ast.Expressions.CharacterLiteral;
import OneLang.One.Ast.Expressions.ElementAccessExpression;
import OneLang.One.Ast.Expressions.TemplateString;
import OneLang.One.Ast.Expressions.BinaryExpression;
import OneLang.One.Ast.Expressions.ArrayLiteral;
import OneLang.One.Ast.Expressions.CastExpression;
import OneLang.One.Ast.Expressions.ConditionalExpression;
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
import OneLang.One.Ast.References.SuperReference;
import OneLang.One.Ast.References.StaticFieldReference;
import OneLang.One.Ast.References.StaticPropertyReference;
import OneLang.One.Ast.References.InstanceFieldReference;
import OneLang.One.Ast.References.InstancePropertyReference;
import OneLang.One.Ast.References.EnumMemberReference;
import OneLang.One.Ast.Expressions.NullCoalesceExpression;
import OneLang.One.Ast.Interfaces.IExpression;
import OneLang.One.Ast.Statements.Block;
import OneLang.One.Ast.Statements.BreakStatement;
import OneLang.One.Ast.Statements.ReturnStatement;
import OneLang.One.Ast.Statements.UnsetStatement;
import OneLang.One.Ast.Statements.ThrowStatement;
import OneLang.One.Ast.Statements.ExpressionStatement;
import OneLang.One.Ast.Statements.ForeachStatement;
import OneLang.One.Ast.Statements.IfStatement;
import OneLang.One.Ast.Statements.WhileStatement;
import OneLang.One.Ast.Statements.ForStatement;
import OneLang.One.Ast.Statements.DoStatement;
import OneLang.One.Ast.Statements.TryStatement;
import OneLang.One.Ast.Statements.ContinueStatement;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Types.Constructor;
import OneLang.One.Ast.Types.GlobalFunction;
import OneLang.One.Ast.AstTypes.VoidType;
import OneLang.One.Ast.Types.IMethodBase;
import java.util.ArrayList;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.IInterface;
import OneStd.RegExp;
import OneLang.One.Ast.Types.UnresolvedImport;
import OneLang.One.Ast.Types.Interface;
import OneLang.One.Ast.Types.Enum;
import OneLang.One.Ast.Types.IImportable;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Types.IAstNode;
import java.util.List;
import OneLang.One.Ast.Types.SourceFile;

public class TSOverviewGenerator {
    public static TSOverviewGenerator preview;
    public Boolean previewOnly;
    public Boolean showTypes;
    
    static {
        TSOverviewGenerator.preview = new TSOverviewGenerator(true, false);
    }
    
    public TSOverviewGenerator(Boolean previewOnly, Boolean showTypes)
    {
        this.previewOnly = previewOnly;
        this.showTypes = showTypes;
    }
    
    public String leading(IHasAttributesAndTrivia item) {
        var result = "";
        if (item.getLeadingTrivia() != null && item.getLeadingTrivia().length() > 0)
            result += item.getLeadingTrivia();
        if (item.getAttributes() != null)
            result += Arrays.stream(Arrays.stream(item.getAttributes().keySet().toArray(String[]::new)).map(x -> "/// {ATTR} name=\"" + x + "\", value=" + JSON.stringify(item.getAttributes().get(x)) + "\n").toArray(String[]::new)).collect(Collectors.joining(""));
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
    
    public String type(IType t, Boolean raw) {
        var repr = t == null ? "???" : t.repr();
        if (Objects.equals(repr, "U:UNKNOWN")) { }
        return (raw ? "" : "{T}") + repr;
    }
    
    public String var(IVariable v) {
        var result = "";
        var isProp = v instanceof Property;
        if (v instanceof Field || v instanceof Property) {
            var m = ((IClassMember)v);
            result += this.preIf("", m.getIsStatic());
            result += m.getVisibility() == Visibility.Private ? "private " : m.getVisibility() == Visibility.Protected ? "protected " : m.getVisibility() == Visibility.Public ? "public " : "VISIBILITY-NOT-SET";
        }
        result += (isProp ? "@prop " : "");
        if (v.getMutability() != null) {
            result += (v.getMutability().unused ? "@unused " : "");
            result += (v.getMutability().mutated ? "@mutated " : "");
            result += (v.getMutability().reassigned ? "@reass " : "");
        }
        result += v.getName() + (isProp ? "()" : "") + ": " + this.type(v.getType(), false);
        if (v instanceof VariableDeclaration || v instanceof ForVariable || v instanceof Field || v instanceof MethodParameter) {
            var init = (((IVariableWithInitializer)v)).getInitializer();
            if (init != null)
                result += this.pre(" = ", this.expr(init));
        }
        return result;
    }
    
    public String expr(IExpression expr) {
        var res = "UNKNOWN-EXPR";
        if (expr instanceof NewExpression)
            res = "new " + this.type(((NewExpression)expr).cls, false) + "(" + (this.previewOnly ? "..." : Arrays.stream(Arrays.stream(((NewExpression)expr).args).map(x -> this.expr(x)).toArray(String[]::new)).collect(Collectors.joining(", "))) + ")";
        else if (expr instanceof UnresolvedNewExpression)
            res = "new " + this.type(((UnresolvedNewExpression)expr).cls, false) + "(" + (this.previewOnly ? "..." : Arrays.stream(Arrays.stream(((UnresolvedNewExpression)expr).args).map(x -> this.expr(x)).toArray(String[]::new)).collect(Collectors.joining(", "))) + ")";
        else if (expr instanceof Identifier)
            res = "{ID}" + ((Identifier)expr).text;
        else if (expr instanceof PropertyAccessExpression)
            res = this.expr(((PropertyAccessExpression)expr).object) + ".{PA}" + ((PropertyAccessExpression)expr).propertyName;
        else if (expr instanceof UnresolvedCallExpression) {
            var typeArgs = ((UnresolvedCallExpression)expr).typeArgs.length > 0 ? "<" + Arrays.stream(Arrays.stream(((UnresolvedCallExpression)expr).typeArgs).map(x -> this.type(x, false)).toArray(String[]::new)).collect(Collectors.joining(", ")) + ">" : "";
            res = this.expr(((UnresolvedCallExpression)expr).func) + typeArgs + "(" + (this.previewOnly ? "..." : Arrays.stream(Arrays.stream(((UnresolvedCallExpression)expr).args).map(x -> this.expr(x)).toArray(String[]::new)).collect(Collectors.joining(", "))) + ")";
        }
        else if (expr instanceof UnresolvedMethodCallExpression) {
            var typeArgs = ((UnresolvedMethodCallExpression)expr).typeArgs.length > 0 ? "<" + Arrays.stream(Arrays.stream(((UnresolvedMethodCallExpression)expr).typeArgs).map(x -> this.type(x, false)).toArray(String[]::new)).collect(Collectors.joining(", ")) + ">" : "";
            res = this.expr(((UnresolvedMethodCallExpression)expr).object) + ".{UM}" + ((UnresolvedMethodCallExpression)expr).methodName + typeArgs + "(" + (this.previewOnly ? "..." : Arrays.stream(Arrays.stream(((UnresolvedMethodCallExpression)expr).args).map(x -> this.expr(x)).toArray(String[]::new)).collect(Collectors.joining(", "))) + ")";
        }
        else if (expr instanceof InstanceMethodCallExpression) {
            var typeArgs = ((InstanceMethodCallExpression)expr).getTypeArgs().length > 0 ? "<" + Arrays.stream(Arrays.stream(((InstanceMethodCallExpression)expr).getTypeArgs()).map(x -> this.type(x, false)).toArray(String[]::new)).collect(Collectors.joining(", ")) + ">" : "";
            res = this.expr(((InstanceMethodCallExpression)expr).object) + ".{M}" + ((InstanceMethodCallExpression)expr).getMethod().name + typeArgs + "(" + (this.previewOnly ? "..." : Arrays.stream(Arrays.stream(((InstanceMethodCallExpression)expr).getArgs()).map(x -> this.expr(x)).toArray(String[]::new)).collect(Collectors.joining(", "))) + ")";
        }
        else if (expr instanceof StaticMethodCallExpression) {
            var typeArgs = ((StaticMethodCallExpression)expr).getTypeArgs().length > 0 ? "<" + Arrays.stream(Arrays.stream(((StaticMethodCallExpression)expr).getTypeArgs()).map(x -> this.type(x, false)).toArray(String[]::new)).collect(Collectors.joining(", ")) + ">" : "";
            res = ((StaticMethodCallExpression)expr).getMethod().parentInterface.getName() + ".{M}" + ((StaticMethodCallExpression)expr).getMethod().name + typeArgs + "(" + (this.previewOnly ? "..." : Arrays.stream(Arrays.stream(((StaticMethodCallExpression)expr).getArgs()).map(x -> this.expr(x)).toArray(String[]::new)).collect(Collectors.joining(", "))) + ")";
        }
        else if (expr instanceof GlobalFunctionCallExpression)
            res = ((GlobalFunctionCallExpression)expr).func.getName() + "(" + (this.previewOnly ? "..." : Arrays.stream(Arrays.stream(((GlobalFunctionCallExpression)expr).args).map(x -> this.expr(x)).toArray(String[]::new)).collect(Collectors.joining(", "))) + ")";
        else if (expr instanceof LambdaCallExpression)
            res = this.expr(((LambdaCallExpression)expr).method) + "(" + (this.previewOnly ? "..." : Arrays.stream(Arrays.stream(((LambdaCallExpression)expr).args).map(x -> this.expr(x)).toArray(String[]::new)).collect(Collectors.joining(", "))) + ")";
        else if (expr instanceof BooleanLiteral)
            res = (((BooleanLiteral)expr).boolValue ? "true" : "false");
        else if (expr instanceof StringLiteral)
            res = JSON.stringify(((StringLiteral)expr).stringValue);
        else if (expr instanceof NumericLiteral)
            res = ((NumericLiteral)expr).valueAsText;
        else if (expr instanceof CharacterLiteral)
            res = "'" + ((CharacterLiteral)expr).charValue + "'";
        else if (expr instanceof ElementAccessExpression)
            res = "(" + this.expr(((ElementAccessExpression)expr).object) + ")[" + this.expr(((ElementAccessExpression)expr).elementExpr) + "]";
        else if (expr instanceof TemplateString)
            res = "`" + Arrays.stream(Arrays.stream(((TemplateString)expr).parts).map(x -> x.isLiteral ? x.literalText : "${" + this.expr(x.expression) + "}").toArray(String[]::new)).collect(Collectors.joining("")) + "`";
        else if (expr instanceof BinaryExpression)
            res = this.expr(((BinaryExpression)expr).left) + " " + ((BinaryExpression)expr).operator + " " + this.expr(((BinaryExpression)expr).right);
        else if (expr instanceof ArrayLiteral)
            res = "[" + Arrays.stream(Arrays.stream(((ArrayLiteral)expr).items).map(x -> this.expr(x)).toArray(String[]::new)).collect(Collectors.joining(", ")) + "]";
        else if (expr instanceof CastExpression)
            res = "<" + this.type(((CastExpression)expr).newType, false) + ">(" + this.expr(((CastExpression)expr).expression) + ")";
        else if (expr instanceof ConditionalExpression)
            res = this.expr(((ConditionalExpression)expr).condition) + " ? " + this.expr(((ConditionalExpression)expr).whenTrue) + " : " + this.expr(((ConditionalExpression)expr).whenFalse);
        else if (expr instanceof InstanceOfExpression)
            res = this.expr(((InstanceOfExpression)expr).expr) + " instanceof " + this.type(((InstanceOfExpression)expr).checkType, false);
        else if (expr instanceof ParenthesizedExpression)
            res = "(" + this.expr(((ParenthesizedExpression)expr).expression) + ")";
        else if (expr instanceof RegexLiteral)
            res = "/" + ((RegexLiteral)expr).pattern + "/" + (((RegexLiteral)expr).global ? "g" : "") + (((RegexLiteral)expr).caseInsensitive ? "g" : "");
        else if (expr instanceof Lambda)
            res = "(" + Arrays.stream(Arrays.stream(((Lambda)expr).getParameters()).map(x -> x.getName() + (x.getType() != null ? ": " + this.type(x.getType(), false) : "")).toArray(String[]::new)).collect(Collectors.joining(", ")) + ")" + (((Lambda)expr).captures != null && ((Lambda)expr).captures.size() > 0 ? " @captures(" + Arrays.stream(((Lambda)expr).captures.stream().map(x -> x.getName()).toArray(String[]::new)).collect(Collectors.joining(", ")) + ")" : "") + " => { " + this.rawBlock(((Lambda)expr).getBody()) + " }";
        else if (expr instanceof UnaryExpression && ((UnaryExpression)expr).unaryType == UnaryType.Prefix)
            res = ((UnaryExpression)expr).operator + this.expr(((UnaryExpression)expr).operand);
        else if (expr instanceof UnaryExpression && ((UnaryExpression)expr).unaryType == UnaryType.Postfix)
            res = this.expr(((UnaryExpression)expr).operand) + ((UnaryExpression)expr).operator;
        else if (expr instanceof MapLiteral) {
            var repr = Arrays.stream(Arrays.stream(((MapLiteral)expr).items).map(item -> item.key + ": " + this.expr(item.value)).toArray(String[]::new)).collect(Collectors.joining(",\n"));
            res = "{L:M}" + (Objects.equals(repr, "") ? "{}" : repr.contains("\n") ? "{\n" + this.pad(repr) + "\n}" : "{ " + repr + " }");
        }
        else if (expr instanceof NullLiteral)
            res = "null";
        else if (expr instanceof AwaitExpression)
            res = "await " + this.expr(((AwaitExpression)expr).expr);
        else if (expr instanceof ThisReference)
            res = "{R}this";
        else if (expr instanceof StaticThisReference)
            res = "{R:Static}this";
        else if (expr instanceof EnumReference)
            res = "{R:Enum}" + ((EnumReference)expr).decl.getName();
        else if (expr instanceof ClassReference)
            res = "{R:Cls}" + ((ClassReference)expr).decl.getName();
        else if (expr instanceof MethodParameterReference)
            res = "{R:MetP}" + ((MethodParameterReference)expr).decl.getName();
        else if (expr instanceof VariableDeclarationReference)
            res = "{V}" + ((VariableDeclarationReference)expr).decl.getName();
        else if (expr instanceof ForVariableReference)
            res = "{R:ForV}" + ((ForVariableReference)expr).decl.getName();
        else if (expr instanceof ForeachVariableReference)
            res = "{R:ForEV}" + ((ForeachVariableReference)expr).decl.getName();
        else if (expr instanceof CatchVariableReference)
            res = "{R:CatchV}" + ((CatchVariableReference)expr).decl.getName();
        else if (expr instanceof GlobalFunctionReference)
            res = "{R:GFunc}" + ((GlobalFunctionReference)expr).decl.getName();
        else if (expr instanceof SuperReference)
            res = "{R}super";
        else if (expr instanceof StaticFieldReference)
            res = "{R:StFi}" + ((StaticFieldReference)expr).decl.parentInterface.getName() + "::" + ((StaticFieldReference)expr).decl.getName();
        else if (expr instanceof StaticPropertyReference)
            res = "{R:StPr}" + ((StaticPropertyReference)expr).decl.parentClass.getName() + "::" + ((StaticPropertyReference)expr).decl.getName();
        else if (expr instanceof InstanceFieldReference)
            res = this.expr(((InstanceFieldReference)expr).object) + ".{F}" + ((InstanceFieldReference)expr).field.getName();
        else if (expr instanceof InstancePropertyReference)
            res = this.expr(((InstancePropertyReference)expr).object) + ".{P}" + ((InstancePropertyReference)expr).property.getName();
        else if (expr instanceof EnumMemberReference)
            res = "{E}" + ((EnumMemberReference)expr).decl.parentEnum.getName() + "::" + ((EnumMemberReference)expr).decl.name;
        else if (expr instanceof NullCoalesceExpression)
            res = this.expr(((NullCoalesceExpression)expr).defaultExpr) + " ?? " + this.expr(((NullCoalesceExpression)expr).exprIfNull);
        else { }
        
        if (this.showTypes)
            res = "<" + this.type(expr.getType(), true) + ">(" + res + ")";
        
        return res;
    }
    
    public String block(Block block, Boolean allowOneLiner) {
        if (this.previewOnly)
            return " { ... }";
        var stmtLen = block.statements.size();
        return stmtLen == 0 ? " { }" : allowOneLiner && stmtLen == 1 ? "\n" + this.pad(this.rawBlock(block)) : " {\n" + this.pad(this.rawBlock(block)) + "\n}";
    }
    
    public String stmt(Statement stmt) {
        var res = "UNKNOWN-STATEMENT";
        if (stmt instanceof BreakStatement)
            res = "break;";
        else if (stmt instanceof ReturnStatement)
            res = ((ReturnStatement)stmt).expression == null ? "return;" : "return " + this.expr(((ReturnStatement)stmt).expression) + ";";
        else if (stmt instanceof UnsetStatement)
            res = "unset " + this.expr(((UnsetStatement)stmt).expression) + ";";
        else if (stmt instanceof ThrowStatement)
            res = "throw " + this.expr(((ThrowStatement)stmt).expression) + ";";
        else if (stmt instanceof ExpressionStatement)
            res = this.expr(((ExpressionStatement)stmt).expression) + ";";
        else if (stmt instanceof VariableDeclaration)
            res = "var " + this.var(((VariableDeclaration)stmt)) + ";";
        else if (stmt instanceof ForeachStatement)
            res = "for (const " + ((ForeachStatement)stmt).itemVar.getName() + " of " + this.expr(((ForeachStatement)stmt).items) + ")" + this.block(((ForeachStatement)stmt).body, true);
        else if (stmt instanceof IfStatement) {
            var elseIf = ((IfStatement)stmt).else_ != null && ((IfStatement)stmt).else_.statements.size() == 1 && ((IfStatement)stmt).else_.statements.get(0) instanceof IfStatement;
            res = "if (" + this.expr(((IfStatement)stmt).condition) + ")" + this.block(((IfStatement)stmt).then, true);
            if (!this.previewOnly)
                res += (elseIf ? "\nelse " + this.stmt(((IfStatement)stmt).else_.statements.get(0)) : "") + (!elseIf && ((IfStatement)stmt).else_ != null ? "\nelse" + this.block(((IfStatement)stmt).else_, true) : "");
        }
        else if (stmt instanceof WhileStatement)
            res = "while (" + this.expr(((WhileStatement)stmt).condition) + ")" + this.block(((WhileStatement)stmt).body, true);
        else if (stmt instanceof ForStatement)
            res = "for (" + (((ForStatement)stmt).itemVar != null ? this.var(((ForStatement)stmt).itemVar) : "") + "; " + this.expr(((ForStatement)stmt).condition) + "; " + this.expr(((ForStatement)stmt).incrementor) + ")" + this.block(((ForStatement)stmt).body, true);
        else if (stmt instanceof DoStatement)
            res = "do" + this.block(((DoStatement)stmt).body, true) + " while (" + this.expr(((DoStatement)stmt).condition) + ")";
        else if (stmt instanceof TryStatement)
            res = "try" + this.block(((TryStatement)stmt).tryBody, false) + (((TryStatement)stmt).catchBody != null ? " catch (" + ((TryStatement)stmt).catchVar.getName() + ")" + this.block(((TryStatement)stmt).catchBody, true) : "") + (((TryStatement)stmt).finallyBody != null ? "finally" + this.block(((TryStatement)stmt).finallyBody, true) : "");
        else if (stmt instanceof ContinueStatement)
            res = "continue;";
        else { }
        return this.previewOnly ? res : this.leading(stmt) + res;
    }
    
    public String rawBlock(Block block) {
        return Arrays.stream(block.statements.stream().map(stmt -> this.stmt(stmt)).toArray(String[]::new)).collect(Collectors.joining("\n"));
    }
    
    public String methodBase(IMethodBase method, IType returns) {
        if (method == null)
            return "";
        var name = method instanceof Method ? ((Method)method).name : method instanceof Constructor ? "constructor" : method instanceof GlobalFunction ? ((GlobalFunction)method).getName() : "???";
        var typeArgs = method instanceof Method ? ((Method)method).typeArguments : null;
        return this.preIf("/* throws */ ", method.getThrows()) + name + this.typeArgs(typeArgs) + "(" + Arrays.stream(Arrays.stream(method.getParameters()).map(p -> this.leading(p) + this.var(p)).toArray(String[]::new)).collect(Collectors.joining(", ")) + ")" + (returns instanceof VoidType ? "" : ": " + this.type(returns, false)) + (method.getBody() != null ? " {\n" + this.pad(this.rawBlock(method.getBody())) + "\n}" : ";");
    }
    
    public String method(Method method) {
        return method == null ? "" : (method.getIsStatic() ? "static " : "") + (method.getAttributes() != null && method.getAttributes().containsKey("mutates") ? "@mutates " : "") + this.methodBase(method, method.returns);
    }
    
    public String classLike(IInterface cls) {
        var resList = new ArrayList<String>();
        resList.add(Arrays.stream(Arrays.stream(cls.getFields()).map(field -> this.var(field) + ";").toArray(String[]::new)).collect(Collectors.joining("\n")));
        if (cls instanceof Class) {
            resList.add(Arrays.stream(Arrays.stream(((Class)cls).properties).map(prop -> this.var(prop) + ";").toArray(String[]::new)).collect(Collectors.joining("\n")));
            resList.add(this.methodBase(((Class)cls).constructor_, VoidType.instance));
        }
        resList.add(Arrays.stream(Arrays.stream(cls.getMethods()).map(method -> this.method(method)).toArray(String[]::new)).collect(Collectors.joining("\n\n")));
        return this.pad(Arrays.stream(resList.stream().filter(x -> !Objects.equals(x, "")).toArray(String[]::new)).collect(Collectors.joining("\n\n")));
    }
    
    public String pad(String str) {
        return Arrays.stream(Arrays.stream(str.split("\\n", -1)).map(x -> "    " + x).toArray(String[]::new)).collect(Collectors.joining("\n"));
    }
    
    public String imp(IImportable imp) {
        return "" + (imp instanceof UnresolvedImport ? "X" : imp instanceof Class ? "C" : imp instanceof Interface ? "I" : imp instanceof Enum ? "E" : "???") + ":" + imp.getName();
    }
    
    public String nodeRepr(IAstNode node) {
        if (node instanceof Statement)
            return this.stmt(((Statement)node));
        else if (node instanceof Expression)
            return this.expr(((Expression)node));
        else
            return "/* TODO: missing */";
    }
    
    public String generate(SourceFile sourceFile) {
        var imps = Arrays.stream(sourceFile.imports).map(imp -> (imp.importAll ? "import * as " + imp.importAs : "import { " + Arrays.stream(Arrays.stream(imp.imports).map(x -> this.imp(x)).toArray(String[]::new)).collect(Collectors.joining(", ")) + " }") + " from \"" + imp.exportScope.packageName + this.pre("/", imp.exportScope.scopeName) + "\";").toArray(String[]::new);
        var enums = Arrays.stream(sourceFile.enums).map(enum_ -> this.leading(enum_) + "enum " + enum_.getName() + " { " + Arrays.stream(Arrays.stream(enum_.values).map(x -> x.name).toArray(String[]::new)).collect(Collectors.joining(", ")) + " }").toArray(String[]::new);
        var intfs = Arrays.stream(sourceFile.interfaces).map(intf -> this.leading(intf) + "interface " + intf.getName() + this.typeArgs(intf.getTypeArguments()) + this.preArr(" extends ", Arrays.stream(intf.getBaseInterfaces()).map(x -> this.type(x, false)).toArray(String[]::new)) + " {\n" + this.classLike(intf) + "\n}").toArray(String[]::new);
        var classes = Arrays.stream(sourceFile.classes).map(cls -> this.leading(cls) + "class " + cls.getName() + this.typeArgs(cls.getTypeArguments()) + this.pre(" extends ", cls.baseClass != null ? this.type(cls.baseClass, false) : null) + this.preArr(" implements ", Arrays.stream(cls.getBaseInterfaces()).map(x -> this.type(x, false)).toArray(String[]::new)) + " {\n" + this.classLike(cls) + "\n}").toArray(String[]::new);
        var funcs = Arrays.stream(sourceFile.funcs).map(func -> this.leading(func) + "function " + func.getName() + this.methodBase(func, func.returns)).toArray(String[]::new);
        var main = this.rawBlock(sourceFile.mainBlock);
        var result = "// export scope: " + sourceFile.exportScope.packageName + "/" + sourceFile.exportScope.scopeName + "\n" + Arrays.stream(new ArrayList<>(List.of(Arrays.stream(imps).collect(Collectors.joining("\n")), Arrays.stream(enums).collect(Collectors.joining("\n")), Arrays.stream(intfs).collect(Collectors.joining("\n\n")), Arrays.stream(classes).collect(Collectors.joining("\n\n")), Arrays.stream(funcs).collect(Collectors.joining("\n\n")), main)).stream().filter(x -> !Objects.equals(x, "")).toArray(String[]::new)).collect(Collectors.joining("\n\n"));
        return result;
    }
}