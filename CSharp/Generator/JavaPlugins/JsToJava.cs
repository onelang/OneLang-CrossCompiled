using Generator;
using One.Ast;

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
        
        public string convertMethod(Class cls, Expression obj, Method method, Expression[] args)
        {
            var objR = obj == null ? null : this.main.expr(obj);
            var argsR = args.map(x => this.main.expr(x));
            // if (cls.name === "TsString") {
            //     if (method.name === "replace") {
            //         if (args[0] instanceof RegexLiteral) {
            //             this.main.imports.add("java.util.regex.Pattern");
            //             return `${objR}.replaceAll(${JSON.stringify((<RegexLiteral>args[0]).pattern)}, ${argsR[1]})`;
            //         }
            
            //         return `${argsR[0]}.replace(${objR}, ${argsR[1]})`;
            //     }
            // }
            
            return null;
        }
        
        public string expr(IExpression expr)
        {
            if (expr is InstanceMethodCallExpression instMethCallExpr && instMethCallExpr.object_.actualType is ClassType classType)
                return this.convertMethod(classType.decl, instMethCallExpr.object_, instMethCallExpr.method, instMethCallExpr.args);
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