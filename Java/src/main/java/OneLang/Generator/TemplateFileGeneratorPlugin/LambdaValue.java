package OneLang.Generator.TemplateFileGeneratorPlugin;

import io.onelang.std.yaml.OneYaml;
import io.onelang.std.yaml.ValueType;
import io.onelang.std.yaml.YamlValue;
import OneLang.Parsers.Common.ExpressionParser.ExpressionParser;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.GlobalFunctionCallExpression;
import OneLang.One.Ast.Expressions.ICallExpression;
import OneLang.One.Ast.Expressions.Identifier;
import OneLang.One.Ast.Expressions.IMethodCallExpression;
import OneLang.One.Ast.Expressions.InstanceMethodCallExpression;
import OneLang.One.Ast.Expressions.NewExpression;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Ast.Expressions.StaticMethodCallExpression;
import OneLang.One.Ast.Expressions.UnresolvedCallExpression;
import OneLang.One.Ast.Expressions.UnresolvedNewExpression;
import OneLang.One.Ast.Interfaces.IExpression;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.Statements.Statement;
import OneLang.Generator.IGeneratorPlugin.IGeneratorPlugin;
import OneLang.Parsers.Common.Reader.Reader;
import OneLang.VM.Values.BooleanValue;
import OneLang.VM.Values.ICallableValue;
import OneLang.VM.Values.IVMValue;
import OneLang.VM.Values.ObjectValue;
import OneLang.VM.Values.StringValue;
import OneLang.One.Ast.References.IInstanceMemberReference;
import OneLang.One.Ast.References.InstanceFieldReference;
import OneLang.One.Ast.References.InstancePropertyReference;
import OneLang.One.Ast.References.StaticFieldReference;
import OneLang.One.Ast.References.StaticPropertyReference;
import OneLang.One.Ast.References.VariableReference;
import OneLang.One.Ast.Types.IClassMember;
import OneLang.Template.TemplateParser.TemplateParser;
import OneLang.Generator.IGenerator.IGenerator;
import OneLang.VM.ExprVM.ExprVM;
import OneLang.VM.ExprVM.IVMHooks;
import OneLang.VM.ExprVM.VMContext;
import OneLang.Parsers.TypeScriptParser.TypeScriptParser2;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.TypeHelper;
import OneLang.One.Ast.AstTypes.UnresolvedType;

import OneLang.VM.Values.ICallableValue;
import OneLang.VM.Values.IVMValue;
import java.util.function.Function;

public class LambdaValue implements ICallableValue {
    public Function<IVMValue[], IVMValue> callback;
    
    public LambdaValue(Function<IVMValue[], IVMValue> callback)
    {
        this.callback = callback;
    }
    
    public Boolean equals(IVMValue other) {
        return false;
    }
    
    public IVMValue call(IVMValue[] args) {
        return this.callback.apply(args);
    }
}