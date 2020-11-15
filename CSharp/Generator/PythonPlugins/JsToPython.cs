using Generator;
using One.Ast;

namespace Generator.PythonPlugins
{
    public class JsToPython : IGeneratorPlugin {
        public Set<string> unhandledMethods;
        public PythonGenerator main;
        
        public JsToPython(PythonGenerator main)
        {
            this.main = main;
            this.unhandledMethods = new Set<string>();
        }
        
        public string convertMethod(Class cls, Expression obj, Method method, Expression[] args)
        {
            if (cls.name == "TsArray") {
                var objR = this.main.expr(obj);
                var argsR = args.map(x => this.main.expr(x));
                if (method.name == "includes")
                    return $"{argsR.get(0)} in {objR}";
                else if (method.name == "set")
                    return $"{objR}[{argsR.get(0)}] = {argsR.get(1)}";
                else if (method.name == "get")
                    return $"{objR}[{argsR.get(0)}]";
                else if (method.name == "join")
                    return $"{argsR.get(0)}.join({objR})";
                else if (method.name == "map")
                    return $"list(map({argsR.get(0)}, {objR}))";
                else if (method.name == "push")
                    return $"{objR}.append({argsR.get(0)})";
                else if (method.name == "pop")
                    return $"{objR}.pop()";
                else if (method.name == "filter")
                    return $"list(filter({argsR.get(0)}, {objR}))";
                else if (method.name == "every")
                    return $"ArrayHelper.every({argsR.get(0)}, {objR})";
                else if (method.name == "some")
                    return $"ArrayHelper.some({argsR.get(0)}, {objR})";
                else if (method.name == "concat")
                    return $"{objR} + {argsR.get(0)}";
                else if (method.name == "shift")
                    return $"{objR}.pop(0)";
                else if (method.name == "find")
                    return $"next(filter({argsR.get(0)}, {objR}), None)";
            }
            else if (cls.name == "TsString") {
                var objR = this.main.expr(obj);
                var argsR = args.map(x => this.main.expr(x));
                if (method.name == "split") {
                    if (args.get(0) is RegexLiteral) {
                        var pattern = (((RegexLiteral)args.get(0))).pattern;
                        if (!pattern.startsWith("^")) {
                            //return `${objR}.split(${JSON.stringify(pattern)})`;
                            this.main.imports.add("import re");
                            return $"re.split({JSON.stringify(pattern)}, {objR})";
                        }
                    }
                    
                    return $"{argsR.get(0)}.split({objR})";
                }
                else if (method.name == "replace") {
                    if (args.get(0) is RegexLiteral) {
                        this.main.imports.add("import re");
                        return $"re.sub({JSON.stringify((((RegexLiteral)args.get(0))).pattern)}, {argsR.get(1)}, {objR})";
                    }
                    
                    return $"{argsR.get(0)}.replace({objR}, {argsR.get(1)})";
                }
                else if (method.name == "includes")
                    return $"{argsR.get(0)} in {objR}";
                else if (method.name == "startsWith")
                    return $"{objR}.startswith({argsR.join(", ")})";
                else if (method.name == "indexOf")
                    return $"{objR}.find({argsR.get(0)}, {argsR.get(1)})";
                else if (method.name == "lastIndexOf")
                    return $"{objR}.rfind({argsR.get(0)}, 0, {argsR.get(1)})";
                else if (method.name == "substr")
                    return argsR.length() == 1 ? $"{objR}[{argsR.get(0)}:]" : $"{objR}[{argsR.get(0)}:{argsR.get(0)} + {argsR.get(1)}]";
                else if (method.name == "substring")
                    return $"{objR}[{argsR.get(0)}:{argsR.get(1)}]";
                else if (method.name == "repeat")
                    return $"{objR} * ({argsR.get(0)})";
                else if (method.name == "toUpperCase")
                    return $"{objR}.upper()";
                else if (method.name == "toLowerCase")
                    return $"{objR}.lower()";
                else if (method.name == "endsWith")
                    return $"{objR}.endswith({argsR.get(0)})";
                else if (method.name == "get")
                    return $"{objR}[{argsR.get(0)}]";
                else if (method.name == "charCodeAt")
                    return $"ord({objR}[{argsR.get(0)}])";
            }
            else if (cls.name == "TsMap") {
                var objR = this.main.expr(obj);
                var argsR = args.map(x => this.main.expr(x));
                if (method.name == "set")
                    return $"{objR}[{argsR.get(0)}] = {argsR.get(1)}";
                else if (method.name == "get")
                    return $"{objR}.get({argsR.get(0)})";
                else if (method.name == "hasKey")
                    return $"{argsR.get(0)} in {objR}";
            }
            else if (cls.name == "Object") {
                var argsR = args.map(x => this.main.expr(x));
                if (method.name == "keys")
                    return $"{argsR.get(0)}.keys()";
                else if (method.name == "values")
                    return $"{argsR.get(0)}.values()";
            }
            else if (cls.name == "Set") {
                var objR = this.main.expr(obj);
                var argsR = args.map(x => this.main.expr(x));
                if (method.name == "values")
                    return $"{objR}.keys()";
                else if (method.name == "has")
                    return $"{argsR.get(0)} in {objR}";
                else if (method.name == "add")
                    return $"{objR}[{argsR.get(0)}] = None";
            }
            else if (cls.name == "ArrayHelper") {
                var argsR = args.map(x => this.main.expr(x));
                if (method.name == "sortBy")
                    return $"sorted({argsR.get(0)}, key={argsR.get(1)})";
                else if (method.name == "removeLastN")
                    return $"del {argsR.get(0)}[-{argsR.get(1)}:]";
            }
            else if (cls.name == "RegExpExecArray") {
                var objR = this.main.expr(obj);
                var argsR = args.map(x => this.main.expr(x));
                return $"{objR}[{argsR.get(0)}]";
            }
            else
                return null;
            
            var methodName = $"{cls.name}.{method.name}";
            if (!this.unhandledMethods.has(methodName)) {
                console.error($"[JsToPython] Method was not handled: {cls.name}.{method.name}");
                this.unhandledMethods.add(methodName);
            }
            //debugger;
            return null;
        }
        
        public string expr(IExpression expr)
        {
            if (expr is InstanceMethodCallExpression instMethCallExpr && instMethCallExpr.object_.actualType is ClassType classType)
                return this.convertMethod(classType.decl, instMethCallExpr.object_, instMethCallExpr.method, instMethCallExpr.args);
            else if (expr is InstancePropertyReference instPropRef && instPropRef.object_.actualType is ClassType) {
                if (instPropRef.property.parentClass.name == "TsString" && instPropRef.property.name == "length")
                    return $"len({this.main.expr(instPropRef.object_)})";
                if (instPropRef.property.parentClass.name == "TsArray" && instPropRef.property.name == "length")
                    return $"len({this.main.expr(instPropRef.object_)})";
            }
            else if (expr is InstanceFieldReference instFieldRef && instFieldRef.object_.actualType is ClassType) {
                if (instFieldRef.field.parentInterface.name == "RegExpExecArray" && instFieldRef.field.name == "length")
                    return $"len({this.main.expr(instFieldRef.object_)})";
            }
            else if (expr is StaticMethodCallExpression statMethCallExpr && statMethCallExpr.method.parentInterface is Class class_)
                return this.convertMethod(class_, null, statMethCallExpr.method, statMethCallExpr.args);
            return null;
        }
        
        public string stmt(Statement stmt)
        {
            return null;
        }
    }
}