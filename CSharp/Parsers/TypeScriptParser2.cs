using One.Ast;
using Parsers.Common;
using System.Collections.Generic;
using System.Linq;

namespace Parsers
{
    public class TypeAndInit {
        public IType type;
        public Expression init;
        
        public TypeAndInit(IType type, Expression init)
        {
            this.type = type;
            this.init = init;
        }
    }
    
    public class MethodSignature {
        public MethodParameter[] params_;
        public Field[] fields;
        public Block body;
        public IType returns;
        public Expression[] superCallArgs;
        
        public MethodSignature(MethodParameter[] params_, Field[] fields, Block body, IType returns, Expression[] superCallArgs)
        {
            this.params_ = params_;
            this.fields = fields;
            this.body = body;
            this.returns = returns;
            this.superCallArgs = superCallArgs;
        }
    }
    
    public class TypeScriptParser2 : IParser, IExpressionParserHooks, IReaderHooks {
        public List<string> context;
        public Reader reader;
        public ExpressionParser expressionParser;
        public NodeManager nodeManager { get; set; }
        public ExportScopeRef exportScope;
        public bool missingReturnTypeIsVoid = false;
        public SourcePath path;
        
        public TypeScriptParser2(string source, SourcePath path = null)
        {
            this.path = path;
            this.context = new List<string>();
            this.reader = new Reader(source);
            this.reader.hooks = this;
            this.nodeManager = new NodeManager(this.reader);
            this.expressionParser = this.createExpressionParser(this.reader, this.nodeManager);
            this.exportScope = this.path != null ? new ExportScopeRef(this.path.pkg.name, this.path.path != null ? this.path.path.replace(new RegExp(".ts$"), "") : null) : null;
        }
        
        public ExpressionParser createExpressionParser(Reader reader, NodeManager nodeManager = null)
        {
            var expressionParser = new ExpressionParser(reader, this, nodeManager);
            expressionParser.stringLiteralType = new UnresolvedType("TsString", new IType[0]);
            expressionParser.numericLiteralType = new UnresolvedType("TsNumber", new IType[0]);
            return expressionParser;
        }
        
        public void errorCallback(ParseError error)
        {
            throw new Error($"[TypeScriptParser] {error.message} at {error.cursor.line}:{error.cursor.column} (context: {this.context.join("/")})\n{this.reader.linePreview(error.cursor)}");
        }
        
        public Expression infixPrehook(Expression left)
        {
            if (left is PropertyAccessExpression propAccExpr && this.reader.peekRegex("<[A-Za-z0-9_<>]*?>\\(") != null) {
                var typeArgs = this.parseTypeArgs();
                this.reader.expectToken("(");
                var args = this.expressionParser.parseCallArguments();
                return new UnresolvedCallExpression(propAccExpr, typeArgs, args);
            }
            else if (this.reader.readToken("instanceof")) {
                var type = this.parseType();
                return new InstanceOfExpression(left, type);
            }
            else if (left is Identifier ident && this.reader.readToken("=>")) {
                var block = this.parseLambdaBlock();
                return new Lambda(new MethodParameter[] { new MethodParameter(ident.text, null, null, null) }, block);
            }
            return null;
        }
        
        public MethodParameter[] parseLambdaParams()
        {
            if (!this.reader.readToken("("))
                return null;
            
            var params_ = new List<MethodParameter>();
            if (!this.reader.readToken(")")) {
                do {
                    var paramName = this.reader.expectIdentifier();
                    var type = this.reader.readToken(":") ? this.parseType() : null;
                    params_.push(new MethodParameter(paramName, type, null, null));
                } while (this.reader.readToken(","));
                this.reader.expectToken(")");
            }
            return params_.ToArray();
        }
        
