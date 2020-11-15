package OneLang.One.Transforms.ConvertNullCoalesce;

import OneLang.Utils.TSOverviewGenerator.TSOverviewGenerator;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.IMethodCallExpression;
import OneLang.One.Ast.Expressions.InstanceMethodCallExpression;
import OneLang.One.Ast.Expressions.NullCoalesceExpression;
import OneLang.One.Ast.Expressions.StaticMethodCallExpression;
import OneLang.One.Ast.References.InstanceFieldReference;
import OneLang.One.Ast.References.StaticFieldReference;
import OneLang.One.Ast.References.VariableDeclarationReference;
import OneLang.One.Ast.Statements.Block;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.Statements.VariableDeclaration;
import OneLang.One.Ast.Types.IMethodBase;
import OneLang.One.Ast.Types.IVariable;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.Ast.Types.MutabilityInfo;
import OneLang.One.AstTransformer.AstTransformer;

import OneLang.One.AstTransformer.AstTransformer;
import OneLang.One.Transforms.ConvertNullCoalesce.DefaultExpressionNamingStrategy;
import OneLang.One.Transforms.ConvertNullCoalesce.VariableNameHandler;
import java.util.List;
import OneLang.One.Ast.Statements.Statement;
import java.util.ArrayList;
import OneLang.One.Ast.Types.IVariable;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.Ast.Types.IMethodBase;
import OneLang.One.Ast.Statements.Block;
import java.util.Arrays;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.NullCoalesceExpression;
import OneLang.One.Ast.References.InstanceFieldReference;
import OneLang.One.Ast.References.StaticFieldReference;
import OneLang.One.Ast.Statements.VariableDeclaration;
import OneLang.One.Ast.Types.MutabilityInfo;
import OneLang.One.Ast.References.VariableDeclarationReference;

public class ConvertNullCoalesce extends AstTransformer {
    public DefaultExpressionNamingStrategy exprNaming;
    public VariableNameHandler varNames;
    public List<Statement> statements;
    
    public ConvertNullCoalesce()
    {
        super("RemoveNullCoalesce");
        this.exprNaming = new DefaultExpressionNamingStrategy();
        this.varNames = new VariableNameHandler();
        this.statements = new ArrayList<Statement>();
    }
    
    protected IVariable visitVariable(IVariable variable) {
        this.varNames.useName(variable.getName());
        return super.visitVariable(variable);
    }
    
    protected void visitMethodBase(IMethodBase methodBase) {
        if (!(methodBase instanceof Lambda))
            this.varNames.resetScope();
        super.visitMethodBase(methodBase);
    }
    
    protected Block visitBlock(Block block) {
        var prevStatements = this.statements.toArray(Statement[]::new);
        this.statements = new ArrayList<Statement>();
        for (var stmt : block.statements)
            this.statements.add(this.visitStatement(stmt));
        block.statements = this.statements;
        this.statements = new ArrayList<>(Arrays.asList(prevStatements));
        return block;
    }
    
    protected Expression visitExpression(Expression expr) {
        expr = super.visitExpression(expr);
        if (expr instanceof NullCoalesceExpression) {
            if (((NullCoalesceExpression)expr).defaultExpr instanceof InstanceFieldReference || ((NullCoalesceExpression)expr).defaultExpr instanceof StaticFieldReference)
                return ((NullCoalesceExpression)expr);
            
            var varName = this.varNames.useName(this.exprNaming.getNameFor(((NullCoalesceExpression)expr).defaultExpr));
            
            var varDecl = new VariableDeclaration(varName, ((NullCoalesceExpression)expr).defaultExpr.getType(), ((NullCoalesceExpression)expr).defaultExpr);
            varDecl.setMutability(new MutabilityInfo(false, false, false));
            this.statements.add(varDecl);
            
            ((NullCoalesceExpression)expr).defaultExpr = new VariableDeclarationReference(varDecl);
        }
        return expr;
    }
}