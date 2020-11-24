package OneLang.VM.ExprVM;

import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.Identifier;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Ast.Expressions.StringLiteral;
import OneLang.One.Ast.Expressions.UnresolvedCallExpression;
import OneLang.VM.Values.ICallableValue;
import OneLang.VM.Values.IVMValue;
import OneLang.VM.Values.ObjectValue;
import OneLang.VM.Values.StringValue;

import OneLang.VM.Values.ObjectValue;
import OneLang.VM.Values.IVMValue;
import OneLang.One.Ast.Expressions.Identifier;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Ast.Expressions.UnresolvedCallExpression;
import OneLang.VM.Values.ICallableValue;
import java.util.Arrays;
import OneLang.One.Ast.Expressions.StringLiteral;
import OneLang.VM.Values.StringValue;
import OneLang.One.Ast.Expressions.Expression;

public class ExprVM {
    public ObjectValue model;
    
    public ExprVM(ObjectValue model)
    {
        this.model = model;
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
            return ExprVM.propAccess(this.model, ((Identifier)expr).text);
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
        else
            throw new Error("Unsupported expression!");
    }
}