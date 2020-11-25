package OneLang.VM.ExprVM;

import OneLang.One.Ast.Expressions.ConditionalExpression;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.Identifier;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Ast.Expressions.StringLiteral;
import OneLang.One.Ast.Expressions.TemplateString;
import OneLang.One.Ast.Expressions.UnresolvedCallExpression;
import OneLang.VM.Values.BooleanValue;
import OneLang.VM.Values.ICallableValue;
import OneLang.VM.Values.IVMValue;
import OneLang.VM.Values.ObjectValue;
import OneLang.VM.Values.StringValue;

import OneLang.VM.Values.ObjectValue;
import OneLang.VM.ExprVM.IVMHooks;

public class VMContext {
    public ObjectValue model;
    public IVMHooks hooks;
    
    public VMContext(ObjectValue model, IVMHooks hooks)
    {
        this.model = model;
        this.hooks = hooks;
    }
}