        public IType parseType()
        {
            if (this.reader.readToken("{")) {
                this.reader.expectToken("[");
                this.reader.readIdentifier();
                this.reader.expectToken(":");
                this.reader.expectToken("string");
                this.reader.expectToken("]");
                this.reader.expectToken(":");
                var mapValueType = this.parseType();
                this.reader.readToken(";");
                this.reader.expectToken("}");
                return new UnresolvedType("TsMap", new IType[] { mapValueType });
            }
            
            if (this.reader.peekToken("(")) {
                var params_ = this.parseLambdaParams();
                this.reader.expectToken("=>");
                var returnType = this.parseType();
                return new LambdaType(params_, returnType);
            }
            
            var typeName = this.reader.expectIdentifier();
            var startPos = this.reader.prevTokenOffset;
            
            IType type;
            if (typeName == "string")
                type = new UnresolvedType("TsString", new IType[0]);
            else if (typeName == "boolean")
                type = new UnresolvedType("TsBoolean", new IType[0]);
            else if (typeName == "number")
                type = new UnresolvedType("TsNumber", new IType[0]);
            else if (typeName == "any")
                type = AnyType.instance;
            else if (typeName == "void")
                type = VoidType.instance;
            else {
                var typeArguments = this.parseTypeArgs();
                type = new UnresolvedType(typeName, typeArguments);
            }
            
            this.nodeManager.addNode(type, startPos);
            
            while (this.reader.readToken("[]")) {
                type = new UnresolvedType("TsArray", new IType[] { type });
                this.nodeManager.addNode(type, startPos);
            }
            
            return type;
        }
        
        public Expression parseExpression()
        {
            return this.expressionParser.parse();
        }
        
        public Expression unaryPrehook()
        {
            if (this.reader.readToken("null"))
                return new NullLiteral();
            else if (this.reader.readToken("true"))
                return new BooleanLiteral(true);
            else if (this.reader.readToken("false"))
                return new BooleanLiteral(false);
            else if (this.reader.readToken("`")) {
                var parts = new List<TemplateStringPart>();
                var litPart = "";
                while (true) {
                    if (this.reader.readExactly("`")) {
                        if (litPart != "") {
                            parts.push(TemplateStringPart.Literal(litPart));
                            litPart = "";
                        }
                        
                        break;
                    }
                    else if (this.reader.readExactly("${")) {
                        if (litPart != "") {
                            parts.push(TemplateStringPart.Literal(litPart));
                            litPart = "";
                        }
                        
                        var expr = this.parseExpression();
                        parts.push(TemplateStringPart.Expression(expr));
                        this.reader.expectToken("}");
                    }
                    else if (this.reader.readExactly("\\")) {
                        var chr = this.reader.readChar();
                        if (chr == "n")
                            litPart += "\n";
                        else if (chr == "r")
                            litPart += "\r";
                        else if (chr == "t")
                            litPart += "\t";
                        else if (chr == "`")
                            litPart += "`";
                        else if (chr == "$")
                            litPart += "$";
                        else if (chr == "\\")
                            litPart += "\\";
                        else
                            this.reader.fail("invalid escape", this.reader.offset - 1);
                    }
                    else {
                        var chr = this.reader.readChar();
                        var chrCode = chr.charCodeAt(0);
                        if (!(32 <= chrCode && chrCode <= 126) || chr == "`" || chr == "\\")
                            this.reader.fail($"not allowed character (code={chrCode})", this.reader.offset - 1);
                        litPart += chr;
                    }
                }
                return new TemplateString(parts.ToArray());
            }
            else if (this.reader.readToken("new")) {
                var type = this.parseType();
                if (type is UnresolvedType unrType) {
                    this.reader.expectToken("(");
                    var args = this.expressionParser.parseCallArguments();
                    return new UnresolvedNewExpression(unrType, args);
                }
                else
                    throw new Error($"[TypeScriptParser2] Expected UnresolvedType here!");
            }
            else if (this.reader.readToken("<")) {
                var newType = this.parseType();
                this.reader.expectToken(">");
                var expression = this.parseExpression();
                return new CastExpression(newType, expression);
            }
            else if (this.reader.readToken("/")) {
                var pattern = "";
                while (true) {
                    var chr = this.reader.readChar();
                    if (chr == "\\") {
                        var chr2 = this.reader.readChar();
                        pattern += chr2 == "/" ? "/" : "\\" + chr2;
                    }
                    else if (chr == "/")
                        break;
                    else
                        pattern += chr;
                }
                var modifiers = this.reader.readModifiers(new string[] { "g", "i" });
                return new RegexLiteral(pattern, modifiers.includes("i"), modifiers.includes("g"));
            }
            else if (this.reader.readToken("typeof")) {
                var expr = this.expressionParser.parse(this.expressionParser.prefixPrecedence);
                this.reader.expectToken("===");
                var check = this.reader.expectString();
                
                string tsType = null;
                if (check == "string")
                    tsType = "TsString";
                else if (check == "boolean")
                    tsType = "TsBoolean";
                else if (check == "object")
                    tsType = "Object";
                else if (check == "function")
                    // TODO: ???
                    tsType = "Function";
                else if (check == "undefined")
                    // TODO: ???
                    tsType = "Object";
                else
                    this.reader.fail("unexpected typeof comparison");
                
                return new InstanceOfExpression(expr, new UnresolvedType(tsType, new IType[0]));
            }
            else if (this.reader.peekRegex("\\([A-Za-z0-9_]+\\s*[:,]|\\(\\)") != null) {
                var params_ = this.parseLambdaParams();
                this.reader.expectToken("=>");
                var block = this.parseLambdaBlock();
                return new Lambda(params_, block);
            }
            else if (this.reader.readToken("await")) {
                var expression = this.parseExpression();
                return new AwaitExpression(expression);
            }
            
            var mapLiteral = this.expressionParser.parseMapLiteral();
            if (mapLiteral != null)
                return mapLiteral;
            
            var arrayLiteral = this.expressionParser.parseArrayLiteral();
            if (arrayLiteral != null)
                return arrayLiteral;
            
            return null;
        }
        
