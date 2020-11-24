package OneLang.Template.Nodes;

import OneLang.Generator.TemplateFileGeneratorPlugin.ExpressionValue;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.StringLiteral;
import OneLang.Utils.TSOverviewGenerator.TSOverviewGenerator;
import OneLang.VM.ExprVM.ExprVM;
import OneLang.VM.Values.ArrayValue;
import OneLang.VM.Values.IVMValue;
import OneLang.VM.Values.ObjectValue;
import OneLang.VM.Values.StringValue;

import OneLang.Template.Nodes.ITemplateNode;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.VM.ExprVM.ExprVM;
import OneLang.VM.Values.StringValue;
import OneLang.Template.Nodes.TemplateContext;

public class ExpressionNode implements ITemplateNode {
    public Expression expr;
    
    public ExpressionNode(Expression expr)
    {
        this.expr = expr;
    }
    
    public String format(TemplateContext context) {
        var value = new ExprVM(context.model).evaluate(this.expr);
        if (value instanceof StringValue)
            return ((StringValue)value).value;
        
        if (context.hooks != null) {
            var result = context.hooks.formatValue(value);
            if (result != null)
                return result;
        }
        
        throw new Error("ExpressionNode (" + TSOverviewGenerator.preview.expr(this.expr) + ") return a non-string result!");
    }
}