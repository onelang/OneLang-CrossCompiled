package OneLang.One.AstTransformer;

import OneLang.One.Ast.AstTypes.IHasTypeArguments;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.InterfaceType;
import OneLang.One.Ast.AstTypes.UnresolvedType;
import OneLang.One.Ast.AstTypes.LambdaType;
import OneLang.One.Ast.Expressions.Identifier;
import OneLang.One.Ast.Expressions.BinaryExpression;
import OneLang.One.Ast.Expressions.ConditionalExpression;
import OneLang.One.Ast.Expressions.NewExpression;
import OneLang.One.Ast.Expressions.TemplateString;
import OneLang.One.Ast.Expressions.ParenthesizedExpression;
import OneLang.One.Ast.Expressions.UnaryExpression;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Ast.Expressions.ElementAccessExpression;
import OneLang.One.Ast.Expressions.ArrayLiteral;
import OneLang.One.Ast.Expressions.MapLiteral;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.CastExpression;
import OneLang.One.Ast.Expressions.UnresolvedCallExpression;
import OneLang.One.Ast.Expressions.InstanceOfExpression;
import OneLang.One.Ast.Expressions.AwaitExpression;
import OneLang.One.Ast.Expressions.StringLiteral;
import OneLang.One.Ast.Expressions.NumericLiteral;
import OneLang.One.Ast.Expressions.NullLiteral;
import OneLang.One.Ast.Expressions.RegexLiteral;
import OneLang.One.Ast.Expressions.BooleanLiteral;
import OneLang.One.Ast.Expressions.StaticMethodCallExpression;
import OneLang.One.Ast.Expressions.InstanceMethodCallExpression;
import OneLang.One.Ast.Expressions.UnresolvedNewExpression;
import OneLang.One.Ast.Expressions.NullCoalesceExpression;
import OneLang.One.Ast.Expressions.UnresolvedMethodCallExpression;
import OneLang.One.Ast.Expressions.GlobalFunctionCallExpression;
import OneLang.One.Ast.Expressions.LambdaCallExpression;
import OneLang.One.Ast.Statements.ReturnStatement;
import OneLang.One.Ast.Statements.ExpressionStatement;
import OneLang.One.Ast.Statements.IfStatement;
import OneLang.One.Ast.Statements.ThrowStatement;
import OneLang.One.Ast.Statements.VariableDeclaration;
import OneLang.One.Ast.Statements.WhileStatement;
import OneLang.One.Ast.Statements.ForStatement;
import OneLang.One.Ast.Statements.ForeachStatement;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.Statements.UnsetStatement;
import OneLang.One.Ast.Statements.BreakStatement;
import OneLang.One.Ast.Statements.ContinueStatement;
import OneLang.One.Ast.Statements.DoStatement;
import OneLang.One.Ast.Statements.TryStatement;
import OneLang.One.Ast.Statements.Block;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Types.Constructor;
import OneLang.One.Ast.Types.Field;
import OneLang.One.Ast.Types.Property;
import OneLang.One.Ast.Types.Interface;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.Enum;
import OneLang.One.Ast.Types.EnumMember;
import OneLang.One.Ast.Types.SourceFile;
import OneLang.One.Ast.Types.IVariable;
import OneLang.One.Ast.Types.IVariableWithInitializer;
import OneLang.One.Ast.Types.MethodParameter;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.Ast.Types.IMethodBase;
import OneLang.One.Ast.Types.Package;
import OneLang.One.Ast.Types.GlobalFunction;
import OneLang.One.Ast.Types.IInterface;
import OneLang.One.Ast.Types.IAstNode;
import OneLang.One.Ast.Types.IHasAttributesAndTrivia;
import OneLang.One.Ast.Types.Import;
import OneLang.One.Ast.References.ClassReference;
import OneLang.One.Ast.References.EnumReference;
import OneLang.One.Ast.References.ThisReference;
import OneLang.One.Ast.References.MethodParameterReference;
import OneLang.One.Ast.References.VariableDeclarationReference;
import OneLang.One.Ast.References.ForVariableReference;
import OneLang.One.Ast.References.ForeachVariableReference;
import OneLang.One.Ast.References.SuperReference;
import OneLang.One.Ast.References.InstanceFieldReference;
import OneLang.One.Ast.References.InstancePropertyReference;
import OneLang.One.Ast.References.StaticPropertyReference;
import OneLang.One.Ast.References.StaticFieldReference;
import OneLang.One.Ast.References.CatchVariableReference;
import OneLang.One.Ast.References.GlobalFunctionReference;
import OneLang.One.Ast.References.EnumMemberReference;
import OneLang.One.Ast.References.StaticThisReference;
import OneLang.One.Ast.References.VariableReference;
import OneLang.One.ErrorManager.ErrorManager;
import OneLang.One.ITransformer.ITransformer;
import OneLang.One.Ast.Interfaces.IType;

