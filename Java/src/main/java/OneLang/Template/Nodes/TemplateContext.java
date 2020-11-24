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

import OneLang.VM.Values.ObjectValue;
import OneLang.Template.Nodes.ITemplateFormatHooks;

public class TemplateContext {
    public ObjectValue model;
    public ITemplateFormatHooks hooks;
    
    public TemplateContext(ObjectValue model, ITemplateFormatHooks hooks)
    {
        this.model = model;
        this.hooks = hooks;
    }
}