package OneLang.One.Transforms.FillParent;

import OneLang.One.Ast.Types.SourceFile;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Types.IInterface;
import OneLang.One.Ast.Types.Enum;
import OneLang.One.Ast.Types.Interface;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.Field;
import OneLang.One.Ast.Types.Property;
import OneLang.One.Ast.Types.IAstNode;
import OneLang.One.Ast.Types.IMethodBase;
import OneLang.One.Ast.Types.Constructor;
import OneLang.One.Ast.Types.GlobalFunction;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.AstTransformer.AstTransformer;

import OneLang.One.AstTransformer.AstTransformer;
import java.util.List;
import OneLang.One.Ast.Types.IAstNode;
import java.util.ArrayList;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.Types.Enum;
import OneLang.One.Ast.Types.Interface;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.GlobalFunction;
import OneLang.One.Ast.Types.Field;
import OneLang.One.Ast.Types.Property;
import OneLang.One.Ast.Types.Constructor;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.Ast.Types.IMethodBase;
import OneLang.One.Ast.Types.SourceFile;

public class FillParent extends AstTransformer {
    public List<IAstNode> parentNodeStack;
    
    public FillParent()
    {
        super("FillParent");
        this.parentNodeStack = new ArrayList<IAstNode>();
    }
    
    protected Expression visitExpression(Expression expr) {
        if (this.parentNodeStack.size() == 0) { }
        expr.parentNode = this.parentNodeStack.get(this.parentNodeStack.size() - 1);
        this.parentNodeStack.add(expr);
        super.visitExpression(expr);
        this.parentNodeStack.remove(this.parentNodeStack.size() - 1);
        return expr;
    }
    
    protected Statement visitStatement(Statement stmt) {
        this.parentNodeStack.add(stmt);
        super.visitStatement(stmt);
        this.parentNodeStack.remove(this.parentNodeStack.size() - 1);
        return stmt;
    }
    
    protected void visitEnum(Enum enum_) {
        enum_.setParentFile(this.currentFile);
        super.visitEnum(enum_);
        for (var value : enum_.values)
            value.parentEnum = enum_;
    }
    
    protected void visitInterface(Interface intf) {
        intf.setParentFile(this.currentFile);
        super.visitInterface(intf);
    }
    
    protected void visitClass(Class cls) {
        cls.setParentFile(this.currentFile);
        super.visitClass(cls);
    }
    
    protected void visitGlobalFunction(GlobalFunction func) {
        func.setParentFile(this.currentFile);
        super.visitGlobalFunction(func);
    }
    
    protected void visitField(Field field) {
        field.parentInterface = this.currentInterface;
        
        this.parentNodeStack.add(field);
        super.visitField(field);
        this.parentNodeStack.remove(this.parentNodeStack.size() - 1);
    }
    
    protected void visitProperty(Property prop) {
        prop.parentClass = ((Class)this.currentInterface);
        
        this.parentNodeStack.add(prop);
        super.visitProperty(prop);
        this.parentNodeStack.remove(this.parentNodeStack.size() - 1);
    }
    
    protected void visitMethodBase(IMethodBase method) {
        if (method instanceof Constructor)
            ((Constructor)method).parentClass = ((Class)this.currentInterface);
        else if (method instanceof Method)
            ((Method)method).parentInterface = this.currentInterface;
        else if (method instanceof GlobalFunction) { }
        else if (method instanceof Lambda) { }
        else { }
        
        for (var param : method.getParameters())
            param.parentMethod = method;
        
        this.parentNodeStack.add(method);
        super.visitMethodBase(method);
        this.parentNodeStack.remove(this.parentNodeStack.size() - 1);
    }
    
    public void visitFile(SourceFile file) {
        for (var imp : file.imports)
            imp.setParentFile(file);
        
        super.visitFile(file);
    }
}