using One.Ast;
using System.Collections.Generic;

namespace Utils
{
    public class TSOverviewGenerator {
        public static TSOverviewGenerator preview;
        public bool previewOnly;
        public bool showTypes;
        
        static TSOverviewGenerator()
        {
            TSOverviewGenerator.preview = new TSOverviewGenerator(true);
        }
        
        public TSOverviewGenerator(bool previewOnly = false, bool showTypes = false)
        {
            this.previewOnly = previewOnly;
            this.showTypes = showTypes;
        }
        
        public string leading(IHasAttributesAndTrivia item)
        {
            var result = "";
            if (item.leadingTrivia != null && item.leadingTrivia.length() > 0)
                result += item.leadingTrivia;
            if (item.attributes != null)
                result += Object.keys(item.attributes).map(x => $"/// {{ATTR}} name=\"{x}\", value={JSON.stringify(item.attributes.get(x))}\n").join("");
            return result;
        }
        
        public string preArr(string prefix, string[] value)
        {
            return value.length() > 0 ? $"{prefix}{value.join(", ")}" : "";
        }
        
        public string preIf(string prefix, bool condition)
        {
            return condition ? prefix : "";
        }
        
        public string pre(string prefix, string value)
        {
            return value != null ? $"{prefix}{value}" : "";
        }
        
        public string typeArgs(string[] args)
        {
            return args != null && args.length() > 0 ? $"<{args.join(", ")}>" : "";
        }
        
        public string type(IType t, bool raw = false)
        {
            var repr = t == null ? "???" : t.repr();
            if (repr == "U:UNKNOWN") { }
            return (raw ? "" : "{T}") + repr;
        }
        
        public string var(IVariable v)
        {
            var result = "";
            var isProp = v is Property;
            if (v is Field field || v is Property) {
                var m = ((IClassMember)v);
                result += this.preIf("", m.isStatic);
                result += m.visibility == Visibility.Private ? "private " : m.visibility == Visibility.Protected ? "protected " : m.visibility == Visibility.Public ? "public " : "VISIBILITY-NOT-SET";
            }
            result += $"{(isProp ? "@prop " : "")}";
            if (v.mutability != null) {
                result += $"{(v.mutability.unused ? "@unused " : "")}";
                result += $"{(v.mutability.mutated ? "@mutated " : "")}";
                result += $"{(v.mutability.reassigned ? "@reass " : "")}";
            }
            result += $"{v.name}{(isProp ? "()" : "")}: {this.type(v.type)}";
            if (v is VariableDeclaration varDecl || v is ForVariable || v is Field || v is MethodParameter) {
                var init = (((IVariableWithInitializer)v)).initializer;
                if (init != null)
                    result += this.pre(" = ", this.expr(init));
            }
            return result;
        }
        
