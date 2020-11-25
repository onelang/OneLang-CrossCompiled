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
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Ast.Expressions.StaticMethodCallExpression;
import OneLang.One.Ast.Expressions.UnresolvedCallExpression;
import OneLang.One.Ast.Interfaces.IExpression;
import OneLang.One.Ast.Interfaces.IType;
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
import OneLang.VM.ExprVM.IVMHooks;
import OneLang.VM.ExprVM.VMContext;

import OneLang.Generator.IGeneratorPlugin.IGeneratorPlugin;
import OneLang.VM.ExprVM.IVMHooks;
import OneLang.Generator.TemplateFileGeneratorPlugin.CallTemplate;
import java.util.Map;
import OneLang.Generator.TemplateFileGeneratorPlugin.FieldAccessTemplate;
import OneLang.VM.Values.IVMValue;
import OneLang.Generator.IGenerator.IGenerator;
import java.util.LinkedHashMap;
import OneLang.Generator.TemplateFileGeneratorPlugin.CodeTemplate;
import OneLang.Generator.TemplateFileGeneratorPlugin.ExpressionValue;
import OneLang.Parsers.Common.ExpressionParser.ExpressionParser;
import OneLang.Parsers.Common.Reader.Reader;
import OneLang.One.Ast.Expressions.UnresolvedCallExpression;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Ast.Expressions.Identifier;
import java.util.Arrays;
import OneLang.One.Ast.Expressions.StaticMethodCallExpression;
import OneLang.One.Ast.Expressions.InstanceMethodCallExpression;
import OneLang.One.Ast.Expressions.GlobalFunctionCallExpression;
import OneLang.One.Ast.Expressions.ICallExpression;
import OneLang.One.Ast.References.StaticFieldReference;
import OneLang.One.Ast.References.StaticPropertyReference;
import OneLang.One.Ast.References.InstanceFieldReference;
import OneLang.One.Ast.References.InstancePropertyReference;
import OneLang.One.Ast.Types.IClassMember;
import OneLang.One.Ast.References.VariableReference;
import OneLang.One.Ast.References.IInstanceMemberReference;
import OneLang.Generator.TemplateFileGeneratorPlugin.TypeValue;
import OneLang.Template.TemplateParser.TemplateParser;
import OneLang.VM.ExprVM.VMContext;
import OneLang.VM.Values.ObjectValue;
import OneLang.One.Ast.Interfaces.IExpression;
import OneLang.One.Ast.Statements.Statement;

public class TemplateFileGeneratorPlugin implements IGeneratorPlugin, IVMHooks {
    public Map<String, CallTemplate> methods;
    public Map<String, FieldAccessTemplate> fields;
    public Map<String, IVMValue> modelGlobals;
    public IGenerator generator;
    
    public TemplateFileGeneratorPlugin(IGenerator generator, String templateYaml)
    {
        this.generator = generator;
        this.methods = new LinkedHashMap<String, CallTemplate>();
        this.fields = new LinkedHashMap<String, FieldAccessTemplate>();
        this.modelGlobals = new LinkedHashMap<String, IVMValue>();
        var root = OneYaml.load(templateYaml);
        var exprDict = root.dict("expressions");
        
        for (var exprStr : exprDict.keySet().toArray(String[]::new)) {
            var val = exprDict.get(exprStr);
            var tmpl = val.type() == ValueType.String ? new CodeTemplate(val.asStr(), new String[0]) : new CodeTemplate(val.str("template"), val.strArr("includes"));
            
            this.addExprTemplate(exprStr, tmpl);
        }
    }
    
    public String stringifyValue(IVMValue value) {
        if (value instanceof ExpressionValue) {
            var result = this.generator.expr(((ExpressionValue)value).value);
            return result;
        }
        return null;
    }
    