import OneLang.One.ITransformer.ITransformer;
import OneLang.One.ErrorManager.ErrorManager;
import OneLang.One.Ast.Types.SourceFile;
import OneLang.One.Ast.Types.IInterface;
import OneLang.One.Ast.Types.IMethodBase;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.Types.IHasAttributesAndTrivia;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.InterfaceType;
import OneLang.One.Ast.AstTypes.UnresolvedType;
import OneLang.One.Ast.AstTypes.IHasTypeArguments;
import java.util.Arrays;
import OneLang.One.Ast.AstTypes.LambdaType;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.Identifier;
import OneLang.One.Ast.Types.IVariable;
import OneLang.One.Ast.Types.IVariableWithInitializer;
import OneLang.One.Ast.Statements.VariableDeclaration;
import OneLang.One.Ast.Statements.ReturnStatement;
import OneLang.One.Ast.Statements.ExpressionStatement;
import OneLang.One.Ast.Statements.IfStatement;
import OneLang.One.Ast.Statements.ThrowStatement;
import OneLang.One.Ast.Statements.WhileStatement;
import OneLang.One.Ast.Statements.DoStatement;
import OneLang.One.Ast.Statements.ForStatement;
import OneLang.One.Ast.Statements.ForeachStatement;
import OneLang.One.Ast.Statements.TryStatement;
import OneLang.One.Ast.Statements.BreakStatement;
import OneLang.One.Ast.Statements.UnsetStatement;
import OneLang.One.Ast.Statements.ContinueStatement;
import OneLang.One.Ast.Statements.Block;
import java.util.ArrayList;
import OneLang.One.Ast.Expressions.TemplateString;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.Ast.References.VariableReference;
import OneLang.One.Ast.Expressions.BinaryExpression;
import OneLang.One.Ast.Expressions.NullCoalesceExpression;
import OneLang.One.Ast.Expressions.UnresolvedCallExpression;
import OneLang.One.Ast.Expressions.UnresolvedMethodCallExpression;
import OneLang.One.Ast.Expressions.ConditionalExpression;
import OneLang.One.Ast.Expressions.UnresolvedNewExpression;
import OneLang.One.Ast.Expressions.NewExpression;
import OneLang.One.Ast.Expressions.ParenthesizedExpression;
import OneLang.One.Ast.Expressions.UnaryExpression;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Ast.Expressions.ElementAccessExpression;
import OneLang.One.Ast.Expressions.ArrayLiteral;
import OneLang.One.Ast.Expressions.MapLiteral;
import OneLang.One.Ast.Expressions.StringLiteral;
import OneLang.One.Ast.Expressions.BooleanLiteral;
import OneLang.One.Ast.Expressions.NumericLiteral;
import OneLang.One.Ast.Expressions.NullLiteral;
import OneLang.One.Ast.Expressions.RegexLiteral;
import OneLang.One.Ast.Expressions.CastExpression;
import OneLang.One.Ast.Expressions.InstanceOfExpression;
import OneLang.One.Ast.Expressions.AwaitExpression;
import OneLang.One.Ast.References.ClassReference;
import OneLang.One.Ast.References.EnumReference;
import OneLang.One.Ast.References.ThisReference;
import OneLang.One.Ast.References.StaticThisReference;
import OneLang.One.Ast.References.MethodParameterReference;
import OneLang.One.Ast.References.VariableDeclarationReference;
import OneLang.One.Ast.References.ForVariableReference;
import OneLang.One.Ast.References.ForeachVariableReference;
import OneLang.One.Ast.References.CatchVariableReference;
import OneLang.One.Ast.References.GlobalFunctionReference;
import OneLang.One.Ast.References.SuperReference;
import OneLang.One.Ast.References.InstanceFieldReference;
import OneLang.One.Ast.References.InstancePropertyReference;
import OneLang.One.Ast.References.StaticFieldReference;
import OneLang.One.Ast.References.StaticPropertyReference;
import OneLang.One.Ast.References.EnumMemberReference;
import OneLang.One.Ast.Expressions.StaticMethodCallExpression;
import OneLang.One.Ast.Expressions.GlobalFunctionCallExpression;
import OneLang.One.Ast.Expressions.InstanceMethodCallExpression;
import OneLang.One.Ast.Expressions.LambdaCallExpression;
import OneLang.One.Ast.Types.MethodParameter;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Types.GlobalFunction;
import OneLang.One.Ast.Types.Constructor;
import OneLang.One.Ast.Types.Field;
import OneLang.One.Ast.Types.Property;
import OneLang.One.Ast.Types.Interface;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.Enum;
import OneLang.One.Ast.Types.EnumMember;
import OneLang.One.Ast.Types.Import;
import OneLang.One.Ast.Types.Package;