        public Block parseLambdaBlock()
        {
            var block = this.parseBlock();
            if (block != null)
                return block;
            
            var returnExpr = this.parseExpression();
            if (returnExpr is ParenthesizedExpression parExpr)
                returnExpr = parExpr.expression;
            return new Block(new ReturnStatement[] { new ReturnStatement(returnExpr) });
        }
        
        public TypeAndInit parseTypeAndInit()
        {
            var type = this.reader.readToken(":") ? this.parseType() : null;
            var init = this.reader.readToken("=") ? this.parseExpression() : null;
            
            if (type == null && init == null)
                this.reader.fail($"expected type declaration or initializer");
            
            return new TypeAndInit(type, init);
        }
        
        public Block expectBlockOrStatement()
        {
            var block = this.parseBlock();
            if (block != null)
                return block;
            
            var stmts = new List<Statement>();
            var stmt = this.expectStatement();
            if (stmt != null)
                stmts.push(stmt);
            return new Block(stmts.ToArray());
        }
        
        public Statement expectStatement()
        {
            Statement statement = null;
            
            var leadingTrivia = this.reader.readLeadingTrivia();
            var startPos = this.reader.offset;
            
            var requiresClosing = true;
            var varDeclMatches = this.reader.readRegex("(const|let|var)\\b");
            if (varDeclMatches != null) {
                var name = this.reader.expectIdentifier("expected variable name");
                var typeAndInit = this.parseTypeAndInit();
                statement = new VariableDeclaration(name, typeAndInit.type, typeAndInit.init);
            }
            else if (this.reader.readToken("delete"))
                statement = new UnsetStatement(this.parseExpression());
            else if (this.reader.readToken("if")) {
                requiresClosing = false;
                this.reader.expectToken("(");
                var condition = this.parseExpression();
                this.reader.expectToken(")");
                var then = this.expectBlockOrStatement();
                var else_ = this.reader.readToken("else") ? this.expectBlockOrStatement() : null;
                statement = new IfStatement(condition, then, else_);
            }
            else if (this.reader.readToken("while")) {
                requiresClosing = false;
                this.reader.expectToken("(");
                var condition = this.parseExpression();
                this.reader.expectToken(")");
                var body = this.expectBlockOrStatement();
                statement = new WhileStatement(condition, body);
            }
            else if (this.reader.readToken("do")) {
                requiresClosing = false;
                var body = this.expectBlockOrStatement();
                this.reader.expectToken("while");
                this.reader.expectToken("(");
                var condition = this.parseExpression();
                this.reader.expectToken(")");
                statement = new DoStatement(condition, body);
            }
            else if (this.reader.readToken("for")) {
                requiresClosing = false;
                this.reader.expectToken("(");
                var varDeclMod = this.reader.readAnyOf(new string[] { "const", "let", "var" });
                var itemVarName = varDeclMod == null ? null : this.reader.expectIdentifier();
                if (itemVarName != null && this.reader.readToken("of")) {
                    var items = this.parseExpression();
                    this.reader.expectToken(")");
                    var body = this.expectBlockOrStatement();
                    statement = new ForeachStatement(new ForeachVariable(itemVarName), items, body);
                }
                else {
                    ForVariable forVar = null;
                    if (itemVarName != null) {
                        var typeAndInit = this.parseTypeAndInit();
                        forVar = new ForVariable(itemVarName, typeAndInit.type, typeAndInit.init);
                    }
                    this.reader.expectToken(";");
                    var condition = this.parseExpression();
                    this.reader.expectToken(";");
                    var incrementor = this.parseExpression();
                    this.reader.expectToken(")");
                    var body = this.expectBlockOrStatement();
                    statement = new ForStatement(forVar, condition, incrementor, body);
                }
            }
            else if (this.reader.readToken("try")) {
                var block = this.expectBlock("try body is missing");
                
                CatchVariable catchVar = null;
                Block catchBody = null;
                if (this.reader.readToken("catch")) {
                    this.reader.expectToken("(");
                    catchVar = new CatchVariable(this.reader.expectIdentifier(), null);
                    this.reader.expectToken(")");
                    catchBody = this.expectBlock("catch body is missing");
                }
                
                var finallyBody = this.reader.readToken("finally") ? this.expectBlock() : null;
                return new TryStatement(block, catchVar, catchBody, finallyBody);
            }
            else if (this.reader.readToken("return")) {
                var expr = this.reader.peekToken(";") ? null : this.parseExpression();
                statement = new ReturnStatement(expr);
            }
            else if (this.reader.readToken("throw")) {
                var expr = this.parseExpression();
                statement = new ThrowStatement(expr);
            }
            else if (this.reader.readToken("break"))
                statement = new BreakStatement();
            else if (this.reader.readToken("continue"))
                statement = new ContinueStatement();
            else if (this.reader.readToken("debugger;"))
                return null;
            else {
                var expr = this.parseExpression();
                statement = new ExpressionStatement(expr);
                var isBinarySet = expr is BinaryExpression binExpr && new List<string> { "=", "+=", "-=" }.includes(binExpr.operator_);
                var isUnarySet = expr is UnaryExpression unaryExpr && new List<string> { "++", "--" }.includes(unaryExpr.operator_);
                if (!(expr is UnresolvedCallExpression || isBinarySet || isUnarySet || expr is AwaitExpression))
                    this.reader.fail("this expression is not allowed as statement");
            }
            
            if (statement == null)
                this.reader.fail("unknown statement");
            
            statement.leadingTrivia = leadingTrivia;
            this.nodeManager.addNode(statement, startPos);
            
            var statementLastLine = this.reader.wsLineCounter;
            if (!this.reader.readToken(";") && requiresClosing && this.reader.wsLineCounter == statementLastLine)
                this.reader.fail("statement is not closed", this.reader.wsOffset);
            
            return statement;
        }
        
