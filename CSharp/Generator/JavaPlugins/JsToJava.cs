using Generator;
using One.Ast;
using System.Collections.Generic;

namespace Generator.JavaPlugins
{
    public class JsToJava : IGeneratorPlugin {
        public Set<string> unhandledMethods;
        public JavaGenerator main;
        
        public JsToJava(JavaGenerator main)
        {
            this.main = main;
            this.unhandledMethods = new Set<string>();
        }
        
        public bool isArray(Expression arrayExpr)
        {
            // TODO: InstanceMethodCallExpression is a hack, we should introduce real stream handling
            return arrayExpr is VariableReference varRef && !varRef.getVariable().mutability.mutated || arrayExpr is StaticMethodCallExpression || arrayExpr is InstanceMethodCallExpression;
        }
        
        public string arrayStream(Expression arrayExpr)
        {
            var isArray = this.isArray(arrayExpr);
            var objR = this.main.expr(arrayExpr);
            if (isArray)
                this.main.imports.add("java.util.Arrays");
            return isArray ? $"Arrays.stream({objR})" : $"{objR}.stream()";
        }
        
        public string toArray(IType arrayType, int typeArgIdx = 0)
        {
            var type = (((ClassType)arrayType)).typeArguments.get(typeArgIdx);
            return $"toArray({this.main.type(type)}[]::new)";
        }
        
        public string convertMethod(Class cls, Expression obj, Method method, Expression[] args, IType returnType)
        {
            var objR = obj == null ? null : this.main.expr(obj);
            var argsR = args.map(x => this.main.expr(x));
            if (cls.name == "TsString") {
                if (method.name == "replace") {
                    if (args.get(0) is RegexLiteral) {
                        this.main.imports.add("java.util.regex.Pattern");
                        return $"{objR}.replaceAll({JSON.stringify((((RegexLiteral)args.get(0))).pattern)}, {argsR.get(1)})";
                    }
                    
                    return $"{argsR.get(0)}.replace({objR}, {argsR.get(1)})";
                }
            }
            else if (new List<string> { "console", "RegExp" }.includes(cls.name)) {
                this.main.imports.add($"io.onelang.std.core.{cls.name}");
                return null;
            }
            else if (new List<string> { "JSON" }.includes(cls.name)) {
                this.main.imports.add($"io.onelang.std.json.{cls.name}");
                return null;
            }
            else
                return null;
            
            return null;
        }
        
        public string expr(IExpression expr)
        {
            if (expr is InstanceMethodCallExpression instMethCallExpr && instMethCallExpr.object_.actualType is ClassType classType)
                return this.convertMethod(classType.decl, instMethCallExpr.object_, instMethCallExpr.method, instMethCallExpr.args, instMethCallExpr.actualType);
            else if (expr is InstancePropertyReference instPropRef && instPropRef.object_.actualType is ClassType) {
                if (instPropRef.property.parentClass.name == "TsString" && instPropRef.property.name == "length")
                    return $"{this.main.expr(instPropRef.object_)}.length()";
                if (instPropRef.property.parentClass.name == "TsArray" && instPropRef.property.name == "length")
                    return $"{this.main.expr(instPropRef.object_)}.{(this.isArray(instPropRef.object_) ? "length" : "size()")}";
            }
            else if (expr is InstanceFieldReference instFieldRef && instFieldRef.object_.actualType is ClassType) {
                if (instFieldRef.field.parentInterface.name == "RegExpExecArray" && instFieldRef.field.name == "length")
                    return $"{this.main.expr(instFieldRef.object_)}.length";
                if (instFieldRef.field.parentInterface.name == "Map" && instFieldRef.field.name == "size")
                    return $"{this.main.expr(instFieldRef.object_)}.size()";
            }
            else if (expr is StaticMethodCallExpression statMethCallExpr && statMethCallExpr.method.parentInterface is Class class_)
                return this.convertMethod(class_, null, statMethCallExpr.method, statMethCallExpr.args, statMethCallExpr.actualType);
            return null;
        }
        
        public string stmt(Statement stmt)
        {
            return null;
        }
    }
}