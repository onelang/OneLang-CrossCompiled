package OneLang.One.Transforms.InferTypes;

import OneLang.One.AstTransformer.AstTransformer;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Types.Package;
import OneLang.One.Ast.Types.Property;
import OneLang.One.Ast.Types.Field;
import OneLang.One.Ast.Types.IMethodBase;
import OneLang.One.Ast.Types.IVariableWithInitializer;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.Ast.Types.IVariable;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.SourceFile;
import OneLang.One.Transforms.InferTypesPlugins.BasicTypeInfer.BasicTypeInfer;
import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import OneLang.One.Transforms.InferTypesPlugins.ArrayAndMapLiteralTypeInfer.ArrayAndMapLiteralTypeInfer;
import OneLang.One.Transforms.InferTypesPlugins.ResolveFieldAndPropertyAccess.ResolveFieldAndPropertyAccess;
import OneLang.One.Transforms.InferTypesPlugins.ResolveMethodCalls.ResolveMethodCalls;
import OneLang.One.Transforms.InferTypesPlugins.LambdaResolver.LambdaResolver;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.Statements.Block;
import OneLang.One.Ast.Statements.ReturnStatement;
import OneLang.One.Transforms.InferTypesPlugins.ResolveEnumMemberAccess.ResolveEnumMemberAccess;
import OneLang.One.Transforms.InferTypesPlugins.InferReturnType.InferReturnType;
import OneLang.One.Transforms.InferTypesPlugins.TypeScriptNullCoalesce.TypeScriptNullCoalesce;
import OneLang.One.Transforms.InferTypesPlugins.InferForeachVarType.InferForeachVarType;
import OneLang.One.Transforms.InferTypesPlugins.ResolveFuncCalls.ResolveFuncCalls;
import OneLang.One.Transforms.InferTypesPlugins.NullabilityCheckWithNot.NullabilityCheckWithNot;
import OneLang.One.Transforms.InferTypesPlugins.ResolveNewCall.ResolveNewCalls;
import OneLang.One.Transforms.InferTypesPlugins.ResolveElementAccess.ResolveElementAccess;
import OneLang.One.Ast.AstTypes.ClassType;

import OneLang.One.AstTransformer.AstTransformer;
import java.util.List;
import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import java.util.ArrayList;
import OneLang.One.Transforms.InferTypesPlugins.BasicTypeInfer.BasicTypeInfer;
import OneLang.One.Transforms.InferTypesPlugins.ArrayAndMapLiteralTypeInfer.ArrayAndMapLiteralTypeInfer;
import OneLang.One.Transforms.InferTypesPlugins.ResolveFieldAndPropertyAccess.ResolveFieldAndPropertyAccess;
import OneLang.One.Transforms.InferTypesPlugins.ResolveMethodCalls.ResolveMethodCalls;
import OneLang.One.Transforms.InferTypesPlugins.LambdaResolver.LambdaResolver;
import OneLang.One.Transforms.InferTypesPlugins.InferReturnType.InferReturnType;
import OneLang.One.Transforms.InferTypesPlugins.ResolveEnumMemberAccess.ResolveEnumMemberAccess;
import OneLang.One.Transforms.InferTypesPlugins.TypeScriptNullCoalesce.TypeScriptNullCoalesce;
import OneLang.One.Transforms.InferTypesPlugins.InferForeachVarType.InferForeachVarType;
import OneLang.One.Transforms.InferTypesPlugins.ResolveFuncCalls.ResolveFuncCalls;
import OneLang.One.Transforms.InferTypesPlugins.NullabilityCheckWithNot.NullabilityCheckWithNot;
import OneLang.One.Transforms.InferTypesPlugins.ResolveNewCall.ResolveNewCalls;
import OneLang.One.Transforms.InferTypesPlugins.ResolveElementAccess.ResolveElementAccess;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.Ast.Types.IMethodBase;
import OneLang.One.Ast.Statements.Block;
import OneLang.One.Ast.Types.IVariable;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Types.IVariableWithInitializer;
import java.util.Arrays;
import java.util.stream.Collectors;
import OneLang.One.Ast.Statements.ReturnStatement;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.Types.Field;
import OneLang.One.Ast.Types.Property;
import OneStd.Objects;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.SourceFile;

public class InferTypes extends AstTransformer {
    protected InferTypesStage stage;
    public List<InferTypesPlugin> plugins;
    public Integer contextInfoIdx = 0;
    
    public InferTypes()
    {
        super("InferTypes");
        this.plugins = new ArrayList<InferTypesPlugin>();
        this.addPlugin(new BasicTypeInfer());
        this.addPlugin(new ArrayAndMapLiteralTypeInfer());
        this.addPlugin(new ResolveFieldAndPropertyAccess());
        this.addPlugin(new ResolveMethodCalls());
        this.addPlugin(new LambdaResolver());
        this.addPlugin(new InferReturnType());
        this.addPlugin(new ResolveEnumMemberAccess());
        this.addPlugin(new TypeScriptNullCoalesce());
        this.addPlugin(new InferForeachVarType());
        this.addPlugin(new ResolveFuncCalls());
        this.addPlugin(new NullabilityCheckWithNot());
        this.addPlugin(new ResolveNewCalls());
        this.addPlugin(new ResolveElementAccess());
    }
    
    public void processLambda(Lambda lambda) {
        super.visitMethodBase(lambda);
    }
    
    public void processMethodBase(IMethodBase method) {
        super.visitMethodBase(method);
    }
    
    public void processBlock(Block block) {
        super.visitBlock(block);
    }
    
    public void processVariable(IVariable variable) {
        super.visitVariable(variable);
    }
    
    public void processStatement(Statement stmt) {
        super.visitStatement(stmt);
    }
    
