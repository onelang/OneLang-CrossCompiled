package OneLang.One.Transforms.InferTypesPlugins.InferReturnType;

import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import OneLang.One.Ast.Types.Property;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Types.IMethodBase;
import OneLang.One.Ast.AstTypes.VoidType;
import OneLang.One.Ast.AstTypes.AnyType;
import OneLang.One.Ast.AstTypes.LambdaType;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.TypeHelper;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.Statements.ReturnStatement;
import OneLang.One.Ast.Statements.ThrowStatement;
import OneLang.One.ErrorManager.ErrorManager;
import OneLang.One.Ast.Expressions.NullLiteral;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Interfaces.IType;

import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import java.util.List;
import OneLang.One.Transforms.InferTypesPlugins.InferReturnType.ReturnTypeInferer;
import java.util.ArrayList;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.Statements.ReturnStatement;
import OneLang.One.Ast.Statements.ThrowStatement;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.AstTypes.LambdaType;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Types.IMethodBase;
import OneLang.One.Ast.Types.Property;

public class InferReturnType extends InferTypesPlugin {
    public List<ReturnTypeInferer> returnTypeInfer;
    
    public ReturnTypeInferer getCurrent() {
        return this.returnTypeInfer.get(this.returnTypeInfer.size() - 1);
    }
    
    public InferReturnType()
    {
        super("InferReturnType");
        this.returnTypeInfer = new ArrayList<ReturnTypeInferer>();
    }
    
    public void start() {
        this.returnTypeInfer.add(new ReturnTypeInferer(this.errorMan));
    }
    
    public IType finish(IType declaredType, String errorContext, ClassType asyncType) {
        return this.returnTypeInfer.remove(this.returnTypeInfer.size() - 1).finish(declaredType, errorContext, asyncType);
    }
    
    public Boolean handleStatement(Statement stmt) {
        if (this.returnTypeInfer.size() == 0)
            return false;
        if (stmt instanceof ReturnStatement && ((ReturnStatement)stmt).expression != null) {
            this.main.processStatement(((ReturnStatement)stmt));
            this.getCurrent().addReturn(((ReturnStatement)stmt).expression);
            return true;
        }
        else if (stmt instanceof ThrowStatement) {
            this.getCurrent().throws_ = true;
            return false;
        }
        else
            return false;
    }
    
    public Boolean handleLambda(Lambda lambda) {
        this.start();
        this.main.processLambda(lambda);
        lambda.returns = this.finish(lambda.returns, "Lambda", null);
        lambda.setActualType(new LambdaType(lambda.getParameters(), lambda.returns), false, true);
        return true;
    }
    
    public Boolean handleMethod(IMethodBase method) {
        if (method instanceof Method && ((Method)method).getBody() != null) {
            this.start();
            this.main.processMethodBase(((Method)method));
            ((Method)method).returns = this.finish(((Method)method).returns, "Method \"" + ((Method)method).name + "\"", ((Method)method).async ? this.main.currentFile.literalTypes.promise : null);
            return true;
        }
        else
            return false;
    }
    
    public Boolean handleProperty(Property prop) {
        this.main.processVariable(prop);
        
        if (prop.getter != null) {
            this.start();
            this.main.processBlock(prop.getter);
            prop.setType(this.finish(prop.getType(), "Property \"" + prop.getName() + "\" getter", null));
        }
        
        if (prop.setter != null) {
            this.start();
            this.main.processBlock(prop.setter);
            this.finish(VoidType.instance, "Property \"" + prop.getName() + "\" setter", null);
        }
        
        return true;
    }
}