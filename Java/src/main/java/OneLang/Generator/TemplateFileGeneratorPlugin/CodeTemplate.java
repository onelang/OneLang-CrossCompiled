package OneLang.Generator.TemplateFileGeneratorPlugin;

import io.onelang.std.yaml.OneYaml;
import io.onelang.std.yaml.ValueType;
import io.onelang.std.yaml.YamlValue;
import OneLang.Parsers.Common.ExpressionParser.ExpressionParser;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.Identifier;
import OneLang.One.Ast.Expressions.IMethodCallExpression;
import OneLang.One.Ast.Expressions.InstanceMethodCallExpression;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Ast.Expressions.StaticMethodCallExpression;
import OneLang.One.Ast.Expressions.UnresolvedCallExpression;
import OneLang.One.Ast.Interfaces.IExpression;
import OneLang.One.Ast.Statements.Statement;
import OneLang.Generator.IGeneratorPlugin.IGeneratorPlugin;
import OneLang.Parsers.Common.Reader.Reader;
import OneLang.VM.Values.ICallableValue;
import OneLang.VM.Values.IVMValue;
import OneLang.VM.Values.ObjectValue;
import OneLang.One.Ast.References.IInstanceMemberReference;
import OneLang.One.Ast.References.InstanceFieldReference;
import OneLang.One.Ast.References.InstancePropertyReference;
import OneLang.One.Ast.References.StaticFieldReference;
import OneLang.One.Ast.References.StaticPropertyReference;
import OneLang.One.Ast.References.VariableReference;
import OneLang.One.Ast.Types.IClassMember;
import OneLang.Template.TemplateParser.TemplateParser;
import OneLang.Generator.IGenerator.IGenerator;
import OneLang.Template.Nodes.ITemplateFormatHooks;
import OneLang.Template.Nodes.TemplateContext;

public class CodeTemplate {
    public String template;
    public String[] includes;
    
    public CodeTemplate(String template, String[] includes)
    {
        this.template = template;
        this.includes = includes;
    }
}