package OneLang.One.Transforms.ResolveIdentifiers;

import OneLang.One.AstTransformer.AstTransformer;
import OneLang.One.Ast.Types.SourceFile;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.Enum;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.Ast.Types.GlobalFunction;
import OneLang.One.Ast.Types.IMethodBase;
import OneLang.One.Ast.Types.Constructor;
import OneLang.One.Ast.Types.Interface;
import OneLang.One.ErrorManager.ErrorManager;
import OneLang.One.Ast.Expressions.Identifier;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.References.IReferencable;
import OneLang.One.Ast.References.Reference;
import OneLang.One.Ast.References.StaticThisReference;
import OneLang.One.Ast.References.ThisReference;
import OneLang.One.Ast.References.SuperReference;
import OneLang.One.Ast.Statements.VariableDeclaration;
import OneLang.One.Ast.Statements.ForStatement;
import OneLang.One.Ast.Statements.ForeachStatement;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.Statements.IfStatement;
import OneLang.One.Ast.Statements.TryStatement;
import OneLang.One.Ast.Statements.Block;
import OneLang.One.Ast.AstTypes.ClassType;

import OneLang.One.ErrorManager.ErrorManager;
import java.util.List;
import OneLang.One.Ast.References.IReferencable;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public class SymbolLookup {
    public ErrorManager errorMan;
    public List<List<String>> levelStack;
    public List<String> levelNames;
    public List<String> currLevel;
    public Map<String, IReferencable> symbols;
    
    public SymbolLookup()
    {
        this.errorMan = new ErrorManager();
        this.levelStack = new ArrayList<List<String>>();
        this.levelNames = new ArrayList<String>();
        this.symbols = new LinkedHashMap<String, IReferencable>();
    }
    
    public void throw_(String msg) {
        this.errorMan.throw_(msg + " (context: " + this.levelNames.stream().collect(Collectors.joining(" > ")) + ")");
    }
    
    public void pushContext(String name) {
        this.levelStack.add(this.currLevel);
        this.levelNames.add(name);
        this.currLevel = new ArrayList<String>();
    }
    
    public void addSymbol(String name, IReferencable ref) {
        if (this.symbols.containsKey(name))
            this.throw_("Symbol shadowing: " + name);
        this.symbols.put(name, ref);
        this.currLevel.add(name);
    }
    
    public void popContext() {
        for (var name : this.currLevel)
            this.symbols.remove(name);
        this.levelNames.remove(this.levelNames.size() - 1);
        this.currLevel = this.levelStack.size() > 0 ? this.levelStack.remove(this.levelStack.size() - 1) : null;
    }
    
    public IReferencable getSymbol(String name) {
        return this.symbols.get(name);
    }
}