        public Block parseBlock()
        {
            if (!this.reader.readToken("{"))
                return null;
            var startPos = this.reader.prevTokenOffset;
            
            var statements = new List<Statement>();
            if (!this.reader.readToken("}"))
                do {
                    var statement = this.expectStatement();
                    if (statement != null)
                        statements.push(statement);
                } while (!this.reader.readToken("}"));
            
            var block = new Block(statements.ToArray());
            this.nodeManager.addNode(block, startPos);
            return block;
        }
        
        public Block expectBlock(string errorMsg = null)
        {
            var block = this.parseBlock();
            if (block == null)
                this.reader.fail(errorMsg ?? "expected block here");
            return block;
        }
        
        public IType[] parseTypeArgs()
        {
            var typeArguments = new List<IType>();
            if (this.reader.readToken("<")) {
                do {
                    var generics = this.parseType();
                    typeArguments.push(generics);
                } while (this.reader.readToken(","));
                this.reader.expectToken(">");
            }
            return typeArguments.ToArray();
        }
        
        public string[] parseGenericsArgs()
        {
            var typeArguments = new List<string>();
            if (this.reader.readToken("<")) {
                do {
                    var generics = this.reader.expectIdentifier();
                    typeArguments.push(generics);
                } while (this.reader.readToken(","));
                this.reader.expectToken(">");
            }
            return typeArguments.ToArray();
        }
        
