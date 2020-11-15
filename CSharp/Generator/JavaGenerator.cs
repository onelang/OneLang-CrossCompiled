using Generator.JavaPlugins;
using Generator;
using One.Ast;
using One.Transforms;
using One;
using System.Collections.Generic;
using System.Linq;

namespace Generator
{
    public class JavaGenerator : IGenerator {
        public Set<string> imports;
        public IInterface currentClass;
        public string[] reservedWords;
        public string[] fieldToMethodHack;
        public List<IGeneratorPlugin> plugins;
        
        public JavaGenerator()
        {
            this.imports = new Set<string>();
            this.reservedWords = new string[] { "class", "interface", "throws", "package", "throw", "boolean" };
            this.fieldToMethodHack = new string[0];
            this.plugins = new List<IGeneratorPlugin>();
            this.plugins.push(new JsToJava(this));
        }
        
        public string getLangName()
        {
            return "Java";
        }
        
        public string getExtension()
        {
            return "java";
        }
        
        public ITransformer[] getTransforms()
        {
            return new ITransformer[] { ((ITransformer)new ConvertNullCoalesce()), ((ITransformer)new UseDefaultCallArgsExplicitly()) };
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
            if (name == "_")
                name = "unused";
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
        
        public string type(IType t, bool mutates = true, bool isNew = false)
        {
            if (t is ClassType classType || t is InterfaceType) {
                var decl = (((IInterfaceType)t)).getDecl();
                if (decl.parentFile.exportScope != null)
                    this.imports.add(this.toImport(decl.parentFile.exportScope) + "." + decl.name);
            }
            
            if (t is ClassType classType2) {
                var typeArgs = this.typeArgs(classType2.typeArguments.map(x => this.type(x)));
                if (classType2.decl.name == "TsString")
                    return "String";
                else if (classType2.decl.name == "TsBoolean")
                    return "Boolean";
                else if (classType2.decl.name == "TsNumber")
                    return "Integer";
                else if (classType2.decl.name == "TsArray") {
                    var realType = isNew ? "ArrayList" : "List";
                    if (mutates) {
                        this.imports.add($"java.util.{realType}");
                        return $"{realType}<{this.type(classType2.typeArguments.get(0))}>";
                    }
                    else
                        return $"{this.type(classType2.typeArguments.get(0))}[]";
                }
                else if (classType2.decl.name == "Map") {
                    var realType = isNew ? "LinkedHashMap" : "Map";
                    this.imports.add($"java.util.{realType}");
                    return $"{realType}<{this.type(classType2.typeArguments.get(0))}, {this.type(classType2.typeArguments.get(1))}>";
                }
                else if (classType2.decl.name == "Set") {
                    var realType = isNew ? "LinkedHashSet" : "Set";
                    this.imports.add($"java.util.{realType}");
                    return $"{realType}<{this.type(classType2.typeArguments.get(0))}>";
                }
                else if (classType2.decl.name == "Promise")
                    return classType2.typeArguments.get(0) is VoidType ? "void" : $"{this.type(classType2.typeArguments.get(0))}";
                else if (classType2.decl.name == "Object")
                    //this.imports.add("System");
                    return $"Object";
                else if (classType2.decl.name == "TsMap") {
                    var realType = isNew ? "LinkedHashMap" : "Map";
                    this.imports.add($"java.util.{realType}");
                    return $"{realType}<String, {this.type(classType2.typeArguments.get(0))}>";
                }
                return this.name_(classType2.decl.name) + typeArgs;
            }
            else if (t is InterfaceType intType)
                return $"{this.name_(intType.decl.name)}{this.typeArgs(intType.typeArguments.map(x => this.type(x)))}";
            else if (t is VoidType)
                return "void";
            else if (t is EnumType enumType)
                return $"{this.name_(enumType.decl.name)}";
            else if (t is AnyType)
                return $"Object";
            else if (t is NullType)
                return $"null";
            else if (t is GenericsType genType)
                return $"{genType.typeVarName}";
            else if (t is LambdaType lambdType) {
                var isFunc = !(lambdType.returnType is VoidType);
                var paramTypes = lambdType.parameters.map(x => this.type(x.type)).ToList();
                if (isFunc)
                    paramTypes.push(this.type(lambdType.returnType));
                this.imports.add("java.util.function." + (isFunc ? "Function" : "Consumer"));
                return $"{(isFunc ? "Function" : "Consumer")}<{paramTypes.join(", ")}>";
            }
            else if (t == null)
                return "/* TODO */ object";
            else
                return "/* MISSING */";
        }
        
        public bool isTsArray(IType type)
        {
            return type is ClassType classType3 && classType3.decl.name == "TsArray";
        }
        
        public string vis(Visibility v)
        {
            return v == Visibility.Private ? "private" : v == Visibility.Protected ? "protected" : v == Visibility.Public ? "public" : "/* TODO: not set */public";
        }
        
        public string varType(IVariable v, IHasAttributesAndTrivia attr)
        {
            string type;
            if (attr != null && attr.attributes != null && attr.attributes.hasKey("java-type"))
                type = attr.attributes.get("java-type");
            else if (v.type is ClassType classType4 && classType4.decl.name == "TsArray") {
                if (v.mutability.mutated) {
                    this.imports.add("java.util.List");
                    type = $"List<{this.type(classType4.typeArguments.get(0))}>";
                }
                else
                    type = $"{this.type(classType4.typeArguments.get(0))}[]";
            }
            else
                type = this.type(v.type);
            return type;
        }
        
        public string varWoInit(IVariable v, IHasAttributesAndTrivia attr)
        {
            return $"{this.varType(v, attr)} {this.name_(v.name)}";
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
                var itemType = (((ClassType)arg.actualType)).typeArguments.get(0);
                if (arg is ArrayLiteral arrayLit && !shouldBeMutable)
                    return arrayLit.items.length() == 0 && !this.isTsArray(itemType) ? $"new {this.type(itemType)}[0]" : $"new {this.type(itemType)}[] {{ {arrayLit.items.map(x => this.expr(x)).join(", ")} }}";
                
                var currentlyMutable = shouldBeMutable;
                if (arg is VariableReference varRef)
                    currentlyMutable = varRef.getVariable().mutability.mutated;
                else if (arg is InstanceMethodCallExpression instMethCallExpr || arg is StaticMethodCallExpression)
                    currentlyMutable = false;
                
                if (currentlyMutable && !shouldBeMutable)
                    return $"{this.expr(arg)}.toArray({this.type(itemType)}[]::new)";
                else if (!currentlyMutable && shouldBeMutable) {
                    this.imports.add("java.util.Arrays");
                    this.imports.add("java.util.ArrayList");
                    return $"new ArrayList<>(Arrays.asList({this.expr(arg)}))";
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
            if (type is ClassType classType5 && classType5.typeArguments.every((x, _) => x is ClassType)) {
                var fullName = classType5.typeArguments.map(x => (((ClassType)x)).decl.name).join("") + classType5.decl.name;
                return NameUtils.shortName(fullName);
            }
            return null;
        }
        
        public bool isSetExpr(VariableReference varRef)
        {
            return varRef.parentNode is BinaryExpression binExpr && binExpr.left == varRef && new List<string> { "=", "+=", "-=" }.includes(binExpr.operator_);
        }
        
        public string expr(IExpression expr)
        {
            foreach (var plugin in this.plugins) {
                var result = plugin.expr(expr);
                if (result != null)
                    return result;
            }
            
            var res = "UNKNOWN-EXPR";
            if (expr is NewExpression newExpr)
                res = $"new {this.type(newExpr.cls, true, true)}{this.callParams(newExpr.args, newExpr.cls.decl.constructor_ != null ? newExpr.cls.decl.constructor_.parameters : new MethodParameter[0])}";
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
                res = $"{this.expr(lambdCallExpr.method)}.apply({lambdCallExpr.args.map(x => this.expr(x)).join(", ")})";
            else if (expr is BooleanLiteral boolLit)
                res = $"{(boolLit.boolValue ? "true" : "false")}";
            else if (expr is StringLiteral strLit)
                res = $"{JSON.stringify(strLit.stringValue)}";
            else if (expr is NumericLiteral numLit)
                res = $"{numLit.valueAsText}";
            else if (expr is CharacterLiteral charLit)
                res = $"'{charLit.charValue}'";
            else if (expr is ElementAccessExpression elemAccExpr)
                res = $"{this.expr(elemAccExpr.object_)}.get({this.expr(elemAccExpr.elementExpr)})";
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
                            else if (chr == "\"")
                                lit += "\\\"";
                            else {
                                var chrCode = chr.charCodeAt(0);
                                if (32 <= chrCode && chrCode <= 126)
                                    lit += chr;
                                else
                                    throw new Error($"invalid char in template string (code={chrCode})");
                            }
                        }
                        parts.push($"\"{lit}\"");
                    }
                    else {
                        var repr = this.expr(part.expression);
                        parts.push(part.expression is ConditionalExpression ? $"({repr})" : repr);
                    }
                }
                res = parts.join(" + ");
            }
            else if (expr is BinaryExpression binExpr2) {
                var modifies = new List<string> { "=", "+=", "-=" }.includes(binExpr2.operator_);
                if (modifies && binExpr2.left is InstanceFieldReference instFieldRef && this.useGetterSetter(instFieldRef))
                    res = $"{this.expr(instFieldRef.object_)}.set{this.ucFirst(instFieldRef.field.name)}({this.mutatedExpr(binExpr2.right, binExpr2.operator_ == "=" ? instFieldRef : null)})";
                else if (new List<string> { "==", "!=" }.includes(binExpr2.operator_)) {
                    var lit = this.currentClass.parentFile.literalTypes;
                    var leftType = binExpr2.left.getType();
                    var rightType = binExpr2.right.getType();
                    var useEquals = TypeHelper.equals(leftType, lit.string_) && rightType != null && TypeHelper.equals(rightType, lit.string_);
                    if (useEquals) {
                        this.imports.add("OneStd.Objects");
                        res = $"{(binExpr2.operator_ == "!=" ? "!" : "")}Objects.equals({this.expr(binExpr2.left)}, {this.expr(binExpr2.right)})";
                    }
                    else
                        res = $"{this.expr(binExpr2.left)} {binExpr2.operator_} {this.expr(binExpr2.right)}";
                }
                else
                    res = $"{this.expr(binExpr2.left)} {binExpr2.operator_} {this.mutatedExpr(binExpr2.right, binExpr2.operator_ == "=" ? binExpr2.left : null)}";
            }
            else if (expr is ArrayLiteral arrayLit2) {
                if (arrayLit2.items.length() == 0)
                    res = $"new {this.type(arrayLit2.actualType, true, true)}()";
                else {
                    this.imports.add($"java.util.List");
                    this.imports.add($"java.util.ArrayList");
                    res = $"new ArrayList<>(List.of({arrayLit2.items.map(x => this.expr(x)).join(", ")}))";
                }
            }
            else if (expr is CastExpression castExpr)
                res = $"(({this.type(castExpr.newType)}){this.expr(castExpr.expression)})";
            else if (expr is ConditionalExpression condExpr)
                res = $"{this.expr(condExpr.condition)} ? {this.expr(condExpr.whenTrue)} : {this.mutatedExpr(condExpr.whenFalse, condExpr.whenTrue)}";
            else if (expr is InstanceOfExpression instOfExpr)
                res = $"{this.expr(instOfExpr.expr)} instanceof {this.type(instOfExpr.checkType)}";
            else if (expr is ParenthesizedExpression parExpr)
                res = $"({this.expr(parExpr.expression)})";
            else if (expr is RegexLiteral regexLit) {
                this.imports.add($"OneStd.RegExp");
                res = $"new RegExp({JSON.stringify(regexLit.pattern)})";
            }
            else if (expr is Lambda lambd) {
                string body;
                if (lambd.body.statements.length() == 1 && lambd.body.statements.get(0) is ReturnStatement)
                    body = " " + this.expr((((ReturnStatement)lambd.body.statements.get(0))).expression);
                else
                    body = this.block(lambd.body, false);
                
                var params_ = lambd.parameters.map(x => this.name_(x.name));
                
                res = $"{(params_.length() == 1 ? params_.get(0) : $"({params_.join(", ")})")} ->{body}";
            }
            else if (expr is UnaryExpression unaryExpr && unaryExpr.unaryType == UnaryType.Prefix)
                res = $"{unaryExpr.operator_}{this.expr(unaryExpr.operand)}";
            else if (expr is UnaryExpression unaryExpr2 && unaryExpr2.unaryType == UnaryType.Postfix)
                res = $"{this.expr(unaryExpr2.operand)}{unaryExpr2.operator_}";
            else if (expr is MapLiteral mapLit) {
                if (mapLit.items.length() > 10)
                    throw new Error("MapLiteral is only supported with maximum of 10 items");
                if (mapLit.items.length() == 0)
                    res = $"new {this.type(mapLit.actualType, true, true)}()";
                else {
                    this.imports.add($"java.util.Map");
                    var repr = mapLit.items.map(item => $"{JSON.stringify(item.key)}, {this.expr(item.value)}").join(", ");
                    res = $"Map.of({repr})";
                }
            }
            else if (expr is NullLiteral)
                res = $"null";
            else if (expr is AwaitExpression awaitExpr)
                res = $"{this.expr(awaitExpr.expr)}";
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
                res = $"super";
            else if (expr is StaticFieldReference statFieldRef)
                res = $"{this.name_(statFieldRef.decl.parentInterface.name)}.{this.name_(statFieldRef.decl.name)}";
            else if (expr is StaticPropertyReference statPropRef)
                res = $"{this.name_(statPropRef.decl.parentClass.name)}.{this.name_(statPropRef.decl.name)}";
            else if (expr is InstanceFieldReference instFieldRef2) {
                // TODO: unified handling of field -> property conversion?
                if (this.useGetterSetter(instFieldRef2))
                    res = $"{this.expr(instFieldRef2.object_)}.get{this.ucFirst(instFieldRef2.field.name)}()";
                else
                    res = $"{this.expr(instFieldRef2.object_)}.{this.name_(instFieldRef2.field.name)}";
            }
            else if (expr is InstancePropertyReference instPropRef)
                res = $"{this.expr(instPropRef.object_)}.{(this.isSetExpr(instPropRef) ? "set" : "get")}{this.ucFirst(instPropRef.property.name)}()";
            else if (expr is EnumMemberReference enumMembRef)
                res = $"{this.name_(enumMembRef.decl.parentEnum.name)}.{this.name_(enumMembRef.decl.name)}";
            else if (expr is NullCoalesceExpression nullCoalExpr)
                res = $"{this.expr(nullCoalExpr.defaultExpr)} != null ? {this.expr(nullCoalExpr.defaultExpr)} : {this.mutatedExpr(nullCoalExpr.exprIfNull, nullCoalExpr.defaultExpr)}";
            else { }
            return res;
        }
        
        public bool useGetterSetter(InstanceFieldReference fieldRef)
        {
            return fieldRef.object_.actualType is InterfaceType || (fieldRef.field.interfaceDeclarations != null && fieldRef.field.interfaceDeclarations.length() > 0);
        }
        
        public string block(Block block, bool allowOneLiner = true)
        {
            var stmtLen = block.statements.length();
            return stmtLen == 0 ? " { }" : allowOneLiner && stmtLen == 1 && !(block.statements.get(0) is IfStatement) && !(block.statements.get(0) is VariableDeclaration) ? $"\n{this.pad(this.rawBlock(block))}" : $" {{\n{this.pad(this.rawBlock(block))}\n}}";
        }
        
        public string stmtDefault(Statement stmt)
        {
            var res = "UNKNOWN-STATEMENT";
            if (stmt is BreakStatement)
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
                res = $"for (var {this.name_(forStat.itemVar.name)} : {this.expr(forStat.items)})" + this.block(forStat.body);
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
                if (tryStat.catchBody != null)
                    //this.imports.add("System");
                    res += $" catch (Exception {this.name_(tryStat.catchVar.name)}) {this.block(tryStat.catchBody, false)}";
                if (tryStat.finallyBody != null)
                    res += "finally" + this.block(tryStat.finallyBody);
            }
            else if (stmt is ContinueStatement)
                res = $"continue;";
            else { }
            return res;
        }
        
        public string stmt(Statement stmt)
        {
            string res = null;
            
            if (stmt.attributes != null && stmt.attributes.hasKey("java-import"))
                this.imports.add(stmt.attributes.get("java-import"));
            
            if (stmt.attributes != null && stmt.attributes.hasKey("java"))
                res = stmt.attributes.get("java");
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
        
        public string stmts(Statement[] stmts)
        {
            return stmts.map(stmt => this.stmt(stmt)).join("\n");
        }
        
        public string rawBlock(Block block)
        {
            return this.stmts(block.statements.ToArray());
        }
        
        public string methodGen(string prefix, MethodParameter[] params_, string body)
        {
            return $"{prefix}({params_.map(p => this.varWoInit(p, p)).join(", ")}){body}";
        }
        
        public string method(Method method, bool isCls)
        {
            // TODO: final
            var prefix = (isCls ? this.vis(method.visibility) + " " : "") + this.preIf("static ", method.isStatic) + this.preIf("/* throws */ ", method.throws) + (method.typeArguments.length() > 0 ? $"<{method.typeArguments.join(", ")}> " : "") + $"{this.type(method.returns, false)} " + this.name_(method.name);
            
            return this.methodGen(prefix, method.parameters, method.body == null ? ";" : $" {{\n{this.pad(this.stmts(method.body.statements.ToArray()))}\n}}");
        }
        
        public string class_(Class cls)
        {
            this.currentClass = cls;
            var resList = new List<string>();
            
            var staticConstructorStmts = new List<Statement>();
            var complexFieldInits = new List<Statement>();
            var fieldReprs = new List<string>();
            var propReprs = new List<string>();
            foreach (var field in cls.fields) {
                var isInitializerComplex = field.initializer != null && !(field.initializer is StringLiteral) && !(field.initializer is BooleanLiteral) && !(field.initializer is NumericLiteral);
                
                var prefix = $"{this.vis(field.visibility)} {this.preIf("static ", field.isStatic)}";
                if (field.interfaceDeclarations.length() > 0) {
                    var varType = this.varType(field, field);
                    var name = this.name_(field.name);
                    var pname = this.ucFirst(field.name);
                    var setToFalse = TypeHelper.equals(field.type, this.currentClass.parentFile.literalTypes.boolean);
                    propReprs.push($"{varType} {name}{(setToFalse ? " = false" : field.initializer != null ? $" = {this.expr(field.initializer)}" : "")};\n" + $"{prefix}{varType} get{pname}() {{ return this.{name}; }}\n" + $"{prefix}void set{pname}({varType} value) {{ this.{name} = value; }}");
                }
                else if (isInitializerComplex) {
                    if (field.isStatic)
                        staticConstructorStmts.push(new ExpressionStatement(new BinaryExpression(new StaticFieldReference(field), "=", field.initializer)));
                    else
                        complexFieldInits.push(new ExpressionStatement(new BinaryExpression(new InstanceFieldReference(new ThisReference(cls), field), "=", field.initializer)));
                    
                    fieldReprs.push($"{prefix}{this.varWoInit(field, field)};");
                }
                else
                    fieldReprs.push($"{prefix}{this.var(field, field)};");
            }
            resList.push(fieldReprs.join("\n"));
            resList.push(propReprs.join("\n\n"));
            
            foreach (var prop in cls.properties) {
                var prefix = $"{this.vis(prop.visibility)} {this.preIf("static ", prop.isStatic)}";
                if (prop.getter != null)
                    resList.push($"{prefix}{this.type(prop.type)} get{this.ucFirst(prop.name)}(){this.block(prop.getter, false)}");
                
                if (prop.setter != null)
                    resList.push($"{prefix}void set{this.ucFirst(prop.name)}({this.type(prop.type)} value){this.block(prop.setter, false)}");
            }
            
            if (staticConstructorStmts.length() > 0)
                resList.push($"static {{\n{this.pad(this.stmts(staticConstructorStmts.ToArray()))}\n}}");
            
            if (cls.constructor_ != null) {
                var constrFieldInits = new List<Statement>();
                foreach (var field in cls.fields.filter(x => x.constructorParam != null)) {
                    var fieldRef = new InstanceFieldReference(new ThisReference(cls), field);
                    var mpRef = new MethodParameterReference(field.constructorParam);
                    // TODO: decide what to do with "after-TypeEngine" transformations
                    mpRef.setActualType(field.type, false, false);
                    constrFieldInits.push(new ExpressionStatement(new BinaryExpression(fieldRef, "=", mpRef)));
                }
                
                var superCall = cls.constructor_.superCallArgs != null ? $"super({cls.constructor_.superCallArgs.map(x => this.expr(x)).join(", ")});\n" : "";
                
                // TODO: super calls
                resList.push(this.methodGen("public " + this.preIf("/* throws */ ", cls.constructor_.throws) + this.name_(cls.name), cls.constructor_.parameters, $"\n{{\n{this.pad(superCall + this.stmts(constrFieldInits.concat(complexFieldInits.ToArray()).concat(cls.constructor_.body.statements.ToArray())))}\n}}"));
            }
            else if (complexFieldInits.length() > 0)
                resList.push($"public {this.name_(cls.name)}()\n{{\n{this.pad(this.stmts(complexFieldInits.ToArray()))}\n}}");
            
            var methods = new List<string>();
            foreach (var method in cls.methods) {
                if (method.body == null)
                    continue;
                // declaration only
                methods.push(this.method(method, true));
            }
            resList.push(methods.join("\n\n"));
            return this.pad(resList.filter(x => x != "").join("\n\n"));
        }
        
        public string ucFirst(string str)
        {
            return str.get(0).toUpperCase() + str.substr(1);
        }
        
        public string interface_(Interface intf)
        {
            this.currentClass = intf;
            
            var resList = new List<string>();
            foreach (var field in intf.fields) {
                var varType = this.varType(field, field);
                var name = this.ucFirst(field.name);
                resList.push($"{varType} get{name}();\nvoid set{name}({varType} value);");
            }
            
            resList.push(intf.methods.map(method => this.method(method, false)).join("\n"));
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
        
        public string importsHead()
        {
            var imports = new List<string>();
            foreach (var imp in this.imports.values())
                imports.push(imp);
            this.imports = new Set<string>();
            return imports.length() == 0 ? "" : imports.map(x => $"import {x};").join("\n") + "\n\n";
        }
        
        public string toImport(ExportScopeRef scope)
        {
            return scope.scopeName == "index" ? $"OneStd" : $"{scope.packageName}.{scope.scopeName.replace(new RegExp("\\.ts$"), "").replace(new RegExp("/"), ".")}";
        }
        
        public GeneratedFile[] generate(Package pkg)
        {
            var result = new List<GeneratedFile>();
            foreach (var path in Object.keys(pkg.files)) {
                var file = pkg.files.get(path);
                var packagePath = $"{pkg.name}/{file.sourcePath.path.replace(new RegExp("\\.ts$"), "")}";
                var dstDir = $"src/main/java/{packagePath}";
                var packageName = packagePath.replace(new RegExp("/"), ".");
                
                var imports = new Set<string>();
                foreach (var impList in file.imports) {
                    var impPkg = this.toImport(impList.exportScope);
                    foreach (var imp in impList.imports)
                        imports.add($"{impPkg}.{imp.name}");
                }
                
                var head = $"package {packageName};\n\n{Array.from(imports.values()).map(x => $"import {x};").join("\n")}\n\n";
                
                foreach (var enum_ in file.enums)
                    result.push(new GeneratedFile($"{dstDir}/{enum_.name}.java", $"{head}public enum {this.name_(enum_.name)} {{ {enum_.values.map(x => this.name_(x.name)).join(", ")} }}"));
                
                foreach (var intf in file.interfaces) {
                    var res = $"public interface {this.name_(intf.name)}{this.typeArgs(intf.typeArguments)}" + $"{this.preArr(" extends ", intf.baseInterfaces.map(x => this.type(x)))} {{\n{this.interface_(intf)}\n}}";
                    result.push(new GeneratedFile($"{dstDir}/{intf.name}.java", $"{head}{this.importsHead()}{res}"));
                }
                
                foreach (var cls in file.classes) {
                    var res = $"public class {this.name_(cls.name)}{this.typeArgs(cls.typeArguments)}" + (cls.baseClass != null ? $" extends {this.type(cls.baseClass)}" : "") + this.preArr(" implements ", cls.baseInterfaces.map(x => this.type(x))) + $" {{\n{this.class_(cls)}\n}}";
                    result.push(new GeneratedFile($"{dstDir}/{cls.name}.java", $"{head}{this.importsHead()}{res}"));
                }
            }
            return result.ToArray();
        }
    }
}