package OneLang.Template.Nodes;

import OneLang.One.Ast.Expressions.Expression;
import OneLang.Utils.TSOverviewGenerator.TSOverviewGenerator;
import OneLang.VM.ExprVM.ExprVM;
import OneLang.VM.ExprVM.VMContext;
import OneLang.VM.Values.ArrayValue;
import OneLang.VM.Values.StringValue;

import OneLang.Template.Nodes.ITemplateNode;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.stream.Collectors;
import OneLang.VM.ExprVM.VMContext;

public class TemplateBlock implements ITemplateNode {
    public List<ITemplateNode> items;
    
    public TemplateBlock(ITemplateNode[] items)
    {
        this.items = new ArrayList<>(Arrays.asList(items));
    }
    
    public String format(VMContext context) {
        return Arrays.stream(this.items.stream().map(x -> x.format(context)).toArray(String[]::new)).collect(Collectors.joining(""));
    }
}