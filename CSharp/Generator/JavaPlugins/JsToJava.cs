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
            if (cls.name == "TsArray") {
                if (method.name == "includes")
                    return $"{this.arrayStream(obj)}.anyMatch({argsR.get(0)}::equals)";
                else if (method.name == "set") {
                    if (this.isArray(obj))
                        return $"{objR}[{argsR.get(0)}] = {argsR.get(1)}";
                    else
                        return $"{objR}.set({argsR.get(0)}, {argsR.get(1)})";
                }
                else if (method.name == "get")
                    return this.isArray(obj) ? $"{objR}[{argsR.get(0)}]" : $"{objR}.get({argsR.get(0)})";
                else if (method.name == "join") {
                    this.main.imports.add("java.util.stream.Collectors");
                    return $"{this.arrayStream(obj)}.collect(Collectors.joining({argsR.get(0)}))";
                }
                else if (method.name == "map")
                    //if (returnType.repr() !== "C:TsArray<C:TsString>") debugger;
                    return $"{this.arrayStream(obj)}.map({argsR.get(0)}).{this.toArray(returnType)}";
                else if (method.name == "push")
                    return $"{objR}.add({argsR.get(0)})";
                else if (method.name == "pop")
                    return $"{objR}.remove({objR}.size() - 1)";
                else if (method.name == "filter")
                    return $"{this.arrayStream(obj)}.filter({argsR.get(0)}).{this.toArray(returnType)}";
                else if (method.name == "every") {
                    this.main.imports.add("OneStd.StdArrayHelper");
                    return $"StdArrayHelper.allMatch({objR}, {argsR.get(0)})";
                }
                else if (method.name == "some")
                    return $"{this.arrayStream(obj)}.anyMatch({argsR.get(0)})";
                else if (method.name == "concat") {
                    this.main.imports.add("java.util.stream.Stream");
                    return $"Stream.of({objR}, {argsR.get(0)}).flatMap(Stream::of).{this.toArray(obj.getType())}";
                }
                else if (method.name == "shift")
                    return $"{objR}.remove(0)";
                else if (method.name == "find")
                    return $"{this.arrayStream(obj)}.filter({argsR.get(0)}).findFirst().orElse(null)";
                else if (method.name == "sort") {
                    this.main.imports.add("java.util.Collections");
                    return $"Collections.sort({objR})";
                }
            }
            else if (cls.name == "TsString") {
                if (method.name == "replace") {
                    if (args.get(0) is RegexLiteral) {
                        this.main.imports.add("java.util.regex.Pattern");
                        return $"{objR}.replaceAll({JSON.stringify((((RegexLiteral)args.get(0))).pattern)}, {argsR.get(1)})";
                    }
                    
                    return $"{argsR.get(0)}.replace({objR}, {argsR.get(1)})";
                }
                else if (method.name == "charCodeAt")
                    return $"(int){objR}.charAt({argsR.get(0)})";
                else if (method.name == "includes")
                    return $"{objR}.contains({argsR.get(0)})";
                else if (method.name == "get")
                    return $"{objR}.substring({argsR.get(0)}, {argsR.get(0)} + 1)";
                else if (method.name == "substr")
                    return argsR.length() == 1 ? $"{objR}.substring({argsR.get(0)})" : $"{objR}.substring({argsR.get(0)}, {argsR.get(0)} + {argsR.get(1)})";
                else if (method.name == "substring")
                    return $"{objR}.substring({argsR.get(0)}, {argsR.get(1)})";
                
                if (method.name == "split" && args.get(0) is RegexLiteral) {
                    var pattern = (((RegexLiteral)args.get(0))).pattern;
                    return $"{objR}.split({JSON.stringify(pattern)}, -1)";
                }
            }
            else if (cls.name == "TsMap" || cls.name == "Map") {
                if (method.name == "set")
                    return $"{objR}.put({argsR.get(0)}, {argsR.get(1)})";
                else if (method.name == "get")
                    return $"{objR}.get({argsR.get(0)})";
                else if (method.name == "hasKey" || method.name == "has")
                    return $"{objR}.containsKey({argsR.get(0)})";
                else if (method.name == "delete")
                    return $"{objR}.remove({argsR.get(0)})";
                else if (method.name == "values")
                    return $"{objR}.values().{this.toArray(obj.getType(), 1)}";
            }
            else if (cls.name == "Object") {
                if (method.name == "keys")
                    return $"{argsR.get(0)}.keySet().toArray(String[]::new)";
                else if (method.name == "values")
                    return $"{argsR.get(0)}.values().{this.toArray(args.get(0).getType())}";
            }
            else if (cls.name == "Set") {
                if (method.name == "values")
                    return $"{objR}.{this.toArray(obj.getType())}";
                else if (method.name == "has")
                    return $"{objR}.contains({argsR.get(0)})";
                else if (method.name == "add")
                    return $"{objR}.add({argsR.get(0)})";
            }
            else if (cls.name == "ArrayHelper") { }
            else if (cls.name == "Array") {
                if (method.name == "from")
                    return $"{argsR.get(0)}";
            }
            else if (cls.name == "Promise") {
                if (method.name == "resolve")
                    return $"{argsR.get(0)}";
            }
            else if (cls.name == "RegExpExecArray") {
                if (method.name == "get")
                    return $"{objR}[{argsR.get(0)}]";
            }
            else if (new List<string> { "JSON", "console", "RegExp" }.includes(cls.name)) {
                this.main.imports.add($"OneStd.{cls.name}");
                return null;
            }
            else
                return null;
            
            var methodName = $"{cls.name}.{method.name}";
            if (!this.unhandledMethods.has(methodName)) {
                console.error($"[JsToJava] Method was not handled: {cls.name}.{method.name}");
                this.unhandledMethods.add(methodName);
            }
            //debugger;
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