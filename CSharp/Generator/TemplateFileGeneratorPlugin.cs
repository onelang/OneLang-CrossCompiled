using Generator;
using One.Ast;
using Parsers.Common;
using Parsers;
using System.Collections.Generic;
using System;
using Template;
using VM;

namespace Generator
{
    public class CodeTemplate {
        public string template;
        public string[] includes;
        public Expression ifExpr;
        
        public CodeTemplate(string template, string[] includes, Expression ifExpr)
        {
            this.template = template;
            this.includes = includes;
            this.ifExpr = ifExpr;
        }
    }
    
    public class CallTemplate {
        public string className;
        public string methodName;
        public string[] args;
        public CodeTemplate template;
        
        public CallTemplate(string className, string methodName, string[] args, CodeTemplate template)
        {
            this.className = className;
            this.methodName = methodName;
            this.args = args;
            this.template = template;
        }
    }
    
    public class FieldAccessTemplate {
        public string className;
        public string fieldName;
        public CodeTemplate template;
        
        public FieldAccessTemplate(string className, string fieldName, CodeTemplate template)
        {
            this.className = className;
            this.fieldName = fieldName;
            this.template = template;
        }
    }
    
    public class ExpressionValue : IVMValue {
        public Expression value;
        
        public ExpressionValue(Expression value)
        {
            this.value = value;
        }
        
        public bool equals(IVMValue other)
        {
            return other is ExpressionValue exprValue && exprValue.value == this.value;
        }
    }
    
    public class TypeValue : IVMValue {
        public IType type;
        
        public TypeValue(IType type)
        {
            this.type = type;
        }
        
        public bool equals(IVMValue other)
        {
            return other is TypeValue typeValue && TypeHelper.equals(typeValue.type, this.type);
        }
    }
    
    public class LambdaValue : ICallableValue {
        public Func<IVMValue[], IVMValue> callback;
        
        public LambdaValue(Func<IVMValue[], IVMValue> callback)
        {
            this.callback = callback;
        }
        
        public bool equals(IVMValue other)
        {
            return false;
        }
        
        public IVMValue call(IVMValue[] args)
        {
            return this.callback(args);
        }
    }
    
    public class TemplateFileGeneratorPlugin : IGeneratorPlugin, IVMHooks {
        public Dictionary<string, List<CallTemplate>> methods;
        public Dictionary<string, FieldAccessTemplate> fields;
        public Dictionary<string, IVMValue> modelGlobals;
        public IGenerator generator;
        
        public TemplateFileGeneratorPlugin(IGenerator generator, string templateYaml)
        {
            this.generator = generator;
            this.methods = new Dictionary<string, List<CallTemplate>> {};
            this.fields = new Dictionary<string, FieldAccessTemplate> {};
            this.modelGlobals = new Dictionary<string, IVMValue> {};
            var root = OneYaml.load(templateYaml);
            var exprDict = root.dict("expressions");
            
            foreach (var exprStr in Object.keys(exprDict)) {
                var val = exprDict.get(exprStr);
                var ifStr = val.str("if");
                var ifExpr = ifStr == null ? null : new TypeScriptParser2(ifStr, null).parseExpression();
                var tmpl = val.type() == ValueType.String ? new CodeTemplate(val.asStr(), new string[0], null) : new CodeTemplate(val.str("template"), val.strArr("includes"), ifExpr);
                
                this.addExprTemplate(exprStr, tmpl);
            }
        }
        
        public IVMValue propAccess(IVMValue obj, string propName)
        {
            if (obj is ExpressionValue exprValue2 && propName == "type")
                return new TypeValue(exprValue2.value.getType());
            if (obj is TypeValue typeValue2 && propName == "name" && typeValue2.type is ClassType classType)
                return new StringValue(classType.decl.name);
            return null;
        }
        
        public string stringifyValue(IVMValue value)
        {
            if (value is ExpressionValue exprValue3) {
                var result = this.generator.expr(exprValue3.value);
                return result;
            }
            return null;
        }
        
        public void addMethod(string name, CallTemplate callTmpl)
        {
            if (!(this.methods.hasKey(name)))
                this.methods.set(name, new List<CallTemplate>());
            this.methods.get(name).push(callTmpl);
        }
        
