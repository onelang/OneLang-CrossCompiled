using One.Ast;
using One;
using System.Collections.Generic;
using Utils;

namespace One.Transforms
{
    public class InstanceOfImplicitCast : AstTransformer
    {
        public List<InstanceOfExpression> casts;
        public List<int> castCounts;
        
        public InstanceOfImplicitCast(): base("InstanceOfImplicitCast")
        {
            this.casts = new List<InstanceOfExpression>();
            this.castCounts = new List<int>();
        }
        
        protected void addCast(InstanceOfExpression cast)
        {
            if (this.castCounts.length() > 0) {
                cast.implicitCasts = new List<CastExpression>();
                this.casts.push(cast);
                var last = this.castCounts.length() - 1;
                this.castCounts.set(last, this.castCounts.get(last) + 1);
            }
        }
        
        protected void pushContext()
        {
            this.castCounts.push(0);
        }
        
        protected void popContext()
        {
            var castCount = this.castCounts.pop();
            if (castCount != 0)
                ArrayHelper.removeLastN(this.casts, castCount);
        }
        
        protected bool equals(Expression expr1, Expression expr2)
        {
            // implicit casts don't matter when checking equality...
            while (expr1 is CastExpression castExpr && castExpr.instanceOfCast != null)
                expr1 = castExpr.expression;
            while (expr2 is CastExpression castExpr2 && castExpr2.instanceOfCast != null)
                expr2 = castExpr2.expression;
            
            // MetP, V, MethP.PA, V.PA, MethP/V [ {FEVR} ], FEVR
            if (expr1 is PropertyAccessExpression propAccExpr)
                return expr2 is PropertyAccessExpression propAccExpr2 && propAccExpr.propertyName == propAccExpr2.propertyName && this.equals(propAccExpr.object_, propAccExpr2.object_);
            else if (expr1 is VariableDeclarationReference varDeclRef)
                return expr2 is VariableDeclarationReference varDeclRef2 && varDeclRef.decl == varDeclRef2.decl;
            else if (expr1 is MethodParameterReference methParRef)
                return expr2 is MethodParameterReference methParRef2 && methParRef.decl == methParRef2.decl;
            else if (expr1 is ForeachVariableReference forVarRef)
                return expr2 is ForeachVariableReference forVarRef2 && forVarRef.decl == forVarRef2.decl;
            else if (expr1 is InstanceFieldReference instFieldRef)
                return expr2 is InstanceFieldReference instFieldRef2 && instFieldRef.field == instFieldRef2.field;
            else if (expr1 is ThisReference)
                return expr2 is ThisReference;
            else if (expr1 is StaticThisReference)
                return expr2 is StaticThisReference;
            return false;
        }
        
        protected override Expression visitExpression(Expression expr)
        {
            var result = expr;
            if (expr is InstanceOfExpression instOfExpr) {
                this.visitExpression(instOfExpr.expr);
                this.addCast(instOfExpr);
            }
            else if (expr is BinaryExpression binExpr && binExpr.operator_ == "&&") {
                binExpr.left = this.visitExpression(binExpr.left);
                binExpr.right = this.visitExpression(binExpr.right);
            }
            else if (expr is ConditionalExpression condExpr) {
                this.pushContext();
                condExpr.condition = this.visitExpression(condExpr.condition);
                condExpr.whenTrue = this.visitExpression(condExpr.whenTrue);
                this.popContext();
                
                condExpr.whenFalse = this.visitExpression(condExpr.whenFalse);
            }
            else if (expr is Reference ref_ && ref_.parentNode is BinaryExpression binExpr2 && binExpr2.operator_ == "=" && binExpr2.left == ref_) { }
            else {
                this.pushContext();
                result = base.visitExpression(expr);
                this.popContext();
                // @java final var result2 = result;
                var result2 = result;
                var match = this.casts.find(cast => this.equals(result2, cast.expr));
                if (match != null) {
                    var castExpr = new CastExpression(match.checkType, result);
                    castExpr.instanceOfCast = match;
                    match.implicitCasts.push(castExpr);
                    result = castExpr;
                }
            }
            return result;
        }
        
        protected override Statement visitStatement(Statement stmt)
        {
            this.currentStatement = stmt;
            
            if (stmt is IfStatement ifStat) {
                this.pushContext();
                ifStat.condition = this.visitExpression(ifStat.condition);
                this.visitBlock(ifStat.then);
                this.popContext();
                
                if (ifStat.else_ != null)
                    this.visitBlock(ifStat.else_);
            }
            else if (stmt is WhileStatement whileStat) {
                this.pushContext();
                whileStat.condition = this.visitExpression(whileStat.condition);
                this.visitBlock(whileStat.body);
                this.popContext();
            }
            else {
                this.pushContext();
                base.visitStatement(stmt);
                this.popContext();
            }
            
            return stmt;
        }
    }
}