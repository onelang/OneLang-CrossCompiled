using One.Ast;
using One.Transforms.InferTypesPlugins.Helpers;
using One.Transforms.InferTypesPlugins;
using One;
using System.Collections.Generic;
using System;

namespace One.Transforms
{
    public enum InferTypesStage { Invalid, Fields, Properties, Methods }
    
    public class InferTypes : AstTransformer
    {
        protected InferTypesStage stage;
        public List<InferTypesPlugin> plugins;
        public int contextInfoIdx = 0;
        
        public InferTypes(): base("InferTypes")
        {
            this.plugins = new List<InferTypesPlugin>();
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
        
        public void processLambda(Lambda lambda)
        {
            base.visitMethodBase(lambda);
        }
        
        public void processMethodBase(IMethodBase method)
        {
            base.visitMethodBase(method);
        }
        
        public void processBlock(Block block)
        {
            base.visitBlock(block);
        }
        
        public void processVariable(IVariable variable)
        {
            base.visitVariable(variable);
        }
        
        public void processStatement(Statement stmt)
        {
            base.visitStatement(stmt);
        }
        
        public void processExpression(Expression expr)
        {
            base.visitExpression(expr);
        }
        
        public void addPlugin(InferTypesPlugin plugin)
        {
            plugin.main = this;
            plugin.errorMan = this.errorMan;
            this.plugins.push(plugin);
        }
        
        protected override IVariableWithInitializer visitVariableWithInitializer(IVariableWithInitializer variable)
        {
            if (variable.type != null && variable.initializer != null)
                variable.initializer.setExpectedType(variable.type);
            
            variable = base.visitVariableWithInitializer(variable);
            
            if (variable.type == null && variable.initializer != null)
                variable.type = variable.initializer.getType();
            
            return variable;
        }
        
        protected Expression runTransformRound(Expression expr)
        {
            if (expr.actualType != null)
                return expr;
            
            this.errorMan.currentNode = expr;
            
            var transformers = this.plugins.filter(x => x.canTransform(expr));
            if (transformers.length() > 1)
                this.errorMan.throw_($"Multiple transformers found: {transformers.map(x => x.name).join(", ")}");
            if (transformers.length() != 1)
                return expr;
            
            var plugin = transformers.get(0);
            this.contextInfoIdx++;
            this.errorMan.lastContextInfo = $"[{this.contextInfoIdx}] running transform plugin \"{plugin.name}\"";
            try {
                var newExpr = plugin.transform(expr);
                // expression changed, restart the type infering process on the new expression
                if (newExpr != null)
                    newExpr.parentNode = expr.parentNode;
                return newExpr;
            } catch (Exception e)  {
                this.errorMan.currentNode = expr;
                this.errorMan.throw_($"Error while running type transformation phase: {e}");
                return expr;
            }
        }
        
        protected bool detectType(Expression expr)
        {
            foreach (var plugin in this.plugins) {
                if (!plugin.canDetectType(expr))
                    continue;
                this.contextInfoIdx++;
                this.errorMan.lastContextInfo = $"[{this.contextInfoIdx}] running type detection plugin \"{plugin.name}\"";
                this.errorMan.currentNode = expr;
                try {
                    if (plugin.detectType(expr))
                        return true;
                } catch (Exception e)  {
                    this.errorMan.throw_($"Error while running type detection phase: {e}");
                }
            }
            return false;
        }
        
        protected override Expression visitExpression(Expression expr)
        {
            var transExpr = expr;
            while (true) {
                var newExpr = this.runTransformRound(transExpr);
                if (newExpr == transExpr)
                    break;
                transExpr = newExpr;
            }
            
            // if the plugin did not handle the expression, we use the default visit method
            if (transExpr == expr)
                transExpr = base.visitExpression(expr);
            
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
        
        protected override Statement visitStatement(Statement stmt)
        {
            this.currentStatement = stmt;
            
            if (stmt is ReturnStatement retStat && retStat.expression != null && this.currentClosure is Method meth && meth.returns != null) {
                var returnType = meth.returns;
                if (returnType is ClassType classType && classType.decl == this.currentFile.literalTypes.promise.decl && meth.async)
                    returnType = classType.typeArguments.get(0);
                retStat.expression.setExpectedType(returnType);
            }
            
            foreach (var plugin in this.plugins) {
                if (plugin.handleStatement(stmt))
                    return stmt;
            }
            
            return base.visitStatement(stmt);
        }
        
        protected override void visitField(Field field)
        {
            if (this.stage != InferTypesStage.Fields)
                return;
            base.visitField(field);
        }
        
        protected override void visitProperty(Property prop)
        {
            if (this.stage != InferTypesStage.Properties)
                return;
            
            foreach (var plugin in this.plugins) {
                if (plugin.handleProperty(prop))
                    return;
            }
            
            base.visitProperty(prop);
        }
        
        protected override void visitMethodBase(IMethodBase method)
        {
            if (this.stage != InferTypesStage.Methods)
                return;
            
            foreach (var plugin in this.plugins) {
                if (plugin.handleMethod(method))
                    return;
            }
            
            base.visitMethodBase(method);
        }
        
        protected override Lambda visitLambda(Lambda lambda)
        {
            if (lambda.actualType != null)
                return lambda;
            
            var prevClosure = this.currentClosure;
            this.currentClosure = lambda;
            
            foreach (var plugin in this.plugins) {
                if (plugin.handleLambda(lambda))
                    return lambda;
            }
            
            this.currentClosure = prevClosure;
            base.visitMethodBase(lambda);
            return lambda;
        }
        
        public Expression runPluginsOn(Expression expr)
        {
            return this.visitExpression(expr);
        }
        
        protected override void visitClass(Class cls)
        {
            if (cls.attributes.get("external") == "true")
                return;
            base.visitClass(cls);
        }
        
        public override void visitFiles(SourceFile[] files)
        {
            foreach (var stage in new List<InferTypesStage> { InferTypesStage.Fields, InferTypesStage.Properties, InferTypesStage.Methods }) {
                this.stage = stage;
                foreach (var file in files)
                    this.visitFile(file);
            }
        }
    }
}