        public ExpressionStatement parseExprStmtFromString(string expression)
        {
            var expr = this.createExpressionParser(new Reader(expression)).parse();
            return new ExpressionStatement(expr);
        }
        
        public MethodSignature parseMethodSignature(bool isConstructor, bool declarationOnly)
        {
            var params_ = new List<MethodParameter>();
            var fields = new List<Field>();
            if (!this.reader.readToken(")")) {
                do {
                    var leadingTrivia = this.reader.readLeadingTrivia();
                    var paramStart = this.reader.offset;
                    var isPublic = this.reader.readToken("public");
                    if (isPublic && !isConstructor)
                        this.reader.fail("public modifier is only allowed in constructor definition");
                    
                    var paramName = this.reader.expectIdentifier();
                    this.context.push($"arg:{paramName}");
                    var typeAndInit = this.parseTypeAndInit();
                    var param = new MethodParameter(paramName, typeAndInit.type, typeAndInit.init, leadingTrivia);
                    params_.push(param);
                    
                    // init should be used as only the constructor's method parameter, but not again as a field initializer too
                    //   (otherwise it would called twice if cloned or cause AST error is just referenced from two separate places)
                    if (isPublic) {
                        var field = new Field(paramName, typeAndInit.type, null, Visibility.Public, false, param, param.leadingTrivia);
                        fields.push(field);
                        param.fieldDecl = field;
                    }
                    
                    this.nodeManager.addNode(param, paramStart);
                    this.context.pop();
                } while (this.reader.readToken(","));
                
                this.reader.expectToken(")");
            }
            
            IType returns = null;
            if (!isConstructor)
                // in case of constructor, "returns" won't be used
                returns = this.reader.readToken(":") ? this.parseType() : this.missingReturnTypeIsVoid ? VoidType.instance : null;
            
            Block body = null;
            Expression[] superCallArgs = null;
            if (declarationOnly)
                this.reader.expectToken(";");
            else {
                body = this.expectBlock("method body is missing");
                var firstStmt = body.statements.length() > 0 ? body.statements.get(0) : null;
                if (firstStmt is ExpressionStatement exprStat && exprStat.expression is UnresolvedCallExpression unrCallExpr && unrCallExpr.func is Identifier ident2 && ident2.text == "super") {
                    superCallArgs = unrCallExpr.args;
                    body.statements.shift();
                }
            }
            
            return new MethodSignature(params_.ToArray(), fields.ToArray(), body, returns, superCallArgs);
        }
        
        public string parseIdentifierOrString()
        {
            return this.reader.readString() ?? this.reader.expectIdentifier();
        }
        
        public Interface parseInterface(string leadingTrivia, bool isExported)
        {
            if (!this.reader.readToken("interface"))
                return null;
            var intfStart = this.reader.prevTokenOffset;
            
            var intfName = this.reader.expectIdentifier("expected identifier after 'interface' keyword");
            this.context.push($"I:{intfName}");
            
            var intfTypeArgs = this.parseGenericsArgs();
            
            var baseInterfaces = new List<IType>();
            if (this.reader.readToken("extends"))
                do
                    baseInterfaces.push(this.parseType()); while (this.reader.readToken(","));
            
            var methods = new List<Method>();
            var fields = new List<Field>();
            
            this.reader.expectToken("{");
            while (!this.reader.readToken("}")) {
                var memberLeadingTrivia = this.reader.readLeadingTrivia();
                
                var memberStart = this.reader.offset;
                var memberName = this.parseIdentifierOrString();
                if (this.reader.readToken(":")) {
                    this.context.push($"F:{memberName}");
                    
                    var fieldType = this.parseType();
                    this.reader.expectToken(";");
                    
                    var field = new Field(memberName, fieldType, null, Visibility.Public, false, null, memberLeadingTrivia);
                    fields.push(field);
                    
                    this.nodeManager.addNode(field, memberStart);
                    this.context.pop();
                }
                else {
                    this.context.push($"M:{memberName}");
                    var methodTypeArgs = this.parseGenericsArgs();
                    this.reader.expectToken("(");
                    // method
                       
                    var sig = this.parseMethodSignature(false, true);
                    
                    var method = new Method(memberName, methodTypeArgs, sig.params_, sig.body, Visibility.Public, false, sig.returns, false, memberLeadingTrivia);
                    methods.push(method);
                    this.nodeManager.addNode(method, memberStart);
                    this.context.pop();
                }
            }
            
            var intf = new Interface(intfName, intfTypeArgs, baseInterfaces.ToArray(), fields.ToArray(), methods.ToArray(), isExported, leadingTrivia);
            this.nodeManager.addNode(intf, intfStart);
            this.context.pop();
            return intf;
        }
        
