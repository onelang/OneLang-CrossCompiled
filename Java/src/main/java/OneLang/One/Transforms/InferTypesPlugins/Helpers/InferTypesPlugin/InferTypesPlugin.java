package OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin;

import OneLang.One.ErrorManager.ErrorManager;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Transforms.InferTypes.InferTypes;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.Types.Property;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Types.IMethodBase;

import OneLang.One.Transforms.InferTypes.InferTypes;
import OneLang.One.ErrorManager.ErrorManager;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Types.Property;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.Ast.Types.IMethodBase;
import OneLang.One.Ast.Statements.Statement;

public class InferTypesPlugin {
    public InferTypes main;
    public ErrorManager errorMan;
    public String name;
    
    public InferTypesPlugin(String name)
    {
        this.name = name;
        this.errorMan = null;
    }
    
    public Boolean canTransform(Expression expr) {
        return false;
    }
    
    public Boolean canDetectType(Expression expr) {
        return false;
    }
    
    public Expression transform(Expression expr) {
        return expr;
    }
    
    public Boolean detectType(Expression expr) {
        return false;
    }
    
    public Boolean handleProperty(Property prop) {
        return false;
    }
    
    public Boolean handleLambda(Lambda lambda) {
        return false;
    }
    
    public Boolean handleMethod(IMethodBase method) {
        return false;
    }
    
    public Boolean handleStatement(Statement stmt) {
        return false;
    }
}