public class AstTransformer implements ITransformer {
    public ErrorManager errorMan;
    public SourceFile currentFile;
    public IInterface currentInterface;
    public IMethodBase currentMethod;
    public IMethodBase currentClosure;
    public Statement currentStatement;
    
    String name;
    public String getName() { return this.name; }
    public void setName(String value) { this.name = value; }
    
    public AstTransformer(String name)
    {
        this.setName(name);
        this.errorMan = new ErrorManager();
        this.currentFile = null;
        this.currentInterface = null;
        this.currentMethod = null;
        this.currentClosure = null;
        this.currentStatement = null;
    }
    
    protected void visitAttributesAndTrivia(IHasAttributesAndTrivia node) {
        
    }
    
    protected IType visitType(IType type) {
        if (type instanceof ClassType || type instanceof InterfaceType || type instanceof UnresolvedType) {
            var type2 = ((IHasTypeArguments)type);
            type2.setTypeArguments(Arrays.stream(type2.getTypeArguments()).map(x -> this.visitType(x)).toArray(IType[]::new));
        }
        else if (type instanceof LambdaType) {
            for (var mp : ((LambdaType)type).parameters)
                this.visitMethodParameter(mp);
            ((LambdaType)type).returnType = this.visitType(((LambdaType)type).returnType);
        }
        return type;
    }
    
    protected Expression visitIdentifier(Identifier id) {
        return id;
    }
    
    protected IVariable visitVariable(IVariable variable) {
        if (variable.getType() != null)
            variable.setType(this.visitType(variable.getType()));
        return variable;
    }
    
    protected IVariableWithInitializer visitVariableWithInitializer(IVariableWithInitializer variable) {
        this.visitVariable(variable);
        if (variable.getInitializer() != null)
            variable.setInitializer(this.visitExpression(variable.getInitializer()));
        return variable;
    }
    
    protected VariableDeclaration visitVariableDeclaration(VariableDeclaration stmt) {
        this.visitVariableWithInitializer(stmt);
        return stmt;
    }
    
    protected Statement visitUnknownStatement(Statement stmt) {
        this.errorMan.throw_("Unknown statement type");
        return stmt;
    }
    