        public UnresolvedType parseSpecifiedType()
        {
            var typeName = this.reader.readIdentifier();
            var typeArgs = this.parseTypeArgs();
            return new UnresolvedType(typeName, typeArgs);
        }
        
        public Class parseClass(string leadingTrivia, bool isExported, bool declarationOnly)
        {
            var clsModifiers = this.reader.readModifiers(new string[] { "abstract" });
            if (!this.reader.readToken("class"))
                return null;
            var clsStart = this.reader.prevTokenOffset;
            
            var clsName = this.reader.expectIdentifier("expected identifier after 'class' keyword");
            this.context.push($"C:{clsName}");
            
            var typeArgs = this.parseGenericsArgs();
            var baseClass = this.reader.readToken("extends") ? this.parseSpecifiedType() : null;
            
            var baseInterfaces = new List<IType>();
            if (this.reader.readToken("implements"))
                do
                    baseInterfaces.push(this.parseSpecifiedType()); while (this.reader.readToken(","));
            
            Constructor constructor = null;
            var fields = new List<Field>();
            var methods = new List<Method>();
            var properties = new List<Property>();
            
            this.reader.expectToken("{");
            while (!this.reader.readToken("}")) {
                var memberLeadingTrivia = this.reader.readLeadingTrivia();
                
                var memberStart = this.reader.offset;
                var modifiers = this.reader.readModifiers(new string[] { "static", "public", "protected", "private", "readonly", "async", "abstract" });
                var isStatic = modifiers.includes("static");
                var isAsync = modifiers.includes("async");
                var isAbstract = modifiers.includes("abstract");
                var visibility = modifiers.includes("private") ? Visibility.Private : modifiers.includes("protected") ? Visibility.Protected : Visibility.Public;
                
                var memberName = this.parseIdentifierOrString();
                var methodTypeArgs = this.parseGenericsArgs();
                if (this.reader.readToken("(")) {
                    // method
                    var isConstructor = memberName == "constructor";
                    
                    IMethodBase member;
                    var sig = this.parseMethodSignature(isConstructor, declarationOnly || isAbstract);
                    if (isConstructor) {
                        member = constructor = new Constructor(sig.params_, sig.body, sig.superCallArgs, memberLeadingTrivia);
                        foreach (var field in sig.fields)
                            fields.push(field);
                    }
                    else {
                        var method = new Method(memberName, methodTypeArgs, sig.params_, sig.body, visibility, isStatic, sig.returns, isAsync, memberLeadingTrivia);
                        methods.push(method);
                        member = method;
                    }
                    
                    this.nodeManager.addNode(member, memberStart);
                }
                else if (memberName == "get" || memberName == "set") {
                    // property
                    var propName = this.reader.expectIdentifier();
                    var prop = properties.find(x => x.name == propName);
                    IType propType = null;
                    Block getter = null;
                    Block setter = null;
                    
                    if (memberName == "get") {
                        // get propName(): propType { return ... }
                        this.context.push($"P[G]:{propName}");
                        this.reader.expectToken("()", "expected '()' after property getter name");
                        propType = this.reader.readToken(":") ? this.parseType() : null;
                        if (declarationOnly) {
                            if (propType == null)
                                this.reader.fail("Type is missing for property in declare class");
                            this.reader.expectToken(";");
                        }
                        else {
                            getter = this.expectBlock("property getter body is missing");
                            if (prop != null)
                                prop.getter = getter;
                        }
                    }
                    else if (memberName == "set") {
                        // set propName(value: propType) { ... }
                        this.context.push($"P[S]:{propName}");
                        this.reader.expectToken("(", "expected '(' after property setter name");
                        this.reader.expectIdentifier();
                        propType = this.reader.readToken(":") ? this.parseType() : null;
                        this.reader.expectToken(")");
                        if (declarationOnly) {
                            if (propType == null)
                                this.reader.fail("Type is missing for property in declare class");
                            this.reader.expectToken(";");
                        }
                        else {
                            setter = this.expectBlock("property setter body is missing");
                            if (prop != null)
                                prop.setter = setter;
                        }
                    }
                    
                    if (prop == null) {
                        prop = new Property(propName, propType, getter, setter, visibility, isStatic, memberLeadingTrivia);
                        properties.push(prop);
                        this.nodeManager.addNode(prop, memberStart);
                    }
                    
                    this.context.pop();
                }
                else {
                    this.context.push($"F:{memberName}");
                    
                    var typeAndInit = this.parseTypeAndInit();
                    this.reader.expectToken(";");
                    
                    var field = new Field(memberName, typeAndInit.type, typeAndInit.init, visibility, isStatic, null, memberLeadingTrivia);
                    fields.push(field);
                    
                    this.nodeManager.addNode(field, memberStart);
                    this.context.pop();
                }
            }
            
            var cls = new Class(clsName, typeArgs, baseClass, baseInterfaces.ToArray(), fields.ToArray(), properties.ToArray(), constructor, methods.ToArray(), isExported, leadingTrivia);
            this.nodeManager.addNode(cls, clsStart);
            this.context.pop();
            return cls;
        }
        
