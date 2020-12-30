using One.Ast;
using VM;

namespace VM
{
    public interface IVMHooks {
        IVMValue propAccess(IVMValue obj, string propName);
        
        string stringifyValue(IVMValue value);
    }
    
    public class VMContext
    {
        public ObjectValue model;
        public IVMHooks hooks;
        
        public VMContext(ObjectValue model, IVMHooks hooks = null)
        {
            this.model = model;
            this.hooks = hooks;
        }
    }
    
    public class ExprVM
    {
        public VMContext context;
        
        public ExprVM(VMContext context)
        {
            this.context = context;
        }
        
        public IVMValue propAccess(IVMValue obj, string propName)
        {
            if (this.context.hooks != null) {
                var value = this.context.hooks.propAccess(obj, propName);
                if (value != null)
                    return value;
            }
            
            if (!(obj is ObjectValue))
                throw new Error("You can only access a property of an object!");
            if (!((((ObjectValue)obj)).props.hasKey(propName)))
                throw new Error($"Property '{propName}' does not exists on this object!");
            return (((ObjectValue)obj)).props.get(propName);
        }
        
        public IVMValue evaluate(Expression expr)
        {
            if (expr is Identifier ident)
                return this.propAccess(this.context.model, ident.text);
            else if (expr is PropertyAccessExpression propAccExpr) {
                var objValue = this.evaluate(propAccExpr.object_);
                return this.propAccess(objValue, propAccExpr.propertyName);
            }
            else if (expr is UnresolvedCallExpression unrCallExpr) {
                var func = ((ICallableValue)this.evaluate(unrCallExpr.func));
                var args = unrCallExpr.args.map(x => this.evaluate(x));
                var result = func.call(args);
                return result;
            }
            else if (expr is StringLiteral strLit)
                return new StringValue(strLit.stringValue);
            else if (expr is NumericLiteral numLit)
                return new NumericValue(Global.parseInt(numLit.valueAsText));
            else if (expr is ConditionalExpression condExpr) {
                var condResult = this.evaluate(condExpr.condition);
                var result = this.evaluate((((BooleanValue)condResult)).value ? condExpr.whenTrue : condExpr.whenFalse);
                return result;
            }
            else if (expr is TemplateString templStr) {
                var result = "";
                foreach (var part in templStr.parts) {
                    if (part.isLiteral)
                        result += part.literalText;
                    else {
                        var value = this.evaluate(part.expression);
                        result += value is StringValue strValue ? strValue.value : this.context.hooks.stringifyValue(value);
                    }
                }
                return new StringValue(result);
            }
            else if (expr is BinaryExpression binExpr) {
                var left = this.evaluate(binExpr.left);
                var right = this.evaluate(binExpr.right);
                if (binExpr.operator_ == "==" || binExpr.operator_ == "===")
                    return new BooleanValue(left.equals(right));
                else if (binExpr.operator_ == "!=" || binExpr.operator_ == "!==")
                    return new BooleanValue(!left.equals(right));
                else
                    throw new Error($"Unsupported binary operator: {binExpr.operator_}");
            }
            else
                throw new Error("Unsupported expression!");
        }
    }
}