using One.Ast;
using One;
using System.Collections.Generic;

namespace One.Transforms
{
    public class FillParent : AstTransformer
    {
        public List<IAstNode> parentNodeStack;
        
        public FillParent(): base("FillParent")
        {
            this.parentNodeStack = new List<IAstNode>();
        }
        
        protected override Expression visitExpression(Expression expr)
        {
            if (this.parentNodeStack.length() == 0) { }
            expr.parentNode = this.parentNodeStack.get(this.parentNodeStack.length() - 1);
            this.parentNodeStack.push(expr);
            base.visitExpression(expr);
            this.parentNodeStack.pop();
            return expr;
        }
        
        protected override Statement visitStatement(Statement stmt)
        {
            this.parentNodeStack.push(stmt);
            base.visitStatement(stmt);
            this.parentNodeStack.pop();
            return stmt;
        }
        
        protected override void visitEnum(Enum_ enum_)
        {
            enum_.parentFile = this.currentFile;
            base.visitEnum(enum_);
            foreach (var value in enum_.values)
                value.parentEnum = enum_;
        }
        
        protected override void visitInterface(Interface intf)
        {
            intf.parentFile = this.currentFile;
            base.visitInterface(intf);
        }
        
        protected override void visitClass(Class cls)
        {
            cls.parentFile = this.currentFile;
            base.visitClass(cls);
        }
        
        protected override void visitGlobalFunction(GlobalFunction func)
        {
            func.parentFile = this.currentFile;
            base.visitGlobalFunction(func);
        }
        
        protected override void visitField(Field field)
        {
            field.parentInterface = this.currentInterface;
            
            this.parentNodeStack.push(field);
            base.visitField(field);
            this.parentNodeStack.pop();
        }
        
        protected override void visitProperty(Property prop)
        {
            prop.parentClass = ((Class)this.currentInterface);
            
            this.parentNodeStack.push(prop);
            base.visitProperty(prop);
            this.parentNodeStack.pop();
        }
        
        protected override void visitMethodBase(IMethodBase method)
        {
            if (method is Constructor const_)
                const_.parentClass = ((Class)this.currentInterface);
            else if (method is Method meth)
                meth.parentInterface = this.currentInterface;
            else if (method is GlobalFunction) { }
            else if (method is Lambda) { }
            else { }
            
            foreach (var param in method.parameters)
                param.parentMethod = method;
            
            this.parentNodeStack.push(method);
            base.visitMethodBase(method);
            this.parentNodeStack.pop();
        }
        
        public override void visitFile(SourceFile file)
        {
            foreach (var imp in file.imports)
                imp.parentFile = file;
            
            base.visitFile(file);
        }
    }
}