    protected Statement visitStatement(Statement stmt) {
        this.currentStatement = stmt;
        this.visitAttributesAndTrivia(stmt);
        if (stmt instanceof ReturnStatement) {
            if (((ReturnStatement)stmt).expression != null)
                ((ReturnStatement)stmt).expression = this.visitExpression(((ReturnStatement)stmt).expression);
        }
        else if (stmt instanceof ExpressionStatement)
            ((ExpressionStatement)stmt).expression = this.visitExpression(((ExpressionStatement)stmt).expression);
        else if (stmt instanceof IfStatement) {
            ((IfStatement)stmt).condition = this.visitExpression(((IfStatement)stmt).condition);
            ((IfStatement)stmt).then = this.visitBlock(((IfStatement)stmt).then);
            if (((IfStatement)stmt).else_ != null)
                ((IfStatement)stmt).else_ = this.visitBlock(((IfStatement)stmt).else_);
        }
        else if (stmt instanceof ThrowStatement)
            ((ThrowStatement)stmt).expression = this.visitExpression(((ThrowStatement)stmt).expression);
        else if (stmt instanceof VariableDeclaration)
            return this.visitVariableDeclaration(((VariableDeclaration)stmt));
        else if (stmt instanceof WhileStatement) {
            ((WhileStatement)stmt).condition = this.visitExpression(((WhileStatement)stmt).condition);
            ((WhileStatement)stmt).body = this.visitBlock(((WhileStatement)stmt).body);
        }
        else if (stmt instanceof DoStatement) {
            ((DoStatement)stmt).condition = this.visitExpression(((DoStatement)stmt).condition);
            ((DoStatement)stmt).body = this.visitBlock(((DoStatement)stmt).body);
        }
        else if (stmt instanceof ForStatement) {
            if (((ForStatement)stmt).itemVar != null)
                this.visitVariableWithInitializer(((ForStatement)stmt).itemVar);
            ((ForStatement)stmt).condition = this.visitExpression(((ForStatement)stmt).condition);
            ((ForStatement)stmt).incrementor = this.visitExpression(((ForStatement)stmt).incrementor);
            ((ForStatement)stmt).body = this.visitBlock(((ForStatement)stmt).body);
        }
        else if (stmt instanceof ForeachStatement) {
            this.visitVariable(((ForeachStatement)stmt).itemVar);
            ((ForeachStatement)stmt).items = this.visitExpression(((ForeachStatement)stmt).items);
            ((ForeachStatement)stmt).body = this.visitBlock(((ForeachStatement)stmt).body);
        }
        else if (stmt instanceof TryStatement) {
            ((TryStatement)stmt).tryBody = this.visitBlock(((TryStatement)stmt).tryBody);
            if (((TryStatement)stmt).catchBody != null) {
                this.visitVariable(((TryStatement)stmt).catchVar);
                ((TryStatement)stmt).catchBody = this.visitBlock(((TryStatement)stmt).catchBody);
            }
            if (((TryStatement)stmt).finallyBody != null)
                ((TryStatement)stmt).finallyBody = this.visitBlock(((TryStatement)stmt).finallyBody);
        }
        else if (stmt instanceof BreakStatement) { }
        else if (stmt instanceof UnsetStatement)
            ((UnsetStatement)stmt).expression = this.visitExpression(((UnsetStatement)stmt).expression);
        else if (stmt instanceof ContinueStatement) { }
        else
            return this.visitUnknownStatement(stmt);
        return stmt;
    }
    
    protected Block visitBlock(Block block) {
        block.statements = new ArrayList<>(Arrays.asList(block.statements.stream().map(x -> this.visitStatement(x)).toArray(Statement[]::new)));
        return block;
    }
    
    protected TemplateString visitTemplateString(TemplateString expr) {
        for (Integer i = 0; i < expr.parts.length; i++) {
            var part = expr.parts[i];
            if (!part.isLiteral)
                part.expression = this.visitExpression(part.expression);
        }
        return expr;
    }
    
    protected Expression visitUnknownExpression(Expression expr) {
        this.errorMan.throw_("Unknown expression type");
        return expr;
    }
    
    protected Lambda visitLambda(Lambda lambda) {
        var prevClosure = this.currentClosure;
        this.currentClosure = lambda;
        this.visitMethodBase(lambda);
        this.currentClosure = prevClosure;
        return lambda;
    }
    
    protected VariableReference visitVariableReference(VariableReference varRef) {
        return varRef;
    }
    
