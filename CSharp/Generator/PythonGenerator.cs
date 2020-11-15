using Generator.PythonPlugins;
using Generator;
using One.Ast;
using One;
using System.Collections.Generic;
using Utils;

namespace Generator
{
    public class PythonGenerator : IGenerator {
        public int tmplStrLevel = 0;
        public Package package;
        public SourceFile currentFile;
        public Set<string> imports;
        public Set<string> importAllScopes;
        public IInterface currentClass;
        public string[] reservedWords;
        public string[] fieldToMethodHack;
        public List<IGeneratorPlugin> plugins;
        
        public PythonGenerator()
        {
            this.reservedWords = new string[] { "from", "async", "global", "lambda", "cls", "import", "pass" };
            this.fieldToMethodHack = new string[0];
            this.plugins = new List<IGeneratorPlugin>();
            this.plugins.push(new JsToPython(this));
        }
        
        public string getLangName()
        {
            return "Python";
        }
        
        public string getExtension()
        {
            return "py";
        }
        
        public ITransformer[] getTransforms()
        {
            return new ITransformer[0];
        }
        
        public string type(IType type)
        {
            if (type is ClassType classType) {
                if (classType.decl.name == "TsString")
                    return "str";
                else if (classType.decl.name == "TsBoolean")
                    return "bool";
                else if (classType.decl.name == "TsNumber")
                    return "int";
                else
                    return this.clsName(classType.decl);
            }
            else
                return "NOT-HANDLED-TYPE";
        }
        
        public string[] splitName(string name)
        {
            var nameParts = new List<string>();
            var partStartIdx = 0;
            for (int i = 1; i < name.length(); i++) {
                var prevChrCode = name.charCodeAt(i - 1);
                var chrCode = name.charCodeAt(i);
                if (65 <= chrCode && chrCode <= 90 && !(65 <= prevChrCode && prevChrCode <= 90)) {
                    // 'A' .. 'Z'
                    nameParts.push(name.substring(partStartIdx, i).toLowerCase());
                    partStartIdx = i;
                }
                else if (chrCode == 95) {
                    // '-'
                    nameParts.push(name.substring(partStartIdx, i));
                    partStartIdx = i + 1;
                }
            }
            nameParts.push(name.substr(partStartIdx).toLowerCase());
            return nameParts.ToArray();
        }
        
        public string name_(string name)
        {
            if (this.reservedWords.includes(name))
                name += "_";
            if (this.fieldToMethodHack.includes(name))
                name += "()";
            return this.splitName(name).join("_");
        }
        
        public string calcImportedName(ExportScopeRef exportScope, string name)
        {
            if (this.importAllScopes.has(exportScope.getId()))
                return name;
            else
                return this.calcImportAlias(exportScope) + "." + name;
        }
        
        public string enumName(Enum_ enum_, bool isDecl = false)
        {
            var name = this.name_(enum_.name).toUpperCase();
            if (isDecl || enum_.parentFile.exportScope == null || enum_.parentFile == this.currentFile)
                return name;
            return this.calcImportedName(enum_.parentFile.exportScope, name);
        }
        
        public string enumMemberName(string name)
        {
            return this.name_(name).toUpperCase();
        }
        
        public string clsName(IInterface cls, bool isDecl = false)
        {
            // TODO: hack
            if (cls.name == "Set")
                return "dict";
            if (isDecl || cls.parentFile.exportScope == null || cls.parentFile == this.currentFile)
                return cls.name;
            return this.calcImportedName(cls.parentFile.exportScope, cls.name);
        }
        