    public void addExprTemplate(String exprStr, CodeTemplate tmpl) {
        var expr = new ExpressionParser(new Reader(exprStr), null, null, null).parse(0, true);
        if (expr instanceof UnresolvedCallExpression && ((UnresolvedCallExpression)expr).func instanceof PropertyAccessExpression && ((PropertyAccessExpression)((UnresolvedCallExpression)expr).func).object instanceof Identifier) {
            var callTmpl = new CallTemplate(((Identifier)((PropertyAccessExpression)((UnresolvedCallExpression)expr).func).object).text, ((PropertyAccessExpression)((UnresolvedCallExpression)expr).func).propertyName, Arrays.stream(((UnresolvedCallExpression)expr).args).map(x -> (((Identifier)x)).text).toArray(String[]::new), tmpl);
            this.methods.put(callTmpl.className + "." + callTmpl.methodName + "@" + callTmpl.args.length, callTmpl);
        }
        else if (expr instanceof UnresolvedCallExpression && ((UnresolvedCallExpression)expr).func instanceof Identifier) {
            var callTmpl = new CallTemplate(null, ((Identifier)((UnresolvedCallExpression)expr).func).text, Arrays.stream(((UnresolvedCallExpression)expr).args).map(x -> (((Identifier)x)).text).toArray(String[]::new), tmpl);
            this.methods.put(callTmpl.methodName + "@" + callTmpl.args.length, callTmpl);
        }
        else if (expr instanceof PropertyAccessExpression && ((PropertyAccessExpression)expr).object instanceof Identifier) {
            var fieldTmpl = new FieldAccessTemplate(((Identifier)((PropertyAccessExpression)expr).object).text, ((PropertyAccessExpression)expr).propertyName, tmpl);
            this.fields.put(fieldTmpl.className + "." + fieldTmpl.fieldName, fieldTmpl);
        }
        else
            throw new Error("This expression template format is not supported: '" + exprStr + "'");
    }
    
    public String expr(IExpression expr) {
        CodeTemplate codeTmpl = null;
        var model = new LinkedHashMap<String, IVMValue>();
        
        if (expr instanceof StaticMethodCallExpression || expr instanceof InstanceMethodCallExpression || expr instanceof GlobalFunctionCallExpression) {
            var call = ((ICallExpression)expr);
            var parentIntf = call.getParentInterface();
            var methodName = (parentIntf == null ? "" : parentIntf.getName() + ".") + call.getName() + "@" + call.getArgs().length;
            var callTmpl = this.methods.get(methodName);
            if (callTmpl == null)
                return null;
            
            if (expr instanceof InstanceMethodCallExpression)
                model.put("this", new ExpressionValue(((InstanceMethodCallExpression)expr).object));
            for (Integer i = 0; i < callTmpl.args.length; i++)
                model.put(callTmpl.args[i], new ExpressionValue(call.getArgs()[i]));
            codeTmpl = callTmpl.template;
        }
        else if (expr instanceof StaticFieldReference || expr instanceof StaticPropertyReference || expr instanceof InstanceFieldReference || expr instanceof InstancePropertyReference) {
            var cm = ((IClassMember)((Object)(((VariableReference)expr)).getVariable()));
            var field = this.fields.get(cm.getParentInterface().getName() + "." + cm.getName());
            if (field == null)
                return null;
            
            if (expr instanceof InstanceFieldReference || expr instanceof InstancePropertyReference)
                model.put("this", new ExpressionValue((((IInstanceMemberReference)expr)).getObject()));
            codeTmpl = field.template;
        }
        else
            return null;
        
        model.put("type", new TypeValue(expr.getType()));
        for (var name : this.modelGlobals.keySet().toArray(String[]::new))
            model.put(name, this.modelGlobals.get(name));
        
        for (var inc : codeTmpl.includes != null ? codeTmpl.includes : new String[0])
            this.generator.addInclude(inc);
        
        var tmpl = new TemplateParser(codeTmpl.template).parse();
        var result = tmpl.format(new VMContext(new ObjectValue(model), this));
        return result;
    }
    
    public String stmt(Statement stmt) {
        return null;
    }
}