    public void processExpression(Expression expr) {
        super.visitExpression(expr);
    }
    
    public void addPlugin(InferTypesPlugin plugin) {
        plugin.main = this;
        plugin.errorMan = this.errorMan;
        this.plugins.add(plugin);
    }
    
    protected IVariableWithInitializer visitVariableWithInitializer(IVariableWithInitializer variable) {
        if (variable.getType() != null && variable.getInitializer() != null)
            variable.getInitializer().setExpectedType(variable.getType(), false);
        
        variable = super.visitVariableWithInitializer(variable);
        
        if (variable.getType() == null && variable.getInitializer() != null)
            variable.setType(variable.getInitializer().getType());
        
        return variable;
    }
    
    protected Expression runTransformRound(Expression expr) {
        if (expr.actualType != null)
            return expr;
        
        this.errorMan.currentNode = expr;
        
        var transformers = this.plugins.stream().filter(x -> x.canTransform(expr)).toArray(InferTypesPlugin[]::new);
        if (transformers.length > 1)
            this.errorMan.throw_("Multiple transformers found: " + Arrays.stream(Arrays.stream(transformers).map(x -> x.name).toArray(String[]::new)).collect(Collectors.joining(", ")));
        if (transformers.length != 1)
            return expr;
        
        var plugin = transformers[0];
        this.contextInfoIdx++;
        this.errorMan.lastContextInfo = "[" + this.contextInfoIdx + "] running transform plugin \"" + plugin.name + "\"";
        try {
            var newExpr = plugin.transform(expr);
            // expression changed, restart the type infering process on the new expression
            if (newExpr != null)
                newExpr.parentNode = expr.parentNode;
            return newExpr;
        } catch (Exception e)  {
            this.errorMan.currentNode = expr;
            this.errorMan.throw_("Error while running type transformation phase: " + e);
            return expr;
        }
    }
    
    protected Boolean detectType(Expression expr) {
        for (var plugin : this.plugins) {
            if (!plugin.canDetectType(expr))
                continue;
            this.contextInfoIdx++;
            this.errorMan.lastContextInfo = "[" + this.contextInfoIdx + "] running type detection plugin \"" + plugin.name + "\"";
            this.errorMan.currentNode = expr;
            try {
                if (plugin.detectType(expr))
                    return true;
            } catch (Exception e)  {
                this.errorMan.throw_("Error while running type detection phase: " + e);
            }
        }
        return false;
    }
    
    protected Expression visitExpression(Expression expr) {
        var transExpr = expr;
        while (true) {
            var newExpr = this.runTransformRound(transExpr);
            if (newExpr == transExpr)
                break;
            transExpr = newExpr;
        }
        
        // if the plugin did not handle the expression, we use the default visit method
        if (transExpr == expr)
            transExpr = super.visitExpression(expr);
        
        if (transExpr.actualType != null)
            return transExpr;
        
        var detectSuccess = this.detectType(transExpr);
        
        if (transExpr.actualType == null) {
            if (detectSuccess)
                this.errorMan.throw_("Type detection failed, although plugin tried to handle it");
            else
                this.errorMan.throw_("Type detection failed: none of the plugins could resolve the type");
        }
        
        return transExpr;
    }
    
    protected Statement visitStatement(Statement stmt) {
        this.currentStatement = stmt;
        
        if (stmt instanceof ReturnStatement && ((ReturnStatement)stmt).expression != null && this.currentClosure instanceof Method && ((Method)this.currentClosure).returns != null) {
            var returnType = ((Method)this.currentClosure).returns;
            if (returnType instanceof ClassType && ((ClassType)returnType).decl == this.currentFile.literalTypes.promise.decl && ((Method)this.currentClosure).async)
                returnType = ((ClassType)returnType).getTypeArguments()[0];
            ((ReturnStatement)stmt).expression.setExpectedType(returnType, false);
        }
        
        for (var plugin : this.plugins) {
            if (plugin.handleStatement(stmt))
                return stmt;
        }
        
        return super.visitStatement(stmt);
    }
    
    protected void visitField(Field field) {
        if (this.stage != InferTypesStage.Fields)
            return;
        super.visitField(field);
    }
    
    protected void visitProperty(Property prop) {
        if (this.stage != InferTypesStage.Properties)
            return;
        
        for (var plugin : this.plugins) {
            if (plugin.handleProperty(prop))
                return;
        }
        
        super.visitProperty(prop);
    }
    
    protected void visitMethodBase(IMethodBase method) {
        if (this.stage != InferTypesStage.Methods)
            return;
        
        for (var plugin : this.plugins) {
            if (plugin.handleMethod(method))
                return;
        }
        
        super.visitMethodBase(method);
    }
    
    protected Lambda visitLambda(Lambda lambda) {
        if (lambda.actualType != null)
            return lambda;
        
        var prevClosure = this.currentClosure;
        this.currentClosure = lambda;
        
        for (var plugin : this.plugins) {
            if (plugin.handleLambda(lambda))
                return lambda;
        }
        
        this.currentClosure = prevClosure;
        super.visitMethodBase(lambda);
        return lambda;
    }
    
    public Expression runPluginsOn(Expression expr) {
        return this.visitExpression(expr);
    }
    
    protected void visitClass(Class cls) {
        if (Objects.equals(cls.getAttributes().get("external"), "true"))
            return;
        super.visitClass(cls);
    }
    
    public void visitFiles(SourceFile[] files) {
        for (var stage : new ArrayList<>(List.of(InferTypesStage.Fields, InferTypesStage.Properties, InferTypesStage.Methods))) {
            this.stage = stage;
            for (var file : files)
                this.visitFile(file);
        }
    }
}