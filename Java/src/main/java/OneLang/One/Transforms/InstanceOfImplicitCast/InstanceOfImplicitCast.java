package OneLang.One.Transforms.InstanceOfImplicitCast;

import OneLang.One.AstTransformer.AstTransformer;
import OneLang.One.Ast.Expressions.InstanceOfExpression;
import OneLang.One.Ast.Expressions.BinaryExpression;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.CastExpression;
import OneLang.One.Ast.Expressions.ConditionalExpression;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.Statements.IfStatement;
import OneLang.One.Ast.Statements.WhileStatement;
import OneLang.One.Ast.References.ForeachVariableReference;
import OneLang.One.Ast.References.VariableDeclarationReference;
import OneLang.One.Ast.References.MethodParameterReference;
import OneLang.One.Ast.References.InstanceFieldReference;
import OneLang.One.Ast.References.ThisReference;
import OneLang.One.Ast.References.Reference;
import OneLang.One.Ast.References.StaticThisReference;
import OneLang.Utils.ArrayHelper.ArrayHelper;

import OneLang.One.AstTransformer.AstTransformer;
import java.util.List;
import OneLang.One.Ast.Expressions.InstanceOfExpression;
import java.util.ArrayList;
import OneLang.One.Ast.Expressions.CastExpression;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import io.onelang.std.core.Objects;
import OneLang.One.Ast.References.VariableDeclarationReference;
import OneLang.One.Ast.References.MethodParameterReference;
import OneLang.One.Ast.References.ForeachVariableReference;
import OneLang.One.Ast.References.InstanceFieldReference;
import OneLang.One.Ast.References.ThisReference;
import OneLang.One.Ast.References.StaticThisReference;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.BinaryExpression;
import OneLang.One.Ast.Expressions.ConditionalExpression;
import OneLang.One.Ast.References.Reference;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.Statements.IfStatement;
import OneLang.One.Ast.Statements.WhileStatement;

public class InstanceOfImplicitCast extends AstTransformer {
    public List<InstanceOfExpression> casts;
    public List<Integer> castCounts;
    
    public InstanceOfImplicitCast()
    {
        super("InstanceOfImplicitCast");
        this.casts = new ArrayList<InstanceOfExpression>();
        this.castCounts = new ArrayList<Integer>();
    }
    
    protected void addCast(InstanceOfExpression cast) {
        if (this.castCounts.size() > 0) {
            cast.implicitCasts = new ArrayList<CastExpression>();
            this.casts.add(cast);
            var last = this.castCounts.size() - 1;
            this.castCounts.set(last, this.castCounts.get(last) + 1);
        }
    }
    
    protected void pushContext() {
        this.castCounts.add(0);
    }
    
    protected void popContext() {
        var castCount = this.castCounts.remove(this.castCounts.size() - 1);
        if (castCount != 0)
            ArrayHelper.removeLastN(this.casts, castCount);
    }
    
    protected Boolean equals(Expression expr1, Expression expr2) {
        // implicit casts don't matter when checking equality...
        while (expr1 instanceof CastExpression && ((CastExpression)expr1).instanceOfCast != null)
            expr1 = ((CastExpression)expr1).expression;
        while (expr2 instanceof CastExpression && ((CastExpression)expr2).instanceOfCast != null)
            expr2 = ((CastExpression)expr2).expression;
        
        // MetP, V, MethP.PA, V.PA, MethP/V [ {FEVR} ], FEVR
        if (expr1 instanceof PropertyAccessExpression)
            return expr2 instanceof PropertyAccessExpression && Objects.equals(((PropertyAccessExpression)expr1).propertyName, ((PropertyAccessExpression)expr2).propertyName) && this.equals(((PropertyAccessExpression)expr1).object, ((PropertyAccessExpression)expr2).object);
        else if (expr1 instanceof VariableDeclarationReference)
            return expr2 instanceof VariableDeclarationReference && ((VariableDeclarationReference)expr1).decl == ((VariableDeclarationReference)expr2).decl;
        else if (expr1 instanceof MethodParameterReference)
            return expr2 instanceof MethodParameterReference && ((MethodParameterReference)expr1).decl == ((MethodParameterReference)expr2).decl;
        else if (expr1 instanceof ForeachVariableReference)
            return expr2 instanceof ForeachVariableReference && ((ForeachVariableReference)expr1).decl == ((ForeachVariableReference)expr2).decl;
        else if (expr1 instanceof InstanceFieldReference)
            return expr2 instanceof InstanceFieldReference && ((InstanceFieldReference)expr1).field == ((InstanceFieldReference)expr2).field;
        else if (expr1 instanceof ThisReference)
            return expr2 instanceof ThisReference;
        else if (expr1 instanceof StaticThisReference)
            return expr2 instanceof StaticThisReference;
        return false;
    }
    
    protected Expression visitExpression(Expression expr) {
        var result = expr;
        if (expr instanceof InstanceOfExpression) {
            this.visitExpression(((InstanceOfExpression)expr).expr);
            this.addCast(((InstanceOfExpression)expr));
        }
        else if (expr instanceof BinaryExpression && Objects.equals(((BinaryExpression)expr).operator, "&&")) {
            ((BinaryExpression)expr).left = this.visitExpression(((BinaryExpression)expr).left);
            ((BinaryExpression)expr).right = this.visitExpression(((BinaryExpression)expr).right);
        }
        else if (expr instanceof ConditionalExpression) {
            this.pushContext();
            ((ConditionalExpression)expr).condition = this.visitExpression(((ConditionalExpression)expr).condition);
            ((ConditionalExpression)expr).whenTrue = this.visitExpression(((ConditionalExpression)expr).whenTrue);
            this.popContext();
            
            ((ConditionalExpression)expr).whenFalse = this.visitExpression(((ConditionalExpression)expr).whenFalse);
        }
        else if (expr instanceof Reference && ((Reference)expr).parentNode instanceof BinaryExpression && Objects.equals(((BinaryExpression)((Reference)expr).parentNode).operator, "=") && ((BinaryExpression)((Reference)expr).parentNode).left == ((Reference)expr)) { }
        else {
            this.pushContext();
            result = super.visitExpression(expr);
            this.popContext();
            // @java final var result2 = result;
            final var result2 = result;
            var match = this.casts.stream().filter(cast -> this.equals(result2, cast.expr)).findFirst().orElse(null);
            if (match != null) {
                var castExpr = new CastExpression(match.checkType, result);
                castExpr.instanceOfCast = match;
                match.implicitCasts.add(castExpr);
                result = castExpr;
            }
        }
        return result;
    }
    
    protected Statement visitStatement(Statement stmt) {
        this.currentStatement = stmt;
        
        if (stmt instanceof IfStatement) {
            this.pushContext();
            ((IfStatement)stmt).condition = this.visitExpression(((IfStatement)stmt).condition);
            this.visitBlock(((IfStatement)stmt).then);
            this.popContext();
            
            if (((IfStatement)stmt).else_ != null)
                this.visitBlock(((IfStatement)stmt).else_);
        }
        else if (stmt instanceof WhileStatement) {
            this.pushContext();
            ((WhileStatement)stmt).condition = this.visitExpression(((WhileStatement)stmt).condition);
            this.visitBlock(((WhileStatement)stmt).body);
            this.popContext();
        }
        else {
            this.pushContext();
            super.visitStatement(stmt);
            this.popContext();
        }
        
        return stmt;
    }
}