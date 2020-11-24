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
import OneLang.Template.Nodes.TemplateBlock;
import OneLang.VM.ExprVM.ExprVM;
import OneLang.VM.Values.ArrayValue;
import io.onelang.std.core.Objects;
import OneLang.Template.Nodes.TemplateContext;

public class ForNode implements ITemplateNode {
    public String variableName;
    public Expression itemsExpr;
    public TemplateBlock body;
    public String joiner;
    
    public ForNode(String variableName, Expression itemsExpr, TemplateBlock body, String joiner)
    {
        this.variableName = variableName;
        this.itemsExpr = itemsExpr;
        this.body = body;
        this.joiner = joiner;
    }
    
    public String format(TemplateContext context) {
        var items = new ExprVM(context.model).evaluate(this.itemsExpr);
        if (!(items instanceof ArrayValue))
            throw new Error("ForNode items (" + TSOverviewGenerator.preview.expr(this.itemsExpr) + ") return a non-array result!");
        
        var result = "";
        for (var item : (((ArrayValue)items)).items) {
            if (this.joiner != null && !Objects.equals(result, ""))
                result += this.joiner;
            
            context.model.props.put(this.variableName, item);
            result += this.body.format(context);
        }
        /* unset context.model.props.get(this.variableName); */
        return result;
    }
}