        public Enum_ parseEnum(string leadingTrivia, bool isExported)
        {
            if (!this.reader.readToken("enum"))
                return null;
            var enumStart = this.reader.prevTokenOffset;
            
            var name = this.reader.expectIdentifier("expected identifier after 'enum' keyword");
            this.context.push($"E:{name}");
            
            var members = new List<EnumMember>();
            
            this.reader.expectToken("{");
            if (!this.reader.readToken("}")) {
                do {
                    if (this.reader.peekToken("}"))
                        break;
                    // eg. "enum { A, B, }" (but multiline)
                    
                    var enumMember = new EnumMember(this.reader.expectIdentifier());
                    members.push(enumMember);
                    this.nodeManager.addNode(enumMember, this.reader.prevTokenOffset);
                    
                    // TODO: generated code compatibility
                    this.reader.readToken($"= \"{enumMember.name}\"");
                } while (this.reader.readToken(","));
                this.reader.expectToken("}");
            }
            
            var enumObj = new Enum_(name, members.ToArray(), isExported, leadingTrivia);
            this.nodeManager.addNode(enumObj, enumStart);
            this.context.pop();
            return enumObj;
        }
        
        public static string calculateRelativePath(string currFile, string relPath)
        {
            if (!relPath.startsWith("."))
                throw new Error($"relPath must start with '.', but got '{relPath}'");
            
            var curr = currFile.split(new RegExp("/")).ToList();
            curr.pop();
            // filename does not matter
            foreach (var part in relPath.split(new RegExp("/"))) {
                if (part == "")
                    throw new Error($"relPath should not contain multiple '/' next to each other (relPath='{relPath}')");
                if (part == ".")
                    // "./" == stay in current directory
                    continue;
                else if (part == "..") {
                    // "../" == parent directory
                    if (curr.length() == 0)
                        throw new Error($"relPath goes out of root (curr='{currFile}', relPath='{relPath}')");
                    curr.pop();
                }
                else
                    curr.push(part);
            }
            return curr.join("/");
        }
        
        public static ExportScopeRef calculateImportScope(ExportScopeRef currScope, string importFile)
        {
            if (importFile.startsWith("."))
                // relative
                return new ExportScopeRef(currScope.packageName, TypeScriptParser2.calculateRelativePath(currScope.scopeName, importFile));
            else {
                var path = importFile.split(new RegExp("/")).ToList();
                var pkgName = path.shift();
                return new ExportScopeRef(pkgName, path.length() == 0 ? Package.INDEX : path.join("/"));
            }
        }
        