        public string expr(IExpression expr)
        {
            var res = "UNKNOWN-EXPR";
            if (expr is NewExpression newExpr)
                res = $"new {this.type(newExpr.cls)}({(this.previewOnly ? "..." : newExpr.args.map(x => this.expr(x)).join(", "))})";
            else if (expr is UnresolvedNewExpression unrNewExpr)
                res = $"new {this.type(unrNewExpr.cls)}({(this.previewOnly ? "..." : unrNewExpr.args.map(x => this.expr(x)).join(", "))})";
            else if (expr is Identifier ident)
                res = $"{{ID}}{ident.text}";
            else if (expr is PropertyAccessExpression propAccExpr)
                res = $"{this.expr(propAccExpr.object_)}.{{PA}}{propAccExpr.propertyName}";
            else if (expr is UnresolvedCallExpression unrCallExpr) {
                var typeArgs = unrCallExpr.typeArgs.length() > 0 ? $"<{unrCallExpr.typeArgs.map(x => this.type(x)).join(", ")}>" : "";
                res = $"{this.expr(unrCallExpr.func)}{typeArgs}({(this.previewOnly ? "..." : unrCallExpr.args.map(x => this.expr(x)).join(", "))})";
            }
            else if (expr is UnresolvedMethodCallExpression unrMethCallExpr) {
                var typeArgs = unrMethCallExpr.typeArgs.length() > 0 ? $"<{unrMethCallExpr.typeArgs.map(x => this.type(x)).join(", ")}>" : "";
                res = $"{this.expr(unrMethCallExpr.object_)}.{{UM}}{unrMethCallExpr.methodName}{typeArgs}({(this.previewOnly ? "..." : unrMethCallExpr.args.map(x => this.expr(x)).join(", "))})";
            }
            else if (expr is InstanceMethodCallExpression instMethCallExpr) {
                var typeArgs = instMethCallExpr.typeArgs.length() > 0 ? $"<{instMethCallExpr.typeArgs.map(x => this.type(x)).join(", ")}>" : "";
                res = $"{this.expr(instMethCallExpr.object_)}.{{M}}{instMethCallExpr.method.name}{typeArgs}({(this.previewOnly ? "..." : instMethCallExpr.args.map(x => this.expr(x)).join(", "))})";
            }
            else if (expr is StaticMethodCallExpression statMethCallExpr) {
                var typeArgs = statMethCallExpr.typeArgs.length() > 0 ? $"<{statMethCallExpr.typeArgs.map(x => this.type(x)).join(", ")}>" : "";
                res = $"{statMethCallExpr.method.parentInterface.name}.{{M}}{statMethCallExpr.method.name}{typeArgs}({(this.previewOnly ? "..." : statMethCallExpr.args.map(x => this.expr(x)).join(", "))})";
            }
            else if (expr is GlobalFunctionCallExpression globFunctCallExpr)
                res = $"{globFunctCallExpr.func.name}({(this.previewOnly ? "..." : globFunctCallExpr.args.map(x => this.expr(x)).join(", "))})";
            else if (expr is LambdaCallExpression lambdCallExpr)
                res = $"{this.expr(lambdCallExpr.method)}({(this.previewOnly ? "..." : lambdCallExpr.args.map(x => this.expr(x)).join(", "))})";
            else if (expr is BooleanLiteral boolLit)
                res = $"{(boolLit.boolValue ? "true" : "false")}";
            else if (expr is StringLiteral strLit)
                res = $"{JSON.stringify(strLit.stringValue)}";
            else if (expr is NumericLiteral numLit)
                res = $"{numLit.valueAsText}";
            else if (expr is CharacterLiteral charLit)
                res = $"'{charLit.charValue}'";
            else if (expr is ElementAccessExpression elemAccExpr)
                res = $"({this.expr(elemAccExpr.object_)})[{this.expr(elemAccExpr.elementExpr)}]";
            else if (expr is TemplateString templStr)
                res = "`" + templStr.parts.map(x => x.isLiteral ? x.literalText : "${" + this.expr(x.expression) + "}").join("") + "`";
            else if (expr is BinaryExpression binExpr)
                res = $"{this.expr(binExpr.left)} {binExpr.operator_} {this.expr(binExpr.right)}";
            else if (expr is ArrayLiteral arrayLit)
                res = $"[{arrayLit.items.map(x => this.expr(x)).join(", ")}]";
            else if (expr is CastExpression castExpr)
                res = $"<{this.type(castExpr.newType)}>({this.expr(castExpr.expression)})";
            else if (expr is ConditionalExpression condExpr)
                res = $"{this.expr(condExpr.condition)} ? {this.expr(condExpr.whenTrue)} : {this.expr(condExpr.whenFalse)}";
            else if (expr is InstanceOfExpression instOfExpr)
                res = $"{this.expr(instOfExpr.expr)} instanceof {this.type(instOfExpr.checkType)}";
            else if (expr is ParenthesizedExpression parExpr)
                res = $"({this.expr(parExpr.expression)})";
            else if (expr is RegexLiteral regexLit)
                res = $"/{regexLit.pattern}/{(regexLit.global ? "g" : "")}{(regexLit.caseInsensitive ? "g" : "")}";
            else if (expr is Lambda lambd)
                res = $"({lambd.parameters.map(x => x.name + (x.type != null ? ": " + this.type(x.type) : "")).join(", ")})" + (lambd.captures != null && lambd.captures.length() > 0 ? $" @captures({lambd.captures.map(x => x.name).join(", ")})" : "") + $" => {{ {this.rawBlock(lambd.body)} }}";
            else if (expr is UnaryExpression unaryExpr && unaryExpr.unaryType == UnaryType.Prefix)
                res = $"{unaryExpr.operator_}{this.expr(unaryExpr.operand)}";
            else if (expr is UnaryExpression unaryExpr2 && unaryExpr2.unaryType == UnaryType.Postfix)
                res = $"{this.expr(unaryExpr2.operand)}{unaryExpr2.operator_}";
            else if (expr is MapLiteral mapLit) {
                var repr = mapLit.items.map(item => $"{item.key}: {this.expr(item.value)}").join(",\n");
                res = "{L:M}" + (repr == "" ? "{}" : repr.includes("\n") ? $"{{\n{this.pad(repr)}\n}}" : $"{{ {repr} }}");
            }
            else if (expr is NullLiteral)
                res = $"null";
            else if (expr is AwaitExpression awaitExpr)
                res = $"await {this.expr(awaitExpr.expr)}";
            else if (expr is ThisReference)
                res = $"{{R}}this";
            else if (expr is StaticThisReference)
                res = $"{{R:Static}}this";
            else if (expr is EnumReference enumRef)
                res = $"{{R:Enum}}{enumRef.decl.name}";
            else if (expr is ClassReference classRef)
                res = $"{{R:Cls}}{classRef.decl.name}";
            else if (expr is MethodParameterReference methParRef)
                res = $"{{R:MetP}}{methParRef.decl.name}";
            else if (expr is VariableDeclarationReference varDeclRef)
                res = $"{{V}}{varDeclRef.decl.name}";
            else if (expr is ForVariableReference forVarRef)
                res = $"{{R:ForV}}{forVarRef.decl.name}";
            else if (expr is ForeachVariableReference forVarRef2)
                res = $"{{R:ForEV}}{forVarRef2.decl.name}";
            else if (expr is CatchVariableReference catchVarRef)
                res = $"{{R:CatchV}}{catchVarRef.decl.name}";
            else if (expr is GlobalFunctionReference globFunctRef)
                res = $"{{R:GFunc}}{globFunctRef.decl.name}";
            else if (expr is SuperReference)
                res = $"{{R}}super";
            else if (expr is StaticFieldReference statFieldRef)
                res = $"{{R:StFi}}{statFieldRef.decl.parentInterface.name}::{statFieldRef.decl.name}";
            else if (expr is StaticPropertyReference statPropRef)
                res = $"{{R:StPr}}{statPropRef.decl.parentClass.name}::{statPropRef.decl.name}";
            else if (expr is InstanceFieldReference instFieldRef)
                res = $"{this.expr(instFieldRef.object_)}.{{F}}{instFieldRef.field.name}";
            else if (expr is InstancePropertyReference instPropRef)
                res = $"{this.expr(instPropRef.object_)}.{{P}}{instPropRef.property.name}";
            else if (expr is EnumMemberReference enumMembRef)
                res = $"{{E}}{enumMembRef.decl.parentEnum.name}::{enumMembRef.decl.name}";
            else if (expr is NullCoalesceExpression nullCoalExpr)
                res = $"{this.expr(nullCoalExpr.defaultExpr)} ?? {this.expr(nullCoalExpr.exprIfNull)}";
            else { }
            
            if (this.showTypes)
                res = $"<{this.type(expr.getType(), true)}>({res})";
            
            return res;
        }
        
