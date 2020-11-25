package OneLang.Template.Nodes;

import OneLang.One.Ast.Expressions.Expression;
import OneLang.Utils.TSOverviewGenerator.TSOverviewGenerator;
import OneLang.VM.ExprVM.ExprVM;
import OneLang.VM.ExprVM.VMContext;
import OneLang.VM.Values.ArrayValue;
import OneLang.VM.Values.StringValue;

import OneLang.Template.Nodes.ITemplateNode;
import OneLang.VM.ExprVM.VMContext;

public class LiteralNode implements ITemplateNode {
    public String value;
    
    public LiteralNode(String value)
    {
        this.value = value;
    }
    
    public String format(VMContext context) {
        return this.value;
    }
}