using Generator;
using One.Ast;

namespace Generator.PhpPlugins
{
    public class JsToPhp : IGeneratorPlugin {
        public Set<string> unhandledMethods;
        public PhpGenerator main;
        
        public JsToPhp(PhpGenerator main)
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
                    return $"in_array({argsR.get(0)}, {objR})";
                else if (method.name == "set")
                    return $"{objR}[{argsR.get(0)}] = {argsR.get(1)}";
                else if (method.name == "get")
                    return $"{objR}[{argsR.get(0)}]";
                else if (method.name == "join")
                    return $"implode({argsR.get(0)}, {objR})";
                else if (method.name == "map")
                    return $"array_map({argsR.get(0)}, {objR})";
                else if (method.name == "push")
                    return $"{objR}[] = {argsR.get(0)}";
                else if (method.name == "pop")
                    return $"array_pop({objR})";
                else if (method.name == "filter")
                    return $"array_values(array_filter({objR}, {argsR.get(0)}))";
                else if (method.name == "every")
                    return $"\\OneLang\\ArrayHelper::every({objR}, {argsR.get(0)})";
                else if (method.name == "some")
                    return $"\\OneLang\\ArrayHelper::some({objR}, {argsR.get(0)})";
                else if (method.name == "concat")
                    return $"array_merge({objR}, {argsR.get(0)})";
                else if (method.name == "shift")
                    return $"array_shift({objR})";
                else if (method.name == "find")
                    return $"\\OneLang\\ArrayHelper::find({objR}, {argsR.get(0)})";
                else if (method.name == "sort")
                    return $"sort({objR})";
            }
            else if (cls.name == "TsString") {
                var objR = this.main.expr(obj);
                var argsR = args.map(x => this.main.expr(x));
                if (method.name == "split") {
                    if (args.get(0) is RegexLiteral) {
                        var pattern = (((RegexLiteral)args.get(0))).pattern;
                        var modPattern = "/" + pattern.replace(new RegExp("/"), "\\/") + "/";
                        return $"preg_split({JSON.stringify(modPattern)}, {objR})";
                    }
                    
                    return $"explode({argsR.get(0)}, {objR})";
                }
                else if (method.name == "replace") {
                    if (args.get(0) is RegexLiteral)
                        return $"preg_replace({JSON.stringify($"/{(((RegexLiteral)args.get(0))).pattern}/")}, {argsR.get(1)}, {objR})";
                    
                    return $"{argsR.get(0)}.replace({objR}, {argsR.get(1)})";
                }
                else if (method.name == "includes")
                    return $"strpos({objR}, {argsR.get(0)}) !== false";
                else if (method.name == "startsWith") {
                    if (argsR.length() > 1)
                        return $"substr_compare({objR}, {argsR.get(0)}, {argsR.get(1)}, strlen({argsR.get(0)})) === 0";
                    else
                        return $"substr_compare({objR}, {argsR.get(0)}, 0, strlen({argsR.get(0)})) === 0";
                }
                else if (method.name == "endsWith") {
                    if (argsR.length() > 1)
                        return $"substr_compare({objR}, {argsR.get(0)}, {argsR.get(1)} - strlen({argsR.get(0)}), strlen({argsR.get(0)})) === 0";
                    else
                        return $"substr_compare({objR}, {argsR.get(0)}, strlen({objR}) - strlen({argsR.get(0)}), strlen({argsR.get(0)})) === 0";
                }
                else if (method.name == "indexOf")
                    return $"strpos({objR}, {argsR.get(0)}, {argsR.get(1)})";
                else if (method.name == "lastIndexOf")
                    return $"strrpos({objR}, {argsR.get(0)}, {argsR.get(1)} - strlen({objR}))";
                else if (method.name == "substr") {
                    if (argsR.length() > 1)
                        return $"substr({objR}, {argsR.get(0)}, {argsR.get(1)})";
                    else
                        return $"substr({objR}, {argsR.get(0)})";
                }
                else if (method.name == "substring")
                    return $"substr({objR}, {argsR.get(0)}, {argsR.get(1)} - ({argsR.get(0)}))";
                else if (method.name == "repeat")
                    return $"str_repeat({objR}, {argsR.get(0)})";
                else if (method.name == "toUpperCase")
                    return $"strtoupper({objR})";
                else if (method.name == "toLowerCase")
                    return $"strtolower({objR})";
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
                    return $"@{objR}[{argsR.get(0)}] ?? null";
                else if (method.name == "hasKey")
                    return $"array_key_exists({argsR.get(0)}, {objR})";
            }
            else if (cls.name == "Object") {
                var argsR = args.map(x => this.main.expr(x));
                if (method.name == "keys")
                    return $"array_keys({argsR.get(0)})";
                else if (method.name == "values")
                    return $"array_values({argsR.get(0)})";
            }
            else if (cls.name == "ArrayHelper") {
                var argsR = args.map(x => this.main.expr(x));
                if (method.name == "sortBy")
                    return $"\\OneLang\\ArrayHelper::sortBy({argsR.get(0)}, {argsR.get(1)})";
                else if (method.name == "removeLastN")
                    return $"array_splice({argsR.get(0)}, -{argsR.get(1)})";
            }
            else if (cls.name == "Math") {
                var argsR = args.map(x => this.main.expr(x));
                if (method.name == "floor")
                    return $"floor({argsR.get(0)})";
            }
            else if (cls.name == "JSON") {
                var argsR = args.map(x => this.main.expr(x));
                if (method.name == "stringify")
                    return $"json_encode({argsR.get(0)}, JSON_UNESCAPED_SLASHES)";
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
                    return $"strlen({this.main.expr(instPropRef.object_)})";
                if (instPropRef.property.parentClass.name == "TsArray" && instPropRef.property.name == "length")
                    return $"count({this.main.expr(instPropRef.object_)})";
            }
            else if (expr is InstanceFieldReference instFieldRef && instFieldRef.object_.actualType is ClassType) {
                if (instFieldRef.field.parentInterface.name == "RegExpExecArray" && instFieldRef.field.name == "length")
                    return $"count({this.main.expr(instFieldRef.object_)})";
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