        public string block(Block block, bool allowOneLiner = true)
        {
            if (this.previewOnly)
                return " { ... }";
            var stmtLen = block.statements.length();
            return stmtLen == 0 ? " { }" : allowOneLiner && stmtLen == 1 ? $"\n{this.pad(this.rawBlock(block))}" : $" {{\n{this.pad(this.rawBlock(block))}\n}}";
        }
        
        public string stmt(Statement stmt)
        {
            var res = "UNKNOWN-STATEMENT";
            if (stmt is BreakStatement)
                res = "break;";
            else if (stmt is ReturnStatement retStat)
                res = retStat.expression == null ? "return;" : $"return {this.expr(retStat.expression)};";
            else if (stmt is UnsetStatement unsetStat)
                res = $"unset {this.expr(unsetStat.expression)};";
            else if (stmt is ThrowStatement throwStat)
                res = $"throw {this.expr(throwStat.expression)};";
            else if (stmt is ExpressionStatement exprStat)
                res = $"{this.expr(exprStat.expression)};";
            else if (stmt is VariableDeclaration varDecl2)
                res = $"var {this.var(varDecl2)};";
            else if (stmt is ForeachStatement forStat)
                res = $"for (const {forStat.itemVar.name} of {this.expr(forStat.items)})" + this.block(forStat.body);
            else if (stmt is IfStatement ifStat) {
                var elseIf = ifStat.else_ != null && ifStat.else_.statements.length() == 1 && ifStat.else_.statements.get(0) is IfStatement;
                res = $"if ({this.expr(ifStat.condition)}){this.block(ifStat.then)}";
                if (!this.previewOnly)
                    res += (elseIf ? $"\nelse {this.stmt(ifStat.else_.statements.get(0))}" : "") + (!elseIf && ifStat.else_ != null ? $"\nelse" + this.block(ifStat.else_) : "");
            }
            else if (stmt is WhileStatement whileStat)
                res = $"while ({this.expr(whileStat.condition)})" + this.block(whileStat.body);
            else if (stmt is ForStatement forStat2)
                res = $"for ({(forStat2.itemVar != null ? this.var(forStat2.itemVar) : "")}; {this.expr(forStat2.condition)}; {this.expr(forStat2.incrementor)})" + this.block(forStat2.body);
            else if (stmt is DoStatement doStat)
                res = $"do{this.block(doStat.body)} while ({this.expr(doStat.condition)})";
            else if (stmt is TryStatement tryStat)
                res = "try" + this.block(tryStat.tryBody, false) + (tryStat.catchBody != null ? $" catch ({tryStat.catchVar.name}){this.block(tryStat.catchBody)}" : "") + (tryStat.finallyBody != null ? "finally" + this.block(tryStat.finallyBody) : "");
            else if (stmt is ContinueStatement)
                res = $"continue;";
            else { }
            return this.previewOnly ? res : this.leading(stmt) + res;
        }
        