    protected Expression visitExpression(Expression expr) {
        if (expr instanceof BinaryExpression) {
            ((BinaryExpression)expr).left = this.visitExpression(((BinaryExpression)expr).left);
            ((BinaryExpression)expr).right = this.visitExpression(((BinaryExpression)expr).right);
        }
        else if (expr instanceof NullCoalesceExpression) {
            ((NullCoalesceExpression)expr).defaultExpr = this.visitExpression(((NullCoalesceExpression)expr).defaultExpr);
            ((NullCoalesceExpression)expr).exprIfNull = this.visitExpression(((NullCoalesceExpression)expr).exprIfNull);
        }
        else if (expr instanceof UnresolvedCallExpression) {
            ((UnresolvedCallExpression)expr).func = this.visitExpression(((UnresolvedCallExpression)expr).func);
            ((UnresolvedCallExpression)expr).typeArgs = Arrays.stream(((UnresolvedCallExpression)expr).typeArgs).map(x -> this.visitType(x)).toArray(IType[]::new);
            ((UnresolvedCallExpression)expr).args = Arrays.stream(((UnresolvedCallExpression)expr).args).map(x -> this.visitExpression(x)).toArray(Expression[]::new);
        }
        else if (expr instanceof UnresolvedMethodCallExpression) {
            ((UnresolvedMethodCallExpression)expr).object = this.visitExpression(((UnresolvedMethodCallExpression)expr).object);
            ((UnresolvedMethodCallExpression)expr).typeArgs = Arrays.stream(((UnresolvedMethodCallExpression)expr).typeArgs).map(x -> this.visitType(x)).toArray(IType[]::new);
            ((UnresolvedMethodCallExpression)expr).args = Arrays.stream(((UnresolvedMethodCallExpression)expr).args).map(x -> this.visitExpression(x)).toArray(Expression[]::new);
        }
        else if (expr instanceof ConditionalExpression) {
            ((ConditionalExpression)expr).condition = this.visitExpression(((ConditionalExpression)expr).condition);
            ((ConditionalExpression)expr).whenTrue = this.visitExpression(((ConditionalExpression)expr).whenTrue);
            ((ConditionalExpression)expr).whenFalse = this.visitExpression(((ConditionalExpression)expr).whenFalse);
        }
        else if (expr instanceof Identifier)
            return this.visitIdentifier(((Identifier)expr));
        else if (expr instanceof UnresolvedNewExpression) {
            this.visitType(((UnresolvedNewExpression)expr).cls);
            ((UnresolvedNewExpression)expr).args = Arrays.stream(((UnresolvedNewExpression)expr).args).map(x -> this.visitExpression(x)).toArray(Expression[]::new);
        }
        else if (expr instanceof NewExpression) {
            this.visitType(((NewExpression)expr).cls);
            ((NewExpression)expr).args = Arrays.stream(((NewExpression)expr).args).map(x -> this.visitExpression(x)).toArray(Expression[]::new);
        }
        else if (expr instanceof TemplateString)
            return this.visitTemplateString(((TemplateString)expr));
        else if (expr instanceof ParenthesizedExpression)
            ((ParenthesizedExpression)expr).expression = this.visitExpression(((ParenthesizedExpression)expr).expression);
        else if (expr instanceof UnaryExpression)
            ((UnaryExpression)expr).operand = this.visitExpression(((UnaryExpression)expr).operand);
        else if (expr instanceof PropertyAccessExpression)
            ((PropertyAccessExpression)expr).object = this.visitExpression(((PropertyAccessExpression)expr).object);
        else if (expr instanceof ElementAccessExpression) {
            ((ElementAccessExpression)expr).object = this.visitExpression(((ElementAccessExpression)expr).object);
            ((ElementAccessExpression)expr).elementExpr = this.visitExpression(((ElementAccessExpression)expr).elementExpr);
        }
        else if (expr instanceof ArrayLiteral)
            ((ArrayLiteral)expr).items = Arrays.stream(((ArrayLiteral)expr).items).map(x -> this.visitExpression(x)).toArray(Expression[]::new);
        else if (expr instanceof MapLiteral)
            for (var item : ((MapLiteral)expr).items)
                item.value = this.visitExpression(item.value);
        else if (expr instanceof StringLiteral) { }
        else if (expr instanceof BooleanLiteral) { }
        else if (expr instanceof NumericLiteral) { }
        else if (expr instanceof NullLiteral) { }
        else if (expr instanceof RegexLiteral) { }
        else if (expr instanceof CastExpression) {
            ((CastExpression)expr).newType = this.visitType(((CastExpression)expr).newType);
            ((CastExpression)expr).expression = this.visitExpression(((CastExpression)expr).expression);
        }
        else if (expr instanceof InstanceOfExpression) {
            ((InstanceOfExpression)expr).expr = this.visitExpression(((InstanceOfExpression)expr).expr);
            ((InstanceOfExpression)expr).checkType = this.visitType(((InstanceOfExpression)expr).checkType);
        }
        else if (expr instanceof AwaitExpression)
            ((AwaitExpression)expr).expr = this.visitExpression(((AwaitExpression)expr).expr);
        else if (expr instanceof Lambda)
            return this.visitLambda(((Lambda)expr));
        else if (expr instanceof ClassReference) { }
        else if (expr instanceof EnumReference) { }
        else if (expr instanceof ThisReference) { }
        else if (expr instanceof StaticThisReference) { }
        else if (expr instanceof MethodParameterReference)
            return this.visitVariableReference(((MethodParameterReference)expr));
        else if (expr instanceof VariableDeclarationReference)
            return this.visitVariableReference(((VariableDeclarationReference)expr));
        else if (expr instanceof ForVariableReference)
            return this.visitVariableReference(((ForVariableReference)expr));
        else if (expr instanceof ForeachVariableReference)
            return this.visitVariableReference(((ForeachVariableReference)expr));
        else if (expr instanceof CatchVariableReference)
            return this.visitVariableReference(((CatchVariableReference)expr));
        else if (expr instanceof GlobalFunctionReference) { }
        else if (expr instanceof SuperReference) { }
        else if (expr instanceof InstanceFieldReference) {
            ((InstanceFieldReference)expr).object = this.visitExpression(((InstanceFieldReference)expr).object);
            return this.visitVariableReference(((InstanceFieldReference)expr));
        }
        else if (expr instanceof InstancePropertyReference) {
            ((InstancePropertyReference)expr).object = this.visitExpression(((InstancePropertyReference)expr).object);
            return this.visitVariableReference(((InstancePropertyReference)expr));
        }
        else if (expr instanceof StaticFieldReference)
            return this.visitVariableReference(((StaticFieldReference)expr));
        else if (expr instanceof StaticPropertyReference)
            return this.visitVariableReference(((StaticPropertyReference)expr));
        else if (expr instanceof EnumMemberReference) { }
        else if (expr instanceof StaticMethodCallExpression) {
            ((StaticMethodCallExpression)expr).setTypeArgs(Arrays.stream(((StaticMethodCallExpression)expr).getTypeArgs()).map(x -> this.visitType(x)).toArray(IType[]::new));
            ((StaticMethodCallExpression)expr).setArgs(Arrays.stream(((StaticMethodCallExpression)expr).getArgs()).map(x -> this.visitExpression(x)).toArray(Expression[]::new));
        }
        else if (expr instanceof GlobalFunctionCallExpression)
            ((GlobalFunctionCallExpression)expr).args = Arrays.stream(((GlobalFunctionCallExpression)expr).args).map(x -> this.visitExpression(x)).toArray(Expression[]::new);
        else if (expr instanceof InstanceMethodCallExpression) {
            ((InstanceMethodCallExpression)expr).object = this.visitExpression(((InstanceMethodCallExpression)expr).object);
            ((InstanceMethodCallExpression)expr).setTypeArgs(Arrays.stream(((InstanceMethodCallExpression)expr).getTypeArgs()).map(x -> this.visitType(x)).toArray(IType[]::new));
            ((InstanceMethodCallExpression)expr).setArgs(Arrays.stream(((InstanceMethodCallExpression)expr).getArgs()).map(x -> this.visitExpression(x)).toArray(Expression[]::new));
        }
        else if (expr instanceof LambdaCallExpression)
            ((LambdaCallExpression)expr).args = Arrays.stream(((LambdaCallExpression)expr).args).map(x -> this.visitExpression(x)).toArray(Expression[]::new);
        else
            return this.visitUnknownExpression(expr);
        return expr;
    }
    
