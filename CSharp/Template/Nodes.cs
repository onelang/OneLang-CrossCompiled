using Generator;
using One.Ast;
using Utils;
using VM;

namespace Template
{
    public interface ITemplateNode {
        string format(TemplateContext context);
    }
    
    public interface ITemplateFormatHooks {
        string formatValue(IVMValue value);
    }
    
    public class TemplateContext {
        public ObjectValue model;
        public ITemplateFormatHooks hooks;
        
        public TemplateContext(ObjectValue model, ITemplateFormatHooks hooks = null)
        {
            this.model = model;
            this.hooks = hooks;
        }
    }
    
    public class TemplateBlock : ITemplateNode {
        public ITemplateNode[] items;
        
        public TemplateBlock(ITemplateNode[] items)
        {
            this.items = items;
        }
        
        public string format(TemplateContext context)
        {
            return this.items.map(x => x.format(context)).join("");
        }
    }
    
    public class LiteralNode : ITemplateNode {
        public string value;
        
        public LiteralNode(string value)
        {
            this.value = value;
        }
        
        public string format(TemplateContext context)
        {
            return this.value;
        }
    }
    
    public class ExpressionNode : ITemplateNode {
        public Expression expr;
        
        public ExpressionNode(Expression expr)
        {
            this.expr = expr;
        }
        
        public string format(TemplateContext context)
        {
            var value = new ExprVM(context.model).evaluate(this.expr);
            if (value is StringValue strValue)
                return strValue.value;
            
            if (context.hooks != null) {
                var result = context.hooks.formatValue(value);
                if (result != null)
                    return result;
            }
            
            throw new Error($"ExpressionNode ({TSOverviewGenerator.preview.expr(this.expr)}) return a non-string result!");
        }
    }
    
    public class ForNode : ITemplateNode {
        public string variableName;
        public Expression itemsExpr;
        public TemplateBlock body;
        public string joiner;
        
        public ForNode(string variableName, Expression itemsExpr, TemplateBlock body, string joiner)
        {
            this.variableName = variableName;
            this.itemsExpr = itemsExpr;
            this.body = body;
            this.joiner = joiner;
        }
        
        public string format(TemplateContext context)
        {
            var items = new ExprVM(context.model).evaluate(this.itemsExpr);
            if (!(items is ArrayValue))
                throw new Error($"ForNode items ({TSOverviewGenerator.preview.expr(this.itemsExpr)}) return a non-array result!");
            
            var result = "";
            foreach (var item in (((ArrayValue)items)).items) {
                if (this.joiner != null && result != "")
                    result += this.joiner;
                
                context.model.props.set(this.variableName, item);
                result += this.body.format(context);
            }
            /* unset context.model.props.get(this.variableName); */
            return result;
        }
    }
}