using One.Ast;
using One.Transforms.InferTypesPlugins.Helpers;

namespace One.Transforms.InferTypesPlugins
{
    public class NullabilityCheckWithNot : InferTypesPlugin {
        public NullabilityCheckWithNot(): base("NullabilityCheckWithNot")
        {
            
        }
        
        public override bool canTransform(Expression expr)
        {
            return expr is UnaryExpression unaryExpr ? unaryExpr.operator_ == "!" : false;
        }
        
        public override Expression transform(Expression expr)
        {
            var unaryExpr = ((UnaryExpression)expr);
            if (unaryExpr.operator_ == "!") {
                this.main.processExpression(expr);
                var type = unaryExpr.operand.actualType;
                var litTypes = this.main.currentFile.literalTypes;
                if (type is ClassType classType && classType.decl != litTypes.boolean.decl && classType.decl != litTypes.numeric.decl)
                    return new BinaryExpression(unaryExpr.operand, "==", new NullLiteral());
            }
            
            return expr;
        }
    }
}