        public void addExprTemplate(string exprStr, CodeTemplate tmpl)
        {
            var expr = new TypeScriptParser2(exprStr, null).parseExpression();
            if (expr is UnresolvedCallExpression unrCallExpr && unrCallExpr.func is PropertyAccessExpression propAccExpr && propAccExpr.object_ is Identifier ident) {
                var callTmpl = new CallTemplate(ident.text, propAccExpr.propertyName, unrCallExpr.args.map(x => (((Identifier)x)).text), tmpl);
                this.addMethod($"{callTmpl.className}.{callTmpl.methodName}@{callTmpl.args.length()}", callTmpl);
            }
            else if (expr is UnresolvedCallExpression unrCallExpr2 && unrCallExpr2.func is Identifier ident2) {
                var callTmpl = new CallTemplate(null, ident2.text, unrCallExpr2.args.map(x => (((Identifier)x)).text), tmpl);
                this.addMethod($"{callTmpl.methodName}@{callTmpl.args.length()}", callTmpl);
            }
            else if (expr is PropertyAccessExpression propAccExpr2 && propAccExpr2.object_ is Identifier ident3) {
                var fieldTmpl = new FieldAccessTemplate(ident3.text, propAccExpr2.propertyName, tmpl);
                this.fields.set($"{fieldTmpl.className}.{fieldTmpl.fieldName}", fieldTmpl);
            }
            else if (expr is UnresolvedNewExpression unrNewExpr && unrNewExpr.cls is UnresolvedType unrType) {
                var callTmpl = new CallTemplate(unrType.typeName, "constructor", unrNewExpr.args.map(x => (((Identifier)x)).text), tmpl);
                this.addMethod($"{callTmpl.className}.{callTmpl.methodName}@{callTmpl.args.length()}", callTmpl);
            }
            else
                throw new Error($"This expression template format is not supported: '{exprStr}'");
        }
        
        public string expr(IExpression expr)
        {
            var isCallExpr = expr is StaticMethodCallExpression statMethCallExpr || expr is InstanceMethodCallExpression || expr is GlobalFunctionCallExpression || expr is NewExpression;
            var isFieldRef = expr is StaticFieldReference statFieldRef || expr is StaticPropertyReference || expr is InstanceFieldReference || expr is InstancePropertyReference;
            
            if (!isCallExpr && !isFieldRef)
                return null;
            // quick return
            
            CodeTemplate codeTmpl = null;
            var model = new Dictionary<string, IVMValue> {};
            var context = new VMContext(new ObjectValue(model), this);
            
            model.set("type", new TypeValue(expr.getType()));
            foreach (var name in Object.keys(this.modelGlobals))
                model.set(name, this.modelGlobals.get(name));
            
            if (isCallExpr) {
                var call = ((ICallExpression)expr);
                var parentIntf = call.getParentInterface();
                var methodName = $"{(parentIntf == null ? "" : $"{parentIntf.name}.")}{call.getMethodName()}@{call.args.length()}";
                var callTmpls = this.methods.get(methodName);
                if (callTmpls == null)
                    return null;
                
                foreach (var callTmpl in callTmpls) {
                    if (expr is InstanceMethodCallExpression instMethCallExpr)
                        model.set("this", new ExpressionValue(instMethCallExpr.object_));
                    for (int i = 0; i < callTmpl.args.length(); i++)
                        model.set(callTmpl.args.get(i), new ExpressionValue(call.args.get(i)));
                    
                    if (callTmpl.template.ifExpr == null || (((BooleanValue)new ExprVM(context).evaluate(callTmpl.template.ifExpr))).value) {
                        codeTmpl = callTmpl.template;
                        break;
                    }
                }
            }
            else if (isFieldRef) {
                var cm = ((IClassMember)((object)(((VariableReference)expr)).getVariable()));
                var field = this.fields.get($"{cm.getParentInterface().name}.{cm.name}");
                if (field == null)
                    return null;
                
                if (expr is InstanceFieldReference instFieldRef || expr is InstancePropertyReference)
                    model.set("this", new ExpressionValue((((IInstanceMemberReference)expr)).object_));
                codeTmpl = field.template;
            }
            else
                return null;
            
            if (codeTmpl == null)
                return null;
            
            foreach (var inc in codeTmpl.includes ?? new string[0])
                this.generator.addInclude(inc);
            
            var tmpl = new TemplateParser(codeTmpl.template).parse();
            var result = tmpl.format(context);
            return result;
        }
        
        public string stmt(Statement stmt)
        {
            return null;
        }
    }
}