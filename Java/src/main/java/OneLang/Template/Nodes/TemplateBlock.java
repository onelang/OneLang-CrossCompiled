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
import java.util.Arrays;
import java.util.stream.Collectors;
import OneLang.Template.Nodes.TemplateContext;

public class TemplateBlock implements ITemplateNode {
    public ITemplateNode[] items;
    
    public TemplateBlock(ITemplateNode[] items)
    {
        this.items = items;
    }
    
    public String format(TemplateContext context) {
        return Arrays.stream(Arrays.stream(this.items).map(x -> x.format(context)).toArray(String[]::new)).collect(Collectors.joining(""));
    }
}