        public string readIdentifier()
        {
            var rawId = this.reader.readIdentifier();
            return rawId.replace(new RegExp("_+$"), "");
        }
        
        public Import[] parseImport(string leadingTrivia)
        {
            if (!this.reader.readToken("import"))
                return null;
            var importStart = this.reader.prevTokenOffset;
            
            string importAllAlias = null;
            var importParts = new List<UnresolvedImport>();
            
            if (this.reader.readToken("*")) {
                this.reader.expectToken("as");
                importAllAlias = this.reader.expectIdentifier();
            }
            else {
                this.reader.expectToken("{");
                do {
                    if (this.reader.peekToken("}"))
                        break;
                    
                    var imp = this.reader.expectIdentifier();
                    if (this.reader.readToken("as"))
                        this.reader.fail("This is not yet supported");
                    importParts.push(new UnresolvedImport(imp));
                    this.nodeManager.addNode(imp, this.reader.prevTokenOffset);
                } while (this.reader.readToken(","));
                this.reader.expectToken("}");
            }
            
            this.reader.expectToken("from");
            var moduleName = this.reader.expectString();
            this.reader.expectToken(";");
            
            var importScope = this.exportScope != null ? TypeScriptParser2.calculateImportScope(this.exportScope, moduleName) : null;
            
            var imports = new List<Import>();
            if (importParts.length() > 0)
                imports.push(new Import(importScope, false, importParts.ToArray(), null, leadingTrivia));
            
            if (importAllAlias != null)
                imports.push(new Import(importScope, true, null, importAllAlias, leadingTrivia));
            //this.nodeManager.addNode(imports, importStart);
            return imports.ToArray();
        }
        
        public SourceFile parseSourceFile()
        {
            var imports = new List<Import>();
            var enums = new List<Enum_>();
            var intfs = new List<Interface>();
            var classes = new List<Class>();
            var funcs = new List<GlobalFunction>();
            while (true) {
                var leadingTrivia = this.reader.readLeadingTrivia();
                if (this.reader.eof)
                    break;
                
                var imps = this.parseImport(leadingTrivia);
                if (imps != null) {
                    foreach (var imp in imps)
                        imports.push(imp);
                    continue;
                }
                
                var modifiers = this.reader.readModifiers(new string[] { "export", "declare" });
                var isExported = modifiers.includes("export");
                var isDeclaration = modifiers.includes("declare");
                
                var cls = this.parseClass(leadingTrivia, isExported, isDeclaration);
                if (cls != null) {
                    classes.push(cls);
                    continue;
                }
                
                var enumObj = this.parseEnum(leadingTrivia, isExported);
                if (enumObj != null) {
                    enums.push(enumObj);
                    continue;
                }
                
                var intf = this.parseInterface(leadingTrivia, isExported);
                if (intf != null) {
                    intfs.push(intf);
                    continue;
                }
                
                if (this.reader.readToken("function")) {
                    var funcName = this.readIdentifier();
                    this.reader.expectToken("(");
                    var sig = this.parseMethodSignature(false, isDeclaration);
                    funcs.push(new GlobalFunction(funcName, sig.params_, sig.body, sig.returns, isExported, leadingTrivia));
                    continue;
                }
                
                break;
            }
            
            this.reader.skipWhitespace();
            
            var stmts = new List<Statement>();
            while (true) {
                var leadingTrivia = this.reader.readLeadingTrivia();
                if (this.reader.eof)
                    break;
                
                var stmt = this.expectStatement();
                if (stmt == null)
                    continue;
                
                stmt.leadingTrivia = leadingTrivia;
                stmts.push(stmt);
            }
            
            return new SourceFile(imports.ToArray(), intfs.ToArray(), classes.ToArray(), enums.ToArray(), funcs.ToArray(), new Block(stmts.ToArray()), this.path, this.exportScope);
        }
        
        public SourceFile parse()
        {
            return this.parseSourceFile();
        }
        
        public static SourceFile parseFile(string source, SourcePath path = null)
        {
            return new TypeScriptParser2(source, path).parseSourceFile();
        }
    }
}