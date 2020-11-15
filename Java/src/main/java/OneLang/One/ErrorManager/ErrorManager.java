package OneLang.One.ErrorManager;

import OneLang.One.Ast.Types.SourceFile;
import OneLang.One.Ast.Types.IInterface;
import OneLang.One.Ast.Types.IMethodBase;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Types.IAstNode;
import OneLang.One.Ast.Types.Field;
import OneLang.One.Ast.Types.Property;
import OneLang.One.Ast.Types.Constructor;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.AstTransformer.AstTransformer;
import OneLang.One.Ast.Statements.Statement;
import OneLang.Utils.TSOverviewGenerator.TSOverviewGenerator;
import OneLang.One.Ast.Expressions.Expression;

import OneLang.One.AstTransformer.AstTransformer;
import OneLang.One.Ast.Types.IAstNode;
import java.util.List;
import OneLang.One.ErrorManager.CompilationError;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Types.Field;
import OneLang.One.Ast.Types.Property;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Types.Constructor;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.Types.Lambda;
import java.util.ArrayList;
import OneStd.console;

public class ErrorManager {
    public AstTransformer transformer;
    public IAstNode currentNode;
    public List<CompilationError> errors;
    public String lastContextInfo;
    
    public String getLocation() {
        var t = this.transformer;
        
        var par = this.currentNode;
        while (par instanceof Expression)
            par = ((Expression)par).parentNode;
        
        String location = null;
        if (par instanceof Field)
            location = ((Field)par).parentInterface.getParentFile().sourcePath.path + " -> " + ((Field)par).parentInterface.getName() + "::" + ((Field)par).getName() + " (field)";
        else if (par instanceof Property)
            location = ((Property)par).parentClass.getParentFile().sourcePath.path + " -> " + ((Property)par).parentClass.getName() + "::" + ((Property)par).getName() + " (property)";
        else if (par instanceof Method)
            location = ((Method)par).parentInterface.getParentFile().sourcePath.path + " -> " + ((Method)par).parentInterface.getName() + "::" + ((Method)par).name + " (method)";
        else if (par instanceof Constructor)
            location = ((Constructor)par).parentClass.getParentFile().sourcePath.path + " -> " + ((Constructor)par).parentClass.getName() + "::constructor";
        else if (par == null) { }
        else if (par instanceof Statement) { }
        else { }
        
        if (location == null && t != null && t.currentFile != null) {
            location = t.currentFile.sourcePath.path;
            if (t.currentInterface != null) {
                location += " -> " + t.currentInterface.getName();
                if (t.currentMethod instanceof Method)
                    location += "::" + ((Method)t.currentMethod).name;
                else if (t.currentMethod instanceof Constructor)
                    location += "::constructor";
                else if (t.currentMethod instanceof Lambda)
                    location += "::<lambda>";
                else if (t.currentMethod == null) { }
                else { }
            }
        }
        
        return location;
    }
    
    public String getCurrentNodeRepr() {
        return TSOverviewGenerator.preview.nodeRepr(this.currentNode);
    }
    
    public String getCurrentStatementRepr() {
        return this.transformer.currentStatement == null ? "<null>" : TSOverviewGenerator.preview.stmt(this.transformer.currentStatement);
    }
    
    public ErrorManager()
    {
        this.transformer = null;
        this.currentNode = null;
        this.errors = new ArrayList<CompilationError>();
    }
    
    public void resetContext(AstTransformer transformer) {
        this.transformer = transformer;
    }
    
    public void log(LogType type, String msg) {
        var t = this.transformer;
        var text = (t != null ? "[" + t.getName() + "] " : "") + msg;
        
        if (this.currentNode != null)
            text += "\n  Node: " + this.getCurrentNodeRepr();
        
        var location = this.getLocation();
        if (location != null)
            text += "\n  Location: " + location;
        
        if (t != null && t.currentStatement != null)
            text += "\n  Statement: " + this.getCurrentStatementRepr();
        
        if (this.lastContextInfo != null)
            text += "\n  Context: " + this.lastContextInfo;
        
        if (type == LogType.Info)
            console.log(text);
        else if (type == LogType.Warning)
            console.error("[WARNING] " + text + "\n");
        else if (type == LogType.Error)
            console.error(text + "\n");
        else { }
        
        if (type == LogType.Error || type == LogType.Warning)
            this.errors.add(new CompilationError(msg, type == LogType.Warning, t != null ? t.getName() : null, this.currentNode));
    }
    
    public void info(String msg) {
        this.log(LogType.Info, msg);
    }
    
    public void warn(String msg) {
        this.log(LogType.Warning, msg);
    }
    
    public void throw_(String msg) {
        this.log(LogType.Error, msg);
    }
}