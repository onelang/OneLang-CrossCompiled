using One.Ast;
using System.Collections.Generic;
using System.Linq;

namespace One.Ast
{
    public class Statement : IHasAttributesAndTrivia, IAstNode {
        public string leadingTrivia { get; set; }
        public Dictionary<string, string> attributes { get; set; }
        
        public Statement()
        {
            
        }
    }
    
    public class IfStatement : Statement {
        public Expression condition;
        public Block then;
        public Block else_;
        
        public IfStatement(Expression condition, Block then, Block else_): base()
        {
            this.condition = condition;
            this.then = then;
            this.else_ = else_;
        }
    }
    
    public class ReturnStatement : Statement {
        public Expression expression;
        
        public ReturnStatement(Expression expression): base()
        {
            this.expression = expression;
        }
    }
    
    public class ThrowStatement : Statement {
        public Expression expression;
        
        public ThrowStatement(Expression expression): base()
        {
            this.expression = expression;
        }
    }
    
    public class ExpressionStatement : Statement {
        public Expression expression;
        
        public ExpressionStatement(Expression expression): base()
        {
            this.expression = expression;
        }
    }
    
    public class BreakStatement : Statement {
        
    }
    
    public class ContinueStatement : Statement {
        
    }
    
    public class UnsetStatement : Statement {
        public Expression expression;
        
        public UnsetStatement(Expression expression): base()
        {
            this.expression = expression;
        }
    }
    
    public class VariableDeclaration : Statement, IVariableWithInitializer, IReferencable {
        public string name { get; set; }
        public IType type { get; set; }
        public Expression initializer { get; set; }
        public List<VariableDeclarationReference> references;
        public MutabilityInfo mutability { get; set; }
        
        public VariableDeclaration(string name, IType type, Expression initializer): base()
        {
            this.name = name;
            this.type = type;
            this.initializer = initializer;
            this.references = new List<VariableDeclarationReference>();
        }
        
        public Reference createReference()
        {
            return new VariableDeclarationReference(this);
        }
    }
    
    public class WhileStatement : Statement {
        public Expression condition;
        public Block body;
        
        public WhileStatement(Expression condition, Block body): base()
        {
            this.condition = condition;
            this.body = body;
        }
    }
    
    public class DoStatement : Statement {
        public Expression condition;
        public Block body;
        
        public DoStatement(Expression condition, Block body): base()
        {
            this.condition = condition;
            this.body = body;
        }
    }
    
    public class ForeachVariable : IVariable, IReferencable {
        public string name { get; set; }
        public IType type { get; set; }
        public List<ForeachVariableReference> references;
        public MutabilityInfo mutability { get; set; }
        
        public ForeachVariable(string name)
        {
            this.name = name;
            this.references = new List<ForeachVariableReference>();
        }
        
        public Reference createReference()
        {
            return new ForeachVariableReference(this);
        }
    }
    
    public class ForeachStatement : Statement {
        public ForeachVariable itemVar;
        public Expression items;
        public Block body;
        
        public ForeachStatement(ForeachVariable itemVar, Expression items, Block body): base()
        {
            this.itemVar = itemVar;
            this.items = items;
            this.body = body;
        }
    }
    
    public class ForVariable : IVariableWithInitializer, IReferencable {
        public string name { get; set; }
        public IType type { get; set; }
        public Expression initializer { get; set; }
        public List<ForVariableReference> references;
        public MutabilityInfo mutability { get; set; }
        
        public ForVariable(string name, IType type, Expression initializer)
        {
            this.name = name;
            this.type = type;
            this.initializer = initializer;
            this.references = new List<ForVariableReference>();
        }
        
        public Reference createReference()
        {
            return new ForVariableReference(this);
        }
    }
    
    public class ForStatement : Statement {
        public ForVariable itemVar;
        public Expression condition;
        public Expression incrementor;
        public Block body;
        
        public ForStatement(ForVariable itemVar, Expression condition, Expression incrementor, Block body): base()
        {
            this.itemVar = itemVar;
            this.condition = condition;
            this.incrementor = incrementor;
            this.body = body;
        }
    }
    
    public class CatchVariable : IVariable, IReferencable {
        public string name { get; set; }
        public IType type { get; set; }
        public List<CatchVariableReference> references;
        public MutabilityInfo mutability { get; set; }
        
        public CatchVariable(string name, IType type)
        {
            this.name = name;
            this.type = type;
            this.references = new List<CatchVariableReference>();
        }
        
        public Reference createReference()
        {
            return new CatchVariableReference(this);
        }
    }
    
    public class TryStatement : Statement {
        public Block tryBody;
        public CatchVariable catchVar;
        public Block catchBody;
        public Block finallyBody;
        
        public TryStatement(Block tryBody, CatchVariable catchVar, Block catchBody, Block finallyBody): base()
        {
            this.tryBody = tryBody;
            this.catchVar = catchVar;
            this.catchBody = catchBody;
            this.finallyBody = finallyBody;
            if (this.catchBody == null && this.finallyBody == null)
                throw new Error("try without catch and finally is not allowed");
        }
    }
    
    public class Block {
        public List<Statement> statements;
        
        public Block(Statement[] statements)
        {
            this.statements = statements.ToList();
        }
    }
}