package OneLang.One.Transforms.InferTypesPlugins.LambdaResolver;

import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.Ast.AstTypes.LambdaType;
import OneLang.One.Ast.AstTypes.TypeHelper;

import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import OneLang.One.Ast.AstTypes.LambdaType;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.Ast.Expressions.Expression;

public class LambdaResolver extends InferTypesPlugin {
    public LambdaResolver()
    {
        super("LambdaResolver");
        
    }
    
    protected void setupLambdaParameterTypes(Lambda lambda) {
        if (lambda.expectedType == null)
            return;
        
        if (lambda.expectedType instanceof LambdaType) {
            var declParams = ((LambdaType)lambda.expectedType).parameters;
            if (declParams.length != lambda.getParameters().length)
                this.errorMan.throw_("Expected " + lambda.getParameters().length + " parameters for lambda, but got " + declParams.length + "!");
            else
                for (Integer i = 0; i < declParams.length; i++) {
                    if (lambda.getParameters()[i].getType() == null)
                        lambda.getParameters()[i].setType(declParams[i].getType());
                    else if (!TypeHelper.isAssignableTo(lambda.getParameters()[i].getType(), declParams[i].getType()))
                        this.errorMan.throw_("Parameter type " + lambda.getParameters()[i].getType().repr() + " cannot be assigned to " + declParams[i].getType().repr() + ".");
                }
        }
        else
            this.errorMan.throw_("Expected LambdaType as Lambda's type!");
    }
    
    protected void visitLambda(Lambda lambda) {
        this.setupLambdaParameterTypes(lambda);
    }
    
    public Boolean canTransform(Expression expr) {
        return expr instanceof Lambda;
    }
    
    public Expression transform(Expression expr) {
        this.visitLambda(((Lambda)expr));
        // does not transform actually
        return expr;
    }
}