using Generator;
using One.Ast;
using One;
using System.Collections.Generic;
using System.Linq;

namespace Generator
{
    public class CsharpGenerator : IGenerator {
        public Set<string> usings;
        public IInterface currentClass;
        public string[] reservedWords;
        public string[] fieldToMethodHack;
        public Dictionary<string, int> instanceOfIds;
        
        public CsharpGenerator()
        {
            this.reservedWords = new string[] { "object", "else", "operator", "class", "enum", "void", "string", "implicit", "Type", "Enum", "params", "using", "throw", "ref", "base", "virtual", "interface", "int", "const" };
            this.fieldToMethodHack = new string[] { "length", "size" };
            this.instanceOfIds = new Dictionary<string, int> {};
        }
        
        public string getLangName()
        {
            return "CSharp";
        }
        
        public string getExtension()
        {
            return "cs";
        }
        
        public ITransformer[] getTransforms()
        {
            return new ITransformer[0];
        }
        
        public string name_(string name)
        {
            if (this.reservedWords.includes(name))
                name += "_";
            if (this.fieldToMethodHack.includes(name))
                name += "()";
            var nameParts = name.split(new RegExp("-"));
            for (int i = 1; i < nameParts.length(); i++)
                nameParts.set(i, nameParts.get(i).get(0).toUpperCase() + nameParts.get(i).substr(1));
            name = nameParts.join("");
            return name;
        }
        
