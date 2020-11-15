using One.Ast;
using One;
using System.Collections.Generic;
using System.Linq;
using Utils;

namespace One.Transforms
{
    public interface IExpressionNamingStrategy {
        string getNameFor(Expression expr);
    }
    
    public class DefaultExpressionNamingStrategy : IExpressionNamingStrategy {
        public string getNameFor(Expression expr)
        {
            if (expr is InstanceMethodCallExpression instMethCallExpr || expr is StaticMethodCallExpression)
                return $"{(((IMethodCallExpression)expr)).method.name}Result";
            return "result";
        }
    }
    
    public class VariableNameHandler {
        public Map<string, int> usageCount;
        
        public VariableNameHandler()
        {
            this.usageCount = new Map<string, int>();
        }
        
        public string useName(string name)
        {
            if (this.usageCount.has(name)) {
                var newIdx = this.usageCount.get(name) + 1;
                this.usageCount.set(name, newIdx);
                return $"{name}{newIdx}";
            }
            else {
                this.usageCount.set(name, 1);
                return name;
            }
        }
        
        public void resetScope()
        {
            this.usageCount = new Map<string, int>();
        }
    }
    
    public class ConvertNullCoalesce : AstTransformer {
        public DefaultExpressionNamingStrategy exprNaming;
        public VariableNameHandler varNames;
        public List<Statement> statements;
        
        public ConvertNullCoalesce(): base("RemoveNullCoalesce")
        {
            this.exprNaming = new DefaultExpressionNamingStrategy();
            this.varNames = new VariableNameHandler();
            this.statements = new List<Statement>();
        }
        
        protected override IVariable visitVariable(IVariable variable)
        {
            this.varNames.useName(variable.name);
            return base.visitVariable(variable);
        }
        
        protected override void visitMethodBase(IMethodBase methodBase)
        {
            if (!(methodBase is Lambda))
                this.varNames.resetScope();
            base.visitMethodBase(methodBase);
        }
        
        protected override Block visitBlock(Block block)
        {
            var prevStatements = this.statements.ToArray();
            this.statements = new List<Statement>();
            foreach (var stmt in block.statements)
                this.statements.push(this.visitStatement(stmt));
            block.statements = this.statements;
            this.statements = prevStatements.ToList();
            return block;
        }
        
        protected override Expression visitExpression(Expression expr)
        {
            expr = base.visitExpression(expr);
            if (expr is NullCoalesceExpression nullCoalExpr) {
                if (nullCoalExpr.defaultExpr is InstanceFieldReference instFieldRef || nullCoalExpr.defaultExpr is StaticFieldReference)
                    return nullCoalExpr;
                
                var varName = this.varNames.useName(this.exprNaming.getNameFor(nullCoalExpr.defaultExpr));
                
                var varDecl = new VariableDeclaration(varName, nullCoalExpr.defaultExpr.getType(), nullCoalExpr.defaultExpr);
                varDecl.mutability = new MutabilityInfo(false, false, false);
                this.statements.push(varDecl);
                
                nullCoalExpr.defaultExpr = new VariableDeclarationReference(varDecl);
            }
            return expr;
        }
    }
}