    protected void visitMethodParameter(MethodParameter methodParameter) {
        this.visitAttributesAndTrivia(methodParameter);
        this.visitVariableWithInitializer(methodParameter);
    }
    
    protected void visitMethodBase(IMethodBase method) {
        for (var item : method.getParameters())
            this.visitMethodParameter(item);
        
        if (method.getBody() != null)
            method.setBody(this.visitBlock(method.getBody()));
    }
    
    protected void visitMethod(Method method) {
        this.currentMethod = method;
        this.currentClosure = method;
        this.visitAttributesAndTrivia(method);
        this.visitMethodBase(method);
        method.returns = this.visitType(method.returns);
        this.currentClosure = null;
        this.currentMethod = null;
    }
    
    protected void visitGlobalFunction(GlobalFunction func) {
        this.visitMethodBase(func);
        func.returns = this.visitType(func.returns);
    }
    
    protected void visitConstructor(Constructor constructor) {
        this.currentMethod = constructor;
        this.currentClosure = constructor;
        this.visitAttributesAndTrivia(constructor);
        this.visitMethodBase(constructor);
        this.currentClosure = null;
        this.currentMethod = null;
    }
    
    protected void visitField(Field field) {
        this.visitAttributesAndTrivia(field);
        this.visitVariableWithInitializer(field);
    }
    