        public string rawBlock(Block block)
        {
            return block.statements.map(stmt => this.stmt(stmt)).join("\n");
        }
        
        public string methodBase(IMethodBase method, IType returns)
        {
            if (method == null)
                return "";
            var name = method is Method meth ? meth.name : method is Constructor ? "constructor" : method is GlobalFunction globFunct ? globFunct.name : "???";
            var typeArgs = method is Method meth2 ? meth2.typeArguments : null;
            return this.preIf("/* throws */ ", method.throws) + $"{name}{this.typeArgs(typeArgs)}({method.parameters.map(p => this.leading(p) + this.var(p)).join(", ")})" + (returns is VoidType ? "" : $": {this.type(returns)}") + (method.body != null ? $" {{\n{this.pad(this.rawBlock(method.body))}\n}}" : ";");
        }
        
        public string method(Method method)
        {
            return method == null ? "" : (method.isStatic ? "static " : "") + (method.attributes != null && method.attributes.hasKey("mutates") ? "@mutates " : "") + this.methodBase(method, method.returns);
        }
        
        public string classLike(IInterface cls)
        {
            var resList = new List<string>();
            resList.push(cls.fields.map(field => this.var(field) + ";").join("\n"));
            if (cls is Class class_) {
                resList.push(class_.properties.map(prop => this.var(prop) + ";").join("\n"));
                resList.push(this.methodBase(class_.constructor_, VoidType.instance));
            }
            resList.push(cls.methods.map(method => this.method(method)).join("\n\n"));
            return this.pad(resList.filter(x => x != "").join("\n\n"));
        }
        
        public string pad(string str)
        {
            return str.split(new RegExp("\\n")).map(x => $"    {x}").join("\n");
        }
        
        public string imp(IImportable imp)
        {
            return "" + (imp is UnresolvedImport ? "X" : imp is Class ? "C" : imp is Interface ? "I" : imp is Enum_ ? "E" : "???") + $":{imp.name}";
        }
        
        public string nodeRepr(IAstNode node)
        {
            if (node is Statement stat)
                return this.stmt(stat);
            else if (node is Expression expr)
                return this.expr(expr);
            else
                return "/* TODO: missing */";
        }
        
        public string generate(SourceFile sourceFile)
        {
            var imps = sourceFile.imports.map(imp => (imp.importAll ? $"import * as {imp.importAs}" : $"import {{ {imp.imports.map(x => this.imp(x)).join(", ")} }}") + $" from \"{imp.exportScope.packageName}{this.pre("/", imp.exportScope.scopeName)}\";");
            var enums = sourceFile.enums.map(enum_ => $"{this.leading(enum_)}enum {enum_.name} {{ {enum_.values.map(x => x.name).join(", ")} }}");
            var intfs = sourceFile.interfaces.map(intf => $"{this.leading(intf)}interface {intf.name}{this.typeArgs(intf.typeArguments)}" + $"{this.preArr(" extends ", intf.baseInterfaces.map(x => this.type(x)))} {{\n{this.classLike(intf)}\n}}");
            var classes = sourceFile.classes.map(cls => $"{this.leading(cls)}class {cls.name}{this.typeArgs(cls.typeArguments)}" + this.pre(" extends ", cls.baseClass != null ? this.type(cls.baseClass) : null) + this.preArr(" implements ", cls.baseInterfaces.map(x => this.type(x))) + $" {{\n{this.classLike(cls)}\n}}");
            var funcs = sourceFile.funcs.map(func => $"{this.leading(func)}function {func.name}{this.methodBase(func, func.returns)}");
            var main = this.rawBlock(sourceFile.mainBlock);
            var result = $"// export scope: {sourceFile.exportScope.packageName}/{sourceFile.exportScope.scopeName}\n" + new List<string> { imps.join("\n"), enums.join("\n"), intfs.join("\n\n"), classes.join("\n\n"), funcs.join("\n\n"), main }.filter(x => x != "").join("\n\n");
            return result;
        }
    }
}