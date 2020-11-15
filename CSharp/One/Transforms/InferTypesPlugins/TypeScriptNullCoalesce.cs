using One.Ast;
using One.Transforms.InferTypesPlugins.Helpers;

namespace One.Transforms.InferTypesPlugins
{
    public class TypeScriptNullCoalesce : InferTypesPlugin {
        public TypeScriptNullCoalesce(): base("TypeScriptNullCoalesce")
        {
            
        }
        
        public override bool canTransform(Expression expr)
        {
            return expr is BinaryExpression binExpr && binExpr.operator_ == "||";
        }
        
        public override Expression transform(Expression expr)
        {
            if (expr is BinaryExpression binExpr2 && binExpr2.operator_ == "||") {
                var litTypes = this.main.currentFile.literalTypes;
                
                binExpr2.left = this.main.runPluginsOn(binExpr2.left);
                var leftType = binExpr2.left.getType();
                
                if (binExpr2.right is ArrayLiteral arrayLit && arrayLit.items.length() == 0) {
                    if (leftType is ClassType classType && classType.decl == litTypes.array.decl) {
                        arrayLit.setActualType(classType);
                        return new NullCoalesceExpression(binExpr2.left, arrayLit);
                    }
                }
                
                if (binExpr2.right is MapLiteral mapLit && mapLit.items.length() == 0) {
                    if (leftType is ClassType classType2 && classType2.decl == litTypes.map.decl) {
                        mapLit.setActualType(classType2);
                        return new NullCoalesceExpression(binExpr2.left, mapLit);
                    }
                }
                
                binExpr2.right = this.main.runPluginsOn(binExpr2.right);
                var rightType = binExpr2.right.getType();
                
                if (binExpr2.right is NullLiteral)
                    // something-which-can-be-undefined || null
                    return binExpr2.left;
                else if (TypeHelper.isAssignableTo(rightType, leftType) && !TypeHelper.equals(rightType, this.main.currentFile.literalTypes.boolean))
                    return new NullCoalesceExpression(binExpr2.left, binExpr2.right);
            }
            return expr;
        }
    }
}