    protected void visitProperty(Property prop) {
        this.visitAttributesAndTrivia(prop);
        this.visitVariable(prop);
        if (prop.getter != null)
            prop.getter = this.visitBlock(prop.getter);
        if (prop.setter != null)
            prop.setter = this.visitBlock(prop.setter);
    }
    
    protected void visitInterface(Interface intf) {
        this.currentInterface = intf;
        this.visitAttributesAndTrivia(intf);
        intf.setBaseInterfaces(Arrays.stream(intf.getBaseInterfaces()).map(x -> this.visitType(x)).toArray(IType[]::new));
        for (var field : intf.getFields())
            this.visitField(field);
        for (var method : intf.getMethods())
            this.visitMethod(method);
        this.currentInterface = null;
    }
    
    protected void visitClass(Class cls) {
        this.currentInterface = cls;
        this.visitAttributesAndTrivia(cls);
        if (cls.constructor_ != null)
            this.visitConstructor(cls.constructor_);
        
        cls.baseClass = this.visitType(cls.baseClass);
        cls.setBaseInterfaces(Arrays.stream(cls.getBaseInterfaces()).map(x -> this.visitType(x)).toArray(IType[]::new));
        for (var field : cls.getFields())
            this.visitField(field);
        for (var prop : cls.properties)
            this.visitProperty(prop);
        for (var method : cls.getMethods())
            this.visitMethod(method);
        this.currentInterface = null;
    }
    
    protected void visitEnum(Enum enum_) {
        this.visitAttributesAndTrivia(enum_);
        for (var value : enum_.values)
            this.visitEnumMember(value);
    }
    
    protected void visitEnumMember(EnumMember enumMember) {
        
    }
    
    protected void visitImport(Import imp) {
        this.visitAttributesAndTrivia(imp);
    }
    
    public void visitFile(SourceFile sourceFile) {
        this.errorMan.resetContext(this);
        this.currentFile = sourceFile;
        for (var imp : sourceFile.imports)
            this.visitImport(imp);
        for (var enum_ : sourceFile.enums)
            this.visitEnum(enum_);
        for (var intf : sourceFile.interfaces)
            this.visitInterface(intf);
        for (var cls : sourceFile.classes)
            this.visitClass(cls);
        for (var func : sourceFile.funcs)
            this.visitGlobalFunction(func);
        sourceFile.mainBlock = this.visitBlock(sourceFile.mainBlock);
        this.currentFile = null;
    }
    
    public void visitFiles(SourceFile[] files) {
        for (var file : files)
            this.visitFile(file);
    }
    
    public void visitPackage(Package pkg) {
        this.visitFiles(pkg.files.values().toArray(SourceFile[]::new));
    }
}