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
import OneLang.Template.Nodes.TemplateContext;

public class LiteralNode implements ITemplateNode {
    public String value;
    
    public LiteralNode(String value)
    {
        this.value = value;
    }
    
    public String format(TemplateContext context) {
        return this.value;
    }
}