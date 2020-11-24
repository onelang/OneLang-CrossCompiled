using One.Ast;
using VM;

namespace VM
{
    public class ExprVM {
        public ObjectValue model;
        
        public ExprVM(ObjectValue model)
        {
            this.model = model;
        }
        
        public static IVMValue propAccess(IVMValue obj, string propName)
        {
            if (!(obj is ObjectValue))
                throw new Error("You can only access a property of an object!");
            if (!((((ObjectValue)obj)).props.hasKey(propName)))
                throw new Error($"Property '{propName}' does not exists on this object!");
            return (((ObjectValue)obj)).props.get(propName);
        }
        
        public IVMValue evaluate(Expression expr)
        {
            if (expr is Identifier ident)
                return ExprVM.propAccess(this.model, ident.text);
            else if (expr is PropertyAccessExpression propAccExpr) {
                var objValue = this.evaluate(propAccExpr.object_);
                return ExprVM.propAccess(objValue, propAccExpr.propertyName);
            }
            else if (expr is UnresolvedCallExpression unrCallExpr) {
                var func = ((ICallableValue)this.evaluate(unrCallExpr.func));
                var args = unrCallExpr.args.map(x => this.evaluate(x));
                var result = func.call(args);
                return result;
            }
            else if (expr is StringLiteral strLit)
                return new StringValue(strLit.stringValue);
            else
                throw new Error("Unsupported expression!");
        }
    }
}