package OneLang.VM.ExprVM;

import OneLang.One.Ast.Expressions.ConditionalExpression;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.Identifier;
import OneLang.One.Ast.Expressions.NumericLiteral;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Ast.Expressions.StringLiteral;
import OneLang.One.Ast.Expressions.TemplateString;
import OneLang.One.Ast.Expressions.UnresolvedCallExpression;
import OneLang.VM.Values.BooleanValue;
import OneLang.VM.Values.ICallableValue;
import OneLang.VM.Values.IVMValue;
import OneLang.VM.Values.NumericValue;
import OneLang.VM.Values.ObjectValue;
import OneLang.VM.Values.StringValue;

import OneLang.VM.ExprVM.VMContext;
import OneLang.VM.Values.IVMValue;
import OneLang.VM.Values.ObjectValue;
import OneLang.One.Ast.Expressions.Identifier;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Ast.Expressions.UnresolvedCallExpression;
import OneLang.VM.Values.ICallableValue;
import java.util.Arrays;
import OneLang.One.Ast.Expressions.StringLiteral;
import OneLang.VM.Values.StringValue;
import OneLang.One.Ast.Expressions.NumericLiteral;
import OneLang.VM.Values.NumericValue;
import OneLang.One.Ast.Expressions.ConditionalExpression;
import OneLang.VM.Values.BooleanValue;
import OneLang.One.Ast.Expressions.TemplateString;
import OneLang.One.Ast.Expressions.Expression;

public class ExprVM {
    public VMContext context;
    
    public ExprVM(VMContext context)
    {
        this.context = context;
    }
    
    public static IVMValue propAccess(IVMValue obj, String propName) {
        if (!(obj instanceof ObjectValue))
            throw new Error("You can only access a property of an object!");
        if (!((((ObjectValue)obj)).props.containsKey(propName)))
            throw new Error("Property '" + propName + "' does not exists on this object!");
        return (((ObjectValue)obj)).props.get(propName);
    }
    
    public IVMValue evaluate(Expression expr) {
        if (expr instanceof Identifier)
            return ExprVM.propAccess(this.context.model, ((Identifier)expr).text);
        else if (expr instanceof PropertyAccessExpression) {
            var objValue = this.evaluate(((PropertyAccessExpression)expr).object);
            return ExprVM.propAccess(objValue, ((PropertyAccessExpression)expr).propertyName);
        }
        else if (expr instanceof UnresolvedCallExpression) {
            var func = ((ICallableValue)this.evaluate(((UnresolvedCallExpression)expr).func));
            var args = Arrays.stream(((UnresolvedCallExpression)expr).args).map(x -> this.evaluate(x)).toArray(IVMValue[]::new);
            var result = func.call(args);
            return result;
        }
        else if (expr instanceof StringLiteral)
            return new StringValue(((StringLiteral)expr).stringValue);
        else if (expr instanceof NumericLiteral)
            return new NumericValue(Integer.parseInt(((NumericLiteral)expr).valueAsText));
        else if (expr instanceof ConditionalExpression) {
            var condResult = this.evaluate(((ConditionalExpression)expr).condition);
            var result = this.evaluate((((BooleanValue)condResult)).value ? ((ConditionalExpression)expr).whenTrue : ((ConditionalExpression)expr).whenFalse);
            return result;
        }
        else if (expr instanceof TemplateString) {
            var result = "";
            for (var part : ((TemplateString)expr).parts) {
                if (part.isLiteral)
                    result += part.literalText;
                else {
                    var value = this.evaluate(part.expression);
                    result += value instanceof StringValue ? ((StringValue)value).value : this.context.hooks.stringifyValue(value);
                }
            }
            return new StringValue(result);
        }
        else
            throw new Error("Unsupported expression!");
    }
}