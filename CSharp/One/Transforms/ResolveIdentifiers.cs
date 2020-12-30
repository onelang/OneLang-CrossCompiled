using One.Ast;
using One;
using System.Collections.Generic;

namespace One.Transforms
{
    public class SymbolLookup
    {
        public ErrorManager errorMan;
        public List<List<string>> levelStack;
        public List<string> levelNames;
        public List<string> currLevel;
        public Map<string, IReferencable> symbols;
        
        public SymbolLookup()
        {
            this.errorMan = new ErrorManager();
            this.levelStack = new List<List<string>>();
            this.levelNames = new List<string>();
            this.symbols = new Map<string, IReferencable>();
        }
        
        public void throw_(string msg)
        {
            this.errorMan.throw_($"{msg} (context: {this.levelNames.join(" > ")})");
        }
        
        public void pushContext(string name)
        {
            this.levelStack.push(this.currLevel);
            this.levelNames.push(name);
            this.currLevel = new List<string>();
        }
        
        public void addSymbol(string name, IReferencable ref_)
        {
            if (this.symbols.has(name))
                this.throw_($"Symbol shadowing: {name}");
            this.symbols.set(name, ref_);
            this.currLevel.push(name);
        }
        
        public void popContext()
        {
            foreach (var name in this.currLevel)
                this.symbols.delete(name);
            this.levelNames.pop();
            this.currLevel = this.levelStack.length() > 0 ? this.levelStack.pop() : null;
        }
        
        public IReferencable getSymbol(string name)
        {
            return this.symbols.get(name);
        }
    }
    
    public class ResolveIdentifiers : AstTransformer
    {
        public SymbolLookup symbolLookup;
        
        public ResolveIdentifiers(): base("ResolveIdentifiers")
        {
            this.symbolLookup = new SymbolLookup();
        }
        
        protected override IType visitType(IType type)
        {
            return type;
        }
        
        protected override Expression visitIdentifier(Identifier id)
        {
            base.visitIdentifier(id);
            var symbol = this.symbolLookup.getSymbol(id.text);
            if (symbol == null) {
                this.errorMan.throw_($"Identifier '{id.text}' was not found in available symbols");
                return id;
            }
            
            Reference ref_ = null;
            if (symbol is Class class_ && id.text == "this") {
                var withinStaticMethod = this.currentMethod is Method meth && meth.isStatic;
                ref_ = withinStaticMethod ? ((Reference)new StaticThisReference(class_)) : new ThisReference(class_);
            }
            else if (symbol is Class class2 && id.text == "super")
                ref_ = new SuperReference(class2);
            else {
                ref_ = symbol.createReference();
                if (ref_ == null)
                    this.errorMan.throw_("createReference() should not return null!");
            }
            ref_.parentNode = id.parentNode;
            return ref_;
        }
        
        protected override Statement visitStatement(Statement stmt)
        {
            if (stmt is ForStatement forStat) {
                this.symbolLookup.pushContext($"For");
                if (forStat.itemVar != null)
                    this.symbolLookup.addSymbol(forStat.itemVar.name, forStat.itemVar);
                base.visitStatement(forStat);
                this.symbolLookup.popContext();
            }
            else if (stmt is ForeachStatement forStat2) {
                this.symbolLookup.pushContext($"Foreach");
                this.symbolLookup.addSymbol(forStat2.itemVar.name, forStat2.itemVar);
                base.visitStatement(forStat2);
                this.symbolLookup.popContext();
            }
            else if (stmt is TryStatement tryStat) {
                this.symbolLookup.pushContext($"Try");
                this.visitBlock(tryStat.tryBody);
                if (tryStat.catchBody != null) {
                    this.symbolLookup.addSymbol(tryStat.catchVar.name, tryStat.catchVar);
                    this.visitBlock(tryStat.catchBody);
                    this.symbolLookup.popContext();
                }
                if (tryStat.finallyBody != null)
                    this.visitBlock(tryStat.finallyBody);
            }
            else
                return base.visitStatement(stmt);
            return stmt;
        }
        
        protected override Lambda visitLambda(Lambda lambda)
        {
            this.symbolLookup.pushContext($"Lambda");
            foreach (var param in lambda.parameters)
                this.symbolLookup.addSymbol(param.name, param);
            base.visitBlock(lambda.body);
            // directly process method's body without opening a new scope again
            this.symbolLookup.popContext();
            return lambda;
        }
        
        protected override Block visitBlock(Block block)
        {
            this.symbolLookup.pushContext("block");
            base.visitBlock(block);
            this.symbolLookup.popContext();
            return block;
        }
        
        protected override VariableDeclaration visitVariableDeclaration(VariableDeclaration stmt)
        {
            this.symbolLookup.addSymbol(stmt.name, stmt);
            return base.visitVariableDeclaration(stmt);
        }
        
        protected override void visitMethodParameter(MethodParameter param)
        {
            this.symbolLookup.addSymbol(param.name, param);
            base.visitMethodParameter(param);
        }
        
        protected override void visitMethodBase(IMethodBase method)
        {
            this.symbolLookup.pushContext(method is Method meth2 ? $"Method: {meth2.name}" : method is Constructor ? "constructor" : "???");
            base.visitMethodBase(method);
            this.symbolLookup.popContext();
        }
        
        protected override void visitClass(Class cls)
        {
            this.symbolLookup.pushContext($"Class: {cls.name}");
            this.symbolLookup.addSymbol("this", cls);
            if (cls.baseClass is ClassType classType)
                this.symbolLookup.addSymbol("super", classType.decl);
            base.visitClass(cls);
            this.symbolLookup.popContext();
        }
        
        public override void visitFile(SourceFile sourceFile)
        {
            this.errorMan.resetContext(this);
            this.symbolLookup.pushContext($"File: {sourceFile.sourcePath.toString()}");
            
            foreach (var symbol in sourceFile.availableSymbols.values()) {
                if (symbol is Class class3)
                    this.symbolLookup.addSymbol(class3.name, class3);
                else if (symbol is Interface) { }
                else if (symbol is Enum_ enum_)
                    this.symbolLookup.addSymbol(enum_.name, enum_);
                else if (symbol is GlobalFunction globFunct)
                    this.symbolLookup.addSymbol(globFunct.name, globFunct);
                else { }
            }
            
            base.visitFile(sourceFile);
            
            this.symbolLookup.popContext();
            this.errorMan.resetContext();
        }
    }
}