        public string leading(Statement item)
        {
            var result = "";
            if (item.leadingTrivia != null && item.leadingTrivia.length() > 0)
                result += item.leadingTrivia.replace(new RegExp("//"), "#");
            //if (item.attributes !== null)
            //    result += Object.keys(item.attributes).map(x => `// @${x} ${item.attributes[x]}\n`).join("");
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
        
        public bool isTsArray(IType type)
        {
            return type is ClassType classType2 && classType2.decl.name == "TsArray";
        }
        
        public string vis(Visibility v)
        {
            return v == Visibility.Private ? "__" : v == Visibility.Protected ? "_" : v == Visibility.Public ? "" : "/* TODO: not set */public";
        }
        
        public string varWoInit(IVariable v, IHasAttributesAndTrivia attr)
        {
            return this.name_(v.name);
        }
        
        public string var(IVariableWithInitializer v, IHasAttributesAndTrivia attrs)
        {
            return $"{this.varWoInit(v, attrs)}{(v.initializer != null ? $" = {this.expr(v.initializer)}" : "")}";
        }
        
        public string exprCall(Expression[] args)
        {
            return $"({args.map(x => this.expr(x)).join(", ")})";
        }
        
        public string callParams(Expression[] args)
        {
            var argReprs = new List<string>();
            for (int i = 0; i < args.length(); i++)
                argReprs.push(this.expr(args.get(i)));
            return $"({argReprs.join(", ")})";
        }
        
        public string methodCall(IMethodCallExpression expr)
        {
            return this.name_(expr.method.name) + this.callParams(expr.args);
        }
        
        public string expr(IExpression expr)
        {
            foreach (var plugin in this.plugins) {
                var result = plugin.expr(expr);
                if (result != null)
                    return result;
            }
            
            var res = "UNKNOWN-EXPR";
            if (expr is NewExpression newExpr) {
                // TODO: hack
                if (newExpr.cls.decl.name == "Set")
                    res = newExpr.args.length() == 0 ? "dict()" : $"dict.fromkeys{this.callParams(newExpr.args)}";
                else
                    res = $"{this.clsName(newExpr.cls.decl)}{this.callParams(newExpr.args)}";
            }
            else if (expr is UnresolvedNewExpression unrNewExpr)
                res = $"/* TODO: UnresolvedNewExpression */ {unrNewExpr.cls.typeName}({unrNewExpr.args.map(x => this.expr(x)).join(", ")})";
            else if (expr is Identifier ident)
                res = $"/* TODO: Identifier */ {ident.text}";
            else if (expr is PropertyAccessExpression propAccExpr)
                res = $"/* TODO: PropertyAccessExpression */ {this.expr(propAccExpr.object_)}.{propAccExpr.propertyName}";
            else if (expr is UnresolvedCallExpression unrCallExpr)
                res = $"/* TODO: UnresolvedCallExpression */ {this.expr(unrCallExpr.func)}{this.exprCall(unrCallExpr.args)}";
            else if (expr is UnresolvedMethodCallExpression unrMethCallExpr)
                res = $"/* TODO: UnresolvedMethodCallExpression */ {this.expr(unrMethCallExpr.object_)}.{unrMethCallExpr.methodName}{this.exprCall(unrMethCallExpr.args)}";
            else if (expr is InstanceMethodCallExpression instMethCallExpr)
                res = $"{this.expr(instMethCallExpr.object_)}.{this.methodCall(instMethCallExpr)}";
            else if (expr is StaticMethodCallExpression statMethCallExpr) {
                //const parent = expr.method.parentInterface === this.currentClass ? "cls" : this.clsName(expr.method.parentInterface);
                var parent = this.clsName(statMethCallExpr.method.parentInterface);
                res = $"{parent}.{this.methodCall(statMethCallExpr)}";
            }
            else if (expr is GlobalFunctionCallExpression globFunctCallExpr) {
                this.imports.add("from OneLangStdLib import *");
                res = $"{this.name_(globFunctCallExpr.func.name)}{this.exprCall(globFunctCallExpr.args)}";
            }
            else if (expr is LambdaCallExpression lambdCallExpr)
                res = $"{this.expr(lambdCallExpr.method)}({lambdCallExpr.args.map(x => this.expr(x)).join(", ")})";
            else if (expr is BooleanLiteral boolLit)
                res = $"{(boolLit.boolValue ? "True" : "False")}";
            else if (expr is StringLiteral strLit)
                res = $"{JSON.stringify(strLit.stringValue)}";
            else if (expr is NumericLiteral numLit)
                res = $"{numLit.valueAsText}";
            else if (expr is CharacterLiteral charLit)
                res = $"'{charLit.charValue}'";
            else if (expr is ElementAccessExpression elemAccExpr)
                res = $"{this.expr(elemAccExpr.object_)}[{this.expr(elemAccExpr.elementExpr)}]";
            else if (expr is TemplateString templStr) {
                var parts = new List<string>();
                foreach (var part in templStr.parts) {
                    if (part.isLiteral) {
                        var lit = "";
                        for (int i = 0; i < part.literalText.length(); i++) {
                            var chr = part.literalText.get(i);
                            if (chr == "\n")
                                lit += "\\n";
                            else if (chr == "\r")
                                lit += "\\r";
                            else if (chr == "\t")
                                lit += "\\t";
                            else if (chr == "\\")
                                lit += "\\\\";
                            else if (chr == "'")
                                lit += "\\'";
                            else if (chr == "{")
                                lit += "{{";
                            else if (chr == "}")
                                lit += "}}";
                            else {
                                var chrCode = chr.charCodeAt(0);
                                if (32 <= chrCode && chrCode <= 126)
                                    lit += chr;
                                else
                                    throw new Error($"invalid char in template string (code={chrCode})");
                            }
                        }
                        parts.push(lit);
                    }
                    else {
                        this.tmplStrLevel++;
                        var repr = this.expr(part.expression);
                        this.tmplStrLevel--;
                        parts.push(part.expression is ConditionalExpression ? $"{{({repr})}}" : $"{{{repr}}}");
                    }
                }
                res = this.tmplStrLevel == 1 ? $"f'{parts.join("")}'" : $"f'''{parts.join("")}'''";
            }
            else if (expr is BinaryExpression binExpr) {
                var op = binExpr.operator_ == "&&" ? "and" : binExpr.operator_ == "||" ? "or" : binExpr.operator_;
                res = $"{this.expr(binExpr.left)} {op} {this.expr(binExpr.right)}";
            }
            else if (expr is ArrayLiteral arrayLit)
                res = $"[{arrayLit.items.map(x => this.expr(x)).join(", ")}]";
            else if (expr is CastExpression castExpr)
                res = $"{this.expr(castExpr.expression)}";
            else if (expr is ConditionalExpression condExpr)
                res = $"{this.expr(condExpr.whenTrue)} if {this.expr(condExpr.condition)} else {this.expr(condExpr.whenFalse)}";
            else if (expr is InstanceOfExpression instOfExpr)
                res = $"isinstance({this.expr(instOfExpr.expr)}, {this.type(instOfExpr.checkType)})";
            else if (expr is ParenthesizedExpression parExpr)
                res = $"({this.expr(parExpr.expression)})";
            else if (expr is RegexLiteral regexLit)
                res = $"RegExp({JSON.stringify(regexLit.pattern)})";
            else if (expr is Lambda lambd) {
                var body = "INVALID-BODY";
                if (lambd.body.statements.length() == 1 && lambd.body.statements.get(0) is ReturnStatement)
                    body = this.expr((((ReturnStatement)lambd.body.statements.get(0))).expression);
                else
                    console.error($"Multi-line lambda is not yet supported for Python: {TSOverviewGenerator.preview.nodeRepr(lambd)}");
                
                var params_ = lambd.parameters.map(x => this.name_(x.name));
                
                res = $"lambda {params_.join(", ")}: {body}";
            }
            else if (expr is UnaryExpression unaryExpr && unaryExpr.unaryType == UnaryType.Prefix) {
                var op = unaryExpr.operator_ == "!" ? "not " : unaryExpr.operator_;
                if (op == "++")
                    res = $"{this.expr(unaryExpr.operand)} = {this.expr(unaryExpr.operand)} + 1";
                else if (op == "--")
                    res = $"{this.expr(unaryExpr.operand)} = {this.expr(unaryExpr.operand)} - 1";
                else
                    res = $"{op}{this.expr(unaryExpr.operand)}";
            }
            else if (expr is UnaryExpression unaryExpr2 && unaryExpr2.unaryType == UnaryType.Postfix) {
                if (unaryExpr2.operator_ == "++")
                    res = $"{this.expr(unaryExpr2.operand)} = {this.expr(unaryExpr2.operand)} + 1";
                else if (unaryExpr2.operator_ == "--")
                    res = $"{this.expr(unaryExpr2.operand)} = {this.expr(unaryExpr2.operand)} - 1";
                else
                    res = $"{this.expr(unaryExpr2.operand)}{unaryExpr2.operator_}";
            }
            else if (expr is MapLiteral mapLit) {
                var repr = mapLit.items.map(item => $"{JSON.stringify(item.key)}: {this.expr(item.value)}").join(",\n");
                res = mapLit.items.length() == 0 ? "{}" : $"{{\n{this.pad(repr)}\n}}";
            }
            else if (expr is NullLiteral)
                res = $"None";
            else if (expr is AwaitExpression awaitExpr)
                res = $"{this.expr(awaitExpr.expr)}";
            else if (expr is ThisReference)
                res = $"self";
            else if (expr is StaticThisReference)
                res = $"cls";
            else if (expr is EnumReference enumRef)
                res = $"{this.enumName(enumRef.decl)}";
            else if (expr is ClassReference classRef)
                res = $"{this.name_(classRef.decl.name)}";
            else if (expr is MethodParameterReference methParRef)
                res = $"{this.name_(methParRef.decl.name)}";
            else if (expr is VariableDeclarationReference varDeclRef)
                res = $"{this.name_(varDeclRef.decl.name)}";
            else if (expr is ForVariableReference forVarRef)
                res = $"{this.name_(forVarRef.decl.name)}";
            else if (expr is ForeachVariableReference forVarRef2)
                res = $"{this.name_(forVarRef2.decl.name)}";
            else if (expr is CatchVariableReference catchVarRef)
                res = $"{this.name_(catchVarRef.decl.name)}";
            else if (expr is GlobalFunctionReference globFunctRef)
                res = $"{this.name_(globFunctRef.decl.name)}";
            else if (expr is SuperReference)
                res = $"super()";
            else if (expr is StaticFieldReference statFieldRef)
                res = $"{this.clsName(statFieldRef.decl.parentInterface)}.{this.name_(statFieldRef.decl.name)}";
            else if (expr is StaticPropertyReference statPropRef)
                res = $"{this.clsName(statPropRef.decl.parentClass)}.get_{this.name_(statPropRef.decl.name)}()";
            else if (expr is InstanceFieldReference instFieldRef)
                res = $"{this.expr(instFieldRef.object_)}.{this.name_(instFieldRef.field.name)}";
            else if (expr is InstancePropertyReference instPropRef)
                res = $"{this.expr(instPropRef.object_)}.get_{this.name_(instPropRef.property.name)}()";
            else if (expr is EnumMemberReference enumMembRef)
                res = $"{this.enumName(enumMembRef.decl.parentEnum)}.{this.enumMemberName(enumMembRef.decl.name)}";
            else if (expr is NullCoalesceExpression nullCoalExpr)
                res = $"{this.expr(nullCoalExpr.defaultExpr)} or {this.expr(nullCoalExpr.exprIfNull)}";
            else { }
            return res;
        }
        
        public string stmtDefault(Statement stmt)
        {
            var nl = "\n";
            if (stmt is BreakStatement)
                return "break";
            else if (stmt is ReturnStatement retStat)
                return retStat.expression == null ? "return" : $"return {this.expr(retStat.expression)}";
            else if (stmt is UnsetStatement unsetStat)
                return $"/* unset {this.expr(unsetStat.expression)}; */";
            else if (stmt is ThrowStatement throwStat)
                return $"raise {this.expr(throwStat.expression)}";
            else if (stmt is ExpressionStatement exprStat)
                return $"{this.expr(exprStat.expression)}";
            else if (stmt is VariableDeclaration varDecl)
                return varDecl.initializer != null ? $"{this.name_(varDecl.name)} = {this.expr(varDecl.initializer)}" : "";
            else if (stmt is ForeachStatement forStat)
                return $"for {this.name_(forStat.itemVar.name)} in {this.expr(forStat.items)}:\n{this.block(forStat.body)}";
            else if (stmt is IfStatement ifStat) {
                var elseIf = ifStat.else_ != null && ifStat.else_.statements.length() == 1 && ifStat.else_.statements.get(0) is IfStatement;
                return $"if {this.expr(ifStat.condition)}:\n{this.block(ifStat.then)}" + (elseIf ? $"\nel{this.stmt(ifStat.else_.statements.get(0))}" : "") + (!elseIf && ifStat.else_ != null ? $"\nelse:\n{this.block(ifStat.else_)}" : "");
            }
            else if (stmt is WhileStatement whileStat)
                return $"while {this.expr(whileStat.condition)}:\n{this.block(whileStat.body)}";
            else if (stmt is ForStatement forStat2)
                return (forStat2.itemVar != null ? $"{this.var(forStat2.itemVar, null)}\n" : "") + $"\nwhile {this.expr(forStat2.condition)}:\n{this.block(forStat2.body)}\n{this.pad(this.expr(forStat2.incrementor))}";
            else if (stmt is DoStatement doStat)
                return $"while True:\n{this.block(doStat.body)}\n{this.pad($"if not ({this.expr(doStat.condition)}):{nl}{this.pad("break")}")}";
            else if (stmt is TryStatement tryStat)
                return $"try:\n{this.block(tryStat.tryBody)}" + (tryStat.catchBody != null ? $"\nexcept Exception as {this.name_(tryStat.catchVar.name)}:\n{this.block(tryStat.catchBody)}" : "") + (tryStat.finallyBody != null ? $"\nfinally:\n{this.block(tryStat.finallyBody)}" : "");
            else if (stmt is ContinueStatement)
                return $"continue";
            else
                return "UNKNOWN-STATEMENT";
        }
        
        public string stmt(Statement stmt)
        {
            string res = null;
            
            if (stmt.attributes != null && stmt.attributes.hasKey("python"))
                res = stmt.attributes.get("python");
            else {
                foreach (var plugin in this.plugins) {
                    res = plugin.stmt(stmt);
                    if (res != null)
                        break;
                }
                
                if (res == null)
                    res = this.stmtDefault(stmt);
            }
            
            return this.leading(stmt) + res;
        }
        
        public string stmts(Statement[] stmts, bool skipPass = false)
        {
            return this.pad(stmts.length() == 0 && !skipPass ? "pass" : stmts.map(stmt => this.stmt(stmt)).join("\n"));
        }
        
        public string block(Block block, bool skipPass = false)
        {
            return this.stmts(block.statements.ToArray(), skipPass);
        }
        
        public string pass(string str)
        {
            return str == "" ? "pass" : str;
        }
        
        public string cls(Class cls)
        {
            if (cls.attributes.get("external") == "true")
                return "";
            this.currentClass = cls;
            var resList = new List<string>();
            var classAttributes = new List<string>();
            
            var staticFields = cls.fields.filter(x => x.isStatic);
            
            if (staticFields.length() > 0) {
                this.imports.add("import OneLangStdLib as one");
                classAttributes.push("@one.static_init");
                var fieldInits = staticFields.map(f => $"cls.{this.vis(f.visibility)}{this.var(f, f).replace(cls.name, "cls")}");
                resList.push($"@classmethod\ndef static_init(cls):\n" + this.pad(fieldInits.join("\n")));
            }
            
            var constrStmts = new List<string>();
            
            foreach (var field in cls.fields.filter(x => !x.isStatic)) {
                var init = field.constructorParam != null ? this.name_(field.constructorParam.name) : field.initializer != null ? this.expr(field.initializer) : "None";
                constrStmts.push($"self.{this.name_(field.name)} = {init}");
            }
            
            if (cls.baseClass != null) {
                if (cls.constructor_ != null && cls.constructor_.superCallArgs != null)
                    constrStmts.push($"super().__init__({cls.constructor_.superCallArgs.map(x => this.expr(x)).join(", ")})");
                else
                    constrStmts.push($"super().__init__()");
            }
            
            if (cls.constructor_ != null)
                foreach (var stmt in cls.constructor_.body.statements)
                    constrStmts.push(this.stmt(stmt));
            
            resList.push($"def __init__(self{(cls.constructor_ == null ? "" : cls.constructor_.parameters.map(p => $", {this.var(p, null)}").join(""))}):\n" + this.pad(this.pass(constrStmts.join("\n"))));
            
            foreach (var prop in cls.properties) {
                if (prop.getter != null)
                    resList.push($"def get_{this.name_(prop.name)}(self):\n{this.block(prop.getter)}");
            }
            
            var methods = new List<string>();
            foreach (var method in cls.methods) {
                if (method.body == null)
                    continue;
                // declaration only
                methods.push((method.isStatic ? "@classmethod\n" : "") + $"def {this.name_(method.name)}" + $"({(method.isStatic ? "cls" : "self")}{method.parameters.map(p => $", {this.var(p, null)}").join("")}):" + "\n" + this.block(method.body));
            }
            resList.push(methods.join("\n\n"));
            var resList2 = resList.filter(x => x != "");
            
            var clsHdr = $"class {this.clsName(cls, true)}{(cls.baseClass != null ? $"({this.clsName((((ClassType)cls.baseClass)).decl)})" : "")}:\n";
            return classAttributes.map(x => $"{x}\n").join("") + clsHdr + this.pad(resList2.length() > 0 ? resList2.join("\n\n") : "pass");
        }
        
        public string pad(string str)
        {
            return str == "" ? "" : str.split(new RegExp("\\n")).map(x => $"    {x}").join("\n");
        }
        
        public string calcRelImport(ExportScopeRef targetPath, ExportScopeRef fromPath)
        {
            var targetParts = targetPath.scopeName.split(new RegExp("/"));
            var fromParts = fromPath.scopeName.split(new RegExp("/"));
            
            var sameLevel = 0;
            while (sameLevel < targetParts.length() && sameLevel < fromParts.length() && targetParts.get(sameLevel) == fromParts.get(sameLevel))
                sameLevel++;
            
            var result = "";
            for (int i = 1; i < fromParts.length() - sameLevel; i++)
                result += ".";
            
            for (int i = sameLevel; i < targetParts.length(); i++)
                result += "." + targetParts.get(i);
            
            return result;
        }
        
        public string calcImportAlias(ExportScopeRef targetPath)
        {
            var parts = targetPath.scopeName.split(new RegExp("/"));
            var filename = parts.get(parts.length() - 1);
            return NameUtils.shortName(filename);
        }
        
        public string genFile(SourceFile sourceFile)
        {
            this.currentFile = sourceFile;
            this.imports = new Set<string>();
            this.importAllScopes = new Set<string>();
            this.imports.add("from OneLangStdLib import *");
            // TODO: do not add this globally, just for nativeResolver methods
                   
            if (sourceFile.enums.length() > 0)
                this.imports.add("from enum import Enum");
            
            foreach (var import_ in sourceFile.imports.filter(x => !x.importAll)) {
                if (import_.attributes.get("python-ignore") == "true")
                    continue;
                
                if (import_.attributes.hasKey("python-import-all")) {
                    this.imports.add($"from {import_.attributes.get("python-import-all")} import *");
                    this.importAllScopes.add(import_.exportScope.getId());
                }
                else {
                    var alias = this.calcImportAlias(import_.exportScope);
                    this.imports.add($"import {this.package.name}.{import_.exportScope.scopeName.replace(new RegExp("/"), ".")} as {alias}");
                }
            }
            
            var enums = new List<string>();
            foreach (var enum_ in sourceFile.enums) {
                var values = new List<string>();
                for (int i = 0; i < enum_.values.length(); i++)
                    values.push($"{this.enumMemberName(enum_.values.get(i).name)} = {i + 1}");
                enums.push($"class {this.enumName(enum_, true)}(Enum):\n" + this.pad(values.join("\n")));
            }
            
            var classes = new List<string>();
            foreach (var cls in sourceFile.classes)
                classes.push(this.cls(cls));
            
            var main = sourceFile.mainBlock.statements.length() > 0 ? this.block(sourceFile.mainBlock) : "";
            
            var imports = new List<string>();
            foreach (var imp in this.imports)
                imports.push(imp);
            
            return new List<string> { imports.join("\n"), enums.join("\n\n"), classes.join("\n\n"), main }.filter(x => x != "").join("\n\n");
        }
        
        public GeneratedFile[] generate(Package pkg)
        {
            this.package = pkg;
            var result = new List<GeneratedFile>();
            foreach (var path in Object.keys(pkg.files))
                result.push(new GeneratedFile($"{pkg.name}/{path}", this.genFile(pkg.files.get(path))));
            return result.ToArray();
        }
    }
}