        public string leading(Statement item)
        {
            var result = "";
            if (item.leadingTrivia != null && item.leadingTrivia.length() > 0)
                result += item.leadingTrivia;
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
        
        public string typeArgs(string[] args)
        {
            return args != null && args.length() > 0 ? $"<{args.join(", ")}>" : "";
        }
        
        public string typeArgs2(IType[] args)
        {
            return this.typeArgs(args.map(x => this.type(x)));
        }
        
        public string type(IType t, bool mutates = true)
        {
            if (t is ClassType classType) {
                var typeArgs = this.typeArgs(classType.typeArguments.map(x => this.type(x)));
                if (classType.decl.name == "TsString")
                    return "string";
                else if (classType.decl.name == "TsBoolean")
                    return "bool";
                else if (classType.decl.name == "TsNumber")
                    return "int";
                else if (classType.decl.name == "TsArray") {
                    if (mutates) {
                        this.usings.add("System.Collections.Generic");
                        return $"List<{this.type(classType.typeArguments.get(0))}>";
                    }
                    else
                        return $"{this.type(classType.typeArguments.get(0))}[]";
                }
                else if (classType.decl.name == "Promise") {
                    this.usings.add("System.Threading.Tasks");
                    return classType.typeArguments.get(0) is VoidType ? "Task" : $"Task{typeArgs}";
                }
                else if (classType.decl.name == "Object") {
                    this.usings.add("System");
                    return $"object";
                }
                else if (classType.decl.name == "TsMap") {
                    this.usings.add("System.Collections.Generic");
                    return $"Dictionary<string, {this.type(classType.typeArguments.get(0))}>";
                }
                return this.name_(classType.decl.name) + typeArgs;
            }
            else if (t is InterfaceType intType)
                return $"{this.name_(intType.decl.name)}{this.typeArgs(intType.typeArguments.map(x => this.type(x)))}";
            else if (t is VoidType)
                return "void";
            else if (t is EnumType enumType)
                return $"{this.name_(enumType.decl.name)}";
            else if (t is AnyType)
                return $"object";
            else if (t is NullType)
                return $"null";
            else if (t is GenericsType genType)
                return $"{genType.typeVarName}";
            else if (t is LambdaType lambdType) {
                var isFunc = !(lambdType.returnType is VoidType);
                var paramTypes = lambdType.parameters.map(x => this.type(x.type)).ToList();
                if (isFunc)
                    paramTypes.push(this.type(lambdType.returnType));
                this.usings.add("System");
                return $"{(isFunc ? "Func" : "Action")}<{paramTypes.join(", ")}>";
            }
            else if (t == null)
                return "/* TODO */ object";
            else
                return "/* MISSING */";
        }
        
        public bool isTsArray(IType type)
        {
            return type is ClassType classType2 && classType2.decl.name == "TsArray";
        }
        
        public string vis(Visibility v)
        {
            return v == Visibility.Private ? "private" : v == Visibility.Protected ? "protected" : v == Visibility.Public ? "public" : "/* TODO: not set */public";
        }
        
        public string varWoInit(IVariable v, IHasAttributesAndTrivia attr)
        {
            string type;
            if (attr != null && attr.attributes != null && attr.attributes.hasKey("csharp-type"))
                type = attr.attributes.get("csharp-type");
            else if (v.type is ClassType classType3 && classType3.decl.name == "TsArray") {
                if (v.mutability.mutated) {
                    this.usings.add("System.Collections.Generic");
                    type = $"List<{this.type(classType3.typeArguments.get(0))}>";
                }
                else
                    type = $"{this.type(classType3.typeArguments.get(0))}[]";
            }
            else
                type = this.type(v.type);
            return $"{type} {this.name_(v.name)}";
        }
        
        public string var(IVariableWithInitializer v, IHasAttributesAndTrivia attrs)
        {
            return this.varWoInit(v, attrs) + (v.initializer != null ? $" = {this.expr(v.initializer)}" : "");
        }
        
        public string exprCall(IType[] typeArgs, Expression[] args)
        {
            return this.typeArgs2(typeArgs) + $"({args.map(x => this.expr(x)).join(", ")})";
        }
        
        public string mutateArg(Expression arg, bool shouldBeMutable)
        {
            if (this.isTsArray(arg.actualType)) {
                if (arg is ArrayLiteral arrayLit && !shouldBeMutable) {
                    var itemType = (((ClassType)arrayLit.actualType)).typeArguments.get(0);
                    return arrayLit.items.length() == 0 && !this.isTsArray(itemType) ? $"new {this.type(itemType)}[0]" : $"new {this.type(itemType)}[] {{ {arrayLit.items.map(x => this.expr(x)).join(", ")} }}";
                }
                
                var currentlyMutable = shouldBeMutable;
                if (arg is VariableReference varRef)
                    currentlyMutable = varRef.getVariable().mutability.mutated;
                else if (arg is InstanceMethodCallExpression instMethCallExpr || arg is StaticMethodCallExpression)
                    currentlyMutable = false;
                
                if (currentlyMutable && !shouldBeMutable)
                    return $"{this.expr(arg)}.ToArray()";
                else if (!currentlyMutable && shouldBeMutable) {
                    this.usings.add("System.Linq");
                    return $"{this.expr(arg)}.ToList()";
                }
            }
            return this.expr(arg);
        }
        
        public string mutatedExpr(Expression expr, Expression toWhere)
        {
            if (toWhere is VariableReference varRef2) {
                var v = varRef2.getVariable();
                if (this.isTsArray(v.type))
                    return this.mutateArg(expr, v.mutability.mutated);
            }
            return this.expr(expr);
        }
        
        public string callParams(Expression[] args, MethodParameter[] params_)
        {
            var argReprs = new List<string>();
            for (int i = 0; i < args.length(); i++)
                argReprs.push(this.isTsArray(params_.get(i).type) ? this.mutateArg(args.get(i), params_.get(i).mutability.mutated) : this.expr(args.get(i)));
            return $"({argReprs.join(", ")})";
        }
        
        public string methodCall(IMethodCallExpression expr)
        {
            return this.name_(expr.method.name) + this.typeArgs2(expr.typeArgs) + this.callParams(expr.args, expr.method.parameters);
        }
        
        public string inferExprNameForType(IType type)
        {
            if (type is ClassType classType4 && classType4.typeArguments.every((x, _) => x is ClassType)) {
                var fullName = classType4.typeArguments.map(x => (((ClassType)x)).decl.name).join("") + classType4.decl.name;
                return NameUtils.shortName(fullName);
            }
            return null;
        }
        
        public string expr(IExpression expr)
        {
            var res = "UNKNOWN-EXPR";
            if (expr is NewExpression newExpr)
                res = $"new {this.type(newExpr.cls)}{this.callParams(newExpr.args, newExpr.cls.decl.constructor_ != null ? newExpr.cls.decl.constructor_.parameters : new MethodParameter[0])}";
            else if (expr is UnresolvedNewExpression unrNewExpr)
                res = $"/* TODO: UnresolvedNewExpression */ new {this.type(unrNewExpr.cls)}({unrNewExpr.args.map(x => this.expr(x)).join(", ")})";
            else if (expr is Identifier ident)
                res = $"/* TODO: Identifier */ {ident.text}";
            else if (expr is PropertyAccessExpression propAccExpr)
                res = $"/* TODO: PropertyAccessExpression */ {this.expr(propAccExpr.object_)}.{propAccExpr.propertyName}";
            else if (expr is UnresolvedCallExpression unrCallExpr)
                res = $"/* TODO: UnresolvedCallExpression */ {this.expr(unrCallExpr.func)}{this.exprCall(unrCallExpr.typeArgs, unrCallExpr.args)}";
            else if (expr is UnresolvedMethodCallExpression unrMethCallExpr)
                res = $"/* TODO: UnresolvedMethodCallExpression */ {this.expr(unrMethCallExpr.object_)}.{unrMethCallExpr.methodName}{this.exprCall(unrMethCallExpr.typeArgs, unrMethCallExpr.args)}";
            else if (expr is InstanceMethodCallExpression instMethCallExpr2)
                res = $"{this.expr(instMethCallExpr2.object_)}.{this.methodCall(instMethCallExpr2)}";
            else if (expr is StaticMethodCallExpression statMethCallExpr)
                res = $"{this.name_(statMethCallExpr.method.parentInterface.name)}.{this.methodCall(statMethCallExpr)}";
            else if (expr is GlobalFunctionCallExpression globFunctCallExpr)
                res = $"Global.{this.name_(globFunctCallExpr.func.name)}{this.exprCall(new IType[0], globFunctCallExpr.args)}";
            else if (expr is LambdaCallExpression lambdCallExpr)
                res = $"{this.expr(lambdCallExpr.method)}({lambdCallExpr.args.map(x => this.expr(x)).join(", ")})";
            else if (expr is BooleanLiteral boolLit)
                res = $"{(boolLit.boolValue ? "true" : "false")}";
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
                    // parts.push(part.literalText.replace(new RegExp("\\n"), $"\\n").replace(new RegExp("\\r"), $"\\r").replace(new RegExp("\\t"), $"\\t").replace(new RegExp("{"), "{{").replace(new RegExp("}"), "}}").replace(new RegExp("\""), $"\\\""));
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
                            else if (chr == "\"")
                                lit += "\\\"";
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
                        var repr = this.expr(part.expression);
                        parts.push(part.expression is ConditionalExpression ? $"{{({repr})}}" : $"{{{repr}}}");
                    }
                }
                res = $"$\"{parts.join("")}\"";
            }
            else if (expr is BinaryExpression binExpr)
                res = $"{this.expr(binExpr.left)} {binExpr.operator_} {this.mutatedExpr(binExpr.right, binExpr.operator_ == "=" ? binExpr.left : null)}";
            else if (expr is ArrayLiteral arrayLit2) {
                if (arrayLit2.items.length() == 0)
                    res = $"new {this.type(arrayLit2.actualType)}()";
                else
                    res = $"new {this.type(arrayLit2.actualType)} {{ {arrayLit2.items.map(x => this.expr(x)).join(", ")} }}";
            }
            else if (expr is CastExpression castExpr) {
                if (castExpr.instanceOfCast != null && castExpr.instanceOfCast.alias != null)
                    res = this.name_(castExpr.instanceOfCast.alias);
                else
                    res = $"(({this.type(castExpr.newType)}){this.expr(castExpr.expression)})";
            }
            else if (expr is ConditionalExpression condExpr)
                res = $"{this.expr(condExpr.condition)} ? {this.expr(condExpr.whenTrue)} : {this.mutatedExpr(condExpr.whenFalse, condExpr.whenTrue)}";
            else if (expr is InstanceOfExpression instOfExpr) {
                if (instOfExpr.implicitCasts != null && instOfExpr.implicitCasts.length() > 0) {
                    var aliasPrefix = this.inferExprNameForType(instOfExpr.checkType);
                    if (aliasPrefix == null)
                        aliasPrefix = instOfExpr.expr is VariableReference varRef3 ? varRef3.getVariable().name : "obj";
                    var id = this.instanceOfIds.hasKey(aliasPrefix) ? this.instanceOfIds.get(aliasPrefix) : 1;
                    this.instanceOfIds.set(aliasPrefix, id + 1);
                    instOfExpr.alias = aliasPrefix + (id == 1 ? "" : $"{id}");
                }
                res = $"{this.expr(instOfExpr.expr)} is {this.type(instOfExpr.checkType)}{(instOfExpr.alias != null ? $" {this.name_(instOfExpr.alias)}" : "")}";
            }
            else if (expr is ParenthesizedExpression parExpr)
                res = $"({this.expr(parExpr.expression)})";
            else if (expr is RegexLiteral regexLit)
                res = $"new RegExp({JSON.stringify(regexLit.pattern)})";
            else if (expr is Lambda lambd) {
                string body;
                if (lambd.body.statements.length() == 1 && lambd.body.statements.get(0) is ReturnStatement)
                    body = this.expr((((ReturnStatement)lambd.body.statements.get(0))).expression);
                else
                    body = $"{{ {this.rawBlock(lambd.body)} }}";
                
                var params_ = lambd.parameters.map(x => this.name_(x.name));
                
                res = $"{(params_.length() == 1 ? params_.get(0) : $"({params_.join(", ")})")} => {body}";
            }
            else if (expr is UnaryExpression unaryExpr && unaryExpr.unaryType == UnaryType.Prefix)
                res = $"{unaryExpr.operator_}{this.expr(unaryExpr.operand)}";
            else if (expr is UnaryExpression unaryExpr2 && unaryExpr2.unaryType == UnaryType.Postfix)
                res = $"{this.expr(unaryExpr2.operand)}{unaryExpr2.operator_}";
            else if (expr is MapLiteral mapLit) {
                var repr = mapLit.items.map(item => $"[{JSON.stringify(item.key)}] = {this.expr(item.value)}").join(",\n");
                res = $"new {this.type(mapLit.actualType)} " + (repr == "" ? "{}" : repr.includes("\n") ? $"{{\n{this.pad(repr)}\n}}" : $"{{ {repr} }}");
            }
            else if (expr is NullLiteral)
                res = $"null";
            else if (expr is AwaitExpression awaitExpr)
                res = $"await {this.expr(awaitExpr.expr)}";
            else if (expr is ThisReference)
                res = $"this";
            else if (expr is StaticThisReference)
                res = $"{this.currentClass.name}";
            else if (expr is EnumReference enumRef)
                res = $"{this.name_(enumRef.decl.name)}";
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
                res = $"base";
            else if (expr is StaticFieldReference statFieldRef)
                res = $"{this.name_(statFieldRef.decl.parentInterface.name)}.{this.name_(statFieldRef.decl.name)}";
            else if (expr is StaticPropertyReference statPropRef)
                res = $"{this.name_(statPropRef.decl.parentClass.name)}.{this.name_(statPropRef.decl.name)}";
            else if (expr is InstanceFieldReference instFieldRef)
                res = $"{this.expr(instFieldRef.object_)}.{this.name_(instFieldRef.field.name)}";
            else if (expr is InstancePropertyReference instPropRef)
                res = $"{this.expr(instPropRef.object_)}.{this.name_(instPropRef.property.name)}";
            else if (expr is EnumMemberReference enumMembRef)
                res = $"{this.name_(enumMembRef.decl.parentEnum.name)}.{this.name_(enumMembRef.decl.name)}";
            else if (expr is NullCoalesceExpression nullCoalExpr)
                res = $"{this.expr(nullCoalExpr.defaultExpr)} ?? {this.mutatedExpr(nullCoalExpr.exprIfNull, nullCoalExpr.defaultExpr)}";
            else { }
            return res;
        }
        
        public string block(Block block, bool allowOneLiner = true)
        {
            var stmtLen = block.statements.length();
            return stmtLen == 0 ? " { }" : allowOneLiner && stmtLen == 1 && !(block.statements.get(0) is IfStatement) ? $"\n{this.pad(this.rawBlock(block))}" : $" {{\n{this.pad(this.rawBlock(block))}\n}}";
        }
        
        public string stmt(Statement stmt)
        {
            var res = "UNKNOWN-STATEMENT";
            if (stmt.attributes != null && stmt.attributes.hasKey("csharp"))
                res = stmt.attributes.get("csharp");
            else if (stmt is BreakStatement)
                res = "break;";
            else if (stmt is ReturnStatement retStat)
                res = retStat.expression == null ? "return;" : $"return {this.mutateArg(retStat.expression, false)};";
            else if (stmt is UnsetStatement unsetStat)
                res = $"/* unset {this.expr(unsetStat.expression)}; */";
            else if (stmt is ThrowStatement throwStat)
                res = $"throw {this.expr(throwStat.expression)};";
            else if (stmt is ExpressionStatement exprStat)
                res = $"{this.expr(exprStat.expression)};";
            else if (stmt is VariableDeclaration varDecl) {
                if (varDecl.initializer is NullLiteral)
                    res = $"{this.type(varDecl.type, varDecl.mutability.mutated)} {this.name_(varDecl.name)} = null;";
                else if (varDecl.initializer != null)
                    res = $"var {this.name_(varDecl.name)} = {this.mutateArg(varDecl.initializer, varDecl.mutability.mutated)};";
                else
                    res = $"{this.type(varDecl.type)} {this.name_(varDecl.name)};";
            }
            else if (stmt is ForeachStatement forStat)
                res = $"foreach (var {this.name_(forStat.itemVar.name)} in {this.expr(forStat.items)})" + this.block(forStat.body);
            else if (stmt is IfStatement ifStat) {
                var elseIf = ifStat.else_ != null && ifStat.else_.statements.length() == 1 && ifStat.else_.statements.get(0) is IfStatement;
                res = $"if ({this.expr(ifStat.condition)}){this.block(ifStat.then)}";
                res += (elseIf ? $"\nelse {this.stmt(ifStat.else_.statements.get(0))}" : "") + (!elseIf && ifStat.else_ != null ? $"\nelse" + this.block(ifStat.else_) : "");
            }
            else if (stmt is WhileStatement whileStat)
                res = $"while ({this.expr(whileStat.condition)})" + this.block(whileStat.body);
            else if (stmt is ForStatement forStat2)
                res = $"for ({(forStat2.itemVar != null ? this.var(forStat2.itemVar, null) : "")}; {this.expr(forStat2.condition)}; {this.expr(forStat2.incrementor)})" + this.block(forStat2.body);
            else if (stmt is DoStatement doStat)
                res = $"do{this.block(doStat.body)} while ({this.expr(doStat.condition)});";
            else if (stmt is TryStatement tryStat) {
                res = "try" + this.block(tryStat.tryBody, false);
                if (tryStat.catchBody != null) {
                    this.usings.add("System");
                    res += $" catch (Exception {this.name_(tryStat.catchVar.name)}) {this.block(tryStat.catchBody, false)}";
                }
                if (tryStat.finallyBody != null)
                    res += "finally" + this.block(tryStat.finallyBody);
            }
            else if (stmt is ContinueStatement)
                res = $"continue;";
            else { }
            return this.leading(stmt) + res;
        }
        
        public string stmts(Statement[] stmts)
        {
            return stmts.map(stmt => this.stmt(stmt)).join("\n");
        }
        
        public string rawBlock(Block block)
        {
            return this.stmts(block.statements.ToArray());
        }
        
        public string classLike(IInterface cls)
        {
            this.currentClass = cls;
            var resList = new List<string>();
            
            var staticConstructorStmts = new List<Statement>();
            var complexFieldInits = new List<Statement>();
            if (cls is Class class_) {
                var fieldReprs = new List<string>();
                foreach (var field in class_.fields) {
                    var isInitializerComplex = field.initializer != null && !(field.initializer is StringLiteral) && !(field.initializer is BooleanLiteral) && !(field.initializer is NumericLiteral);
                    
                    var prefix = $"{this.vis(field.visibility)} {this.preIf("static ", field.isStatic)}";
                    if (field.interfaceDeclarations.length() > 0)
                        fieldReprs.push($"{prefix}{this.varWoInit(field, field)} {{ get; set; }}");
                    else if (isInitializerComplex) {
                        if (field.isStatic)
                            staticConstructorStmts.push(new ExpressionStatement(new BinaryExpression(new StaticFieldReference(field), "=", field.initializer)));
                        else
                            complexFieldInits.push(new ExpressionStatement(new BinaryExpression(new InstanceFieldReference(new ThisReference(class_), field), "=", field.initializer)));
                        
                        fieldReprs.push($"{prefix}{this.varWoInit(field, field)};");
                    }
                    else
                        fieldReprs.push($"{prefix}{this.var(field, field)};");
                }
                resList.push(fieldReprs.join("\n"));
                
                resList.push(class_.properties.map(prop => $"{this.vis(prop.visibility)} {this.preIf("static ", prop.isStatic)}" + this.varWoInit(prop, prop) + (prop.getter != null ? $" {{\n    get {{\n{this.pad(this.block(prop.getter))}\n    }}\n}}" : "") + (prop.setter != null ? $" {{\n    set {{\n{this.pad(this.block(prop.setter))}\n    }}\n}}" : "")).join("\n"));
                
                if (staticConstructorStmts.length() > 0)
                    resList.push($"static {this.name_(class_.name)}()\n{{\n{this.pad(this.stmts(staticConstructorStmts.ToArray()))}\n}}");
                
                if (class_.constructor_ != null) {
                    var constrFieldInits = new List<Statement>();
                    foreach (var field in class_.fields.filter(x => x.constructorParam != null)) {
                        var fieldRef = new InstanceFieldReference(new ThisReference(class_), field);
                        var mpRef = new MethodParameterReference(field.constructorParam);
                        // TODO: decide what to do with "after-TypeEngine" transformations
                        mpRef.setActualType(field.type, false, false);
                        constrFieldInits.push(new ExpressionStatement(new BinaryExpression(fieldRef, "=", mpRef)));
                    }
                    
                    // @java var stmts = Stream.concat(Stream.concat(constrFieldInits.stream(), complexFieldInits.stream()), ((Class)cls).constructor_.getBody().statements.stream()).toArray(Statement[]::new);
                    // @java-import java.util.stream.Stream
                    var stmts = constrFieldInits.concat(complexFieldInits.ToArray()).concat(class_.constructor_.body.statements.ToArray());
                    resList.push("public " + this.preIf("/* throws */ ", class_.constructor_.throws) + this.name_(class_.name) + $"({class_.constructor_.parameters.map(p => this.var(p, p)).join(", ")})" + (class_.constructor_.superCallArgs != null ? $": base({class_.constructor_.superCallArgs.map(x => this.expr(x)).join(", ")})" : "") + $"\n{{\n{this.pad(this.stmts(stmts))}\n}}");
                }
                else if (complexFieldInits.length() > 0)
                    resList.push($"public {this.name_(class_.name)}()\n{{\n{this.pad(this.stmts(complexFieldInits.ToArray()))}\n}}");
            }
            else if (cls is Interface int_)
                resList.push(int_.fields.map(field => $"{this.varWoInit(field, field)} {{ get; set; }}").join("\n"));
            
            var methods = new List<string>();
            foreach (var method in cls.methods) {
                if (cls is Class && method.body == null)
                    continue;
                // declaration only
                methods.push((method.parentInterface is Interface ? "" : this.vis(method.visibility) + " ") + this.preIf("static ", method.isStatic) + this.preIf("virtual ", method.overrides == null && method.overriddenBy.length() > 0) + this.preIf("override ", method.overrides != null) + this.preIf("async ", method.async) + this.preIf("/* throws */ ", method.throws) + $"{this.type(method.returns, false)} " + this.name_(method.name) + this.typeArgs(method.typeArguments) + $"({method.parameters.map(p => this.var(p, null)).join(", ")})" + (method.body != null ? $"\n{{\n{this.pad(this.stmts(method.body.statements.ToArray()))}\n}}" : ";"));
            }
            resList.push(methods.join("\n\n"));
            return this.pad(resList.filter(x => x != "").join("\n\n"));
        }
        
        public string pad(string str)
        {
            return str.split(new RegExp("\\n")).map(x => $"    {x}").join("\n");
        }
        
        public string pathToNs(string path)
        {
            // Generator/ExprLang/ExprLangAst.ts -> Generator.ExprLang
            var parts = path.split(new RegExp("/")).ToList();
            parts.pop();
            return parts.join(".");
        }
        
        public string genFile(SourceFile sourceFile)
        {
            this.instanceOfIds = new Dictionary<string, int> {};
            this.usings = new Set<string>();
            var enums = sourceFile.enums.map(enum_ => $"public enum {this.name_(enum_.name)} {{ {enum_.values.map(x => this.name_(x.name)).join(", ")} }}");
            
            var intfs = sourceFile.interfaces.map(intf => $"public interface {this.name_(intf.name)}{this.typeArgs(intf.typeArguments)}" + $"{this.preArr(" : ", intf.baseInterfaces.map(x => this.type(x)))} {{\n{this.classLike(intf)}\n}}");
            
            var classes = new List<string>();
            foreach (var cls in sourceFile.classes) {
                var baseClasses = new List<IType>();
                if (cls.baseClass != null)
                    baseClasses.push(cls.baseClass);
                foreach (var intf in cls.baseInterfaces)
                    baseClasses.push(intf);
                classes.push($"public class {this.name_(cls.name)}{this.typeArgs(cls.typeArguments)}{this.preArr(" : ", baseClasses.map(x => this.type(x)))} {{\n{this.classLike(cls)}\n}}");
            }
            
            var main = sourceFile.mainBlock.statements.length() > 0 ? $"public class Program\n{{\n    static void Main(string[] args)\n    {{\n{this.pad(this.rawBlock(sourceFile.mainBlock))}\n    }}\n}}" : "";
            
            // @java var usingsSet = new LinkedHashSet<String>(Arrays.stream(sourceFile.imports).map(x -> this.pathToNs(x.exportScope.scopeName)).filter(x -> x != "").collect(Collectors.toList()));
            // @java-import java.util.LinkedHashSet
            var usingsSet = new Set<string>(sourceFile.imports.map(x => this.pathToNs(x.exportScope.scopeName)).filter(x => x != ""));
            foreach (var using_ in this.usings.values())
                usingsSet.add(using_);
            
            var usings = new List<string>();
            foreach (var using_ in usingsSet.values())
                usings.push($"using {using_};");
            usings.sort();
            
            var result = new List<string> { enums.join("\n"), intfs.join("\n\n"), classes.join("\n\n"), main }.filter(x => x != "").join("\n\n");
            var nl = "\n";
            // Python fix
            result = $"{usings.join(nl)}\n\nnamespace {this.pathToNs(sourceFile.sourcePath.path)}\n{{\n{this.pad(result)}\n}}";
            return result;
        }
        
        public GeneratedFile[] generate(Package pkg)
        {
            var result = new List<GeneratedFile>();
            foreach (var path in Object.keys(pkg.files))
                result.push(new GeneratedFile(path, this.genFile(pkg.files.get(path))));
            return result.ToArray();
        }
    }
}