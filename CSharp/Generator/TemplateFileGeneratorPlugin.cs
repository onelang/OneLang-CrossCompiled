using Generator;
using One.Ast;
using Parsers.Common;
using System.Collections.Generic;
using System;
using Template;
using VM;

namespace Generator
{
    public class CodeTemplate {
        public string template;
        public string[] includes;
        
        public CodeTemplate(string template, string[] includes)
        {
            this.template = template;
            this.includes = includes;
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
    }
    
    public class TypeValue : IVMValue {
        public IType type;
        
        public TypeValue(IType type)
        {
            this.type = type;
        }
    }
    
    public class LambdaValue : ICallableValue {
        public Func<IVMValue[], IVMValue> callback;
        
        public LambdaValue(Func<IVMValue[], IVMValue> callback)
        {
            this.callback = callback;
        }
        
        public IVMValue call(IVMValue[] args)
        {
            return this.callback(args);
        }
    }
    
    public class TemplateFileGeneratorPlugin : IGeneratorPlugin, IVMHooks {
        public Dictionary<string, CallTemplate> methods;
        public Dictionary<string, FieldAccessTemplate> fields;
        public Dictionary<string, IVMValue> modelGlobals;
        public IGenerator generator;
        
        public TemplateFileGeneratorPlugin(IGenerator generator, string templateYaml)
        {
            this.generator = generator;
            this.methods = new Dictionary<string, CallTemplate> {};
            this.fields = new Dictionary<string, FieldAccessTemplate> {};
            this.modelGlobals = new Dictionary<string, IVMValue> {};
            var root = OneYaml.load(templateYaml);
            var exprDict = root.dict("expressions");
            
            foreach (var exprStr in Object.keys(exprDict)) {
                var val = exprDict.get(exprStr);
                var tmpl = val.type() == ValueType.String ? new CodeTemplate(val.asStr(), new string[0]) : new CodeTemplate(val.str("template"), val.strArr("includes"));
                
                this.addExprTemplate(exprStr, tmpl);
            }
        }
        
        public string stringifyValue(IVMValue value)
        {
            if (value is ExpressionValue exprValue) {
                var result = this.generator.expr(exprValue.value);
                return result;
            }
            return null;
        }
        
        public void addExprTemplate(string exprStr, CodeTemplate tmpl)
        {
            var expr = new ExpressionParser(new Reader(exprStr)).parse();
            if (expr is UnresolvedCallExpression unrCallExpr && unrCallExpr.func is PropertyAccessExpression propAccExpr && propAccExpr.object_ is Identifier ident) {
                var callTmpl = new CallTemplate(ident.text, propAccExpr.propertyName, unrCallExpr.args.map(x => (((Identifier)x)).text), tmpl);
                this.methods.set($"{callTmpl.className}.{callTmpl.methodName}@{callTmpl.args.length()}", callTmpl);
            }
            else if (expr is UnresolvedCallExpression unrCallExpr2 && unrCallExpr2.func is Identifier ident2) {
                var callTmpl = new CallTemplate(null, ident2.text, unrCallExpr2.args.map(x => (((Identifier)x)).text), tmpl);
                this.methods.set($"{callTmpl.methodName}@{callTmpl.args.length()}", callTmpl);
            }
            else if (expr is PropertyAccessExpression propAccExpr2 && propAccExpr2.object_ is Identifier ident3) {
                var fieldTmpl = new FieldAccessTemplate(ident3.text, propAccExpr2.propertyName, tmpl);
                this.fields.set($"{fieldTmpl.className}.{fieldTmpl.fieldName}", fieldTmpl);
            }
            else
                throw new Error($"This expression template format is not supported: '{exprStr}'");
        }
        
        public string expr(IExpression expr)
        {
            CodeTemplate codeTmpl = null;
            var model = new Dictionary<string, IVMValue> {};
            
            if (expr is StaticMethodCallExpression statMethCallExpr || expr is InstanceMethodCallExpression || expr is GlobalFunctionCallExpression) {
                var call = ((ICallExpression)expr);
                var parentIntf = call.getParentInterface();
                var methodName = $"{(parentIntf == null ? "" : $"{parentIntf.name}.")}{call.getName()}@{call.args.length()}";
                var callTmpl = this.methods.get(methodName);
                if (callTmpl == null)
                    return null;
                
                if (expr is InstanceMethodCallExpression instMethCallExpr)
                    model.set("this", new ExpressionValue(instMethCallExpr.object_));
                for (int i = 0; i < callTmpl.args.length(); i++)
                    model.set(callTmpl.args.get(i), new ExpressionValue(call.args.get(i)));
                codeTmpl = callTmpl.template;
            }
            else if (expr is StaticFieldReference statFieldRef || expr is StaticPropertyReference || expr is InstanceFieldReference || expr is InstancePropertyReference) {
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
            
            model.set("type", new TypeValue(expr.getType()));
            foreach (var name in Object.keys(this.modelGlobals))
                model.set(name, this.modelGlobals.get(name));
            
            foreach (var inc in codeTmpl.includes ?? new string[0])
                this.generator.addInclude(inc);
            
            var tmpl = new TemplateParser(codeTmpl.template).parse();
            var result = tmpl.format(new VMContext(new ObjectValue(model), this));
            return result;
        }
        
        public string stmt(Statement stmt)
        {
            return null;
        }
    }
}