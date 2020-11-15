using One.Ast;
using One.Transforms.InferTypesPlugins.Helpers;
using One.Transforms.InferTypesPlugins;
using System.Collections.Generic;

namespace One.Transforms.InferTypesPlugins
{
    public class ResolveElementAccess : InferTypesPlugin {
        public ResolveElementAccess(): base("ResolveElementAccess")
        {
            
        }
        
        public override bool canTransform(Expression expr)
        {
            var isSet = expr is BinaryExpression binExpr && binExpr.left is ElementAccessExpression && new List<string> { "=" }.includes(binExpr.operator_);
            return expr is ElementAccessExpression || isSet;
        }
        
        public bool isMapOrArrayType(IType type)
        {
            return TypeHelper.isAssignableTo(type, this.main.currentFile.literalTypes.map) || this.main.currentFile.arrayTypes.some(x => TypeHelper.isAssignableTo(type, x));
        }
        
        public override Expression transform(Expression expr)
        {
            // TODO: convert ElementAccess to ElementGet and ElementSet expressions
            if (expr is BinaryExpression binExpr2 && binExpr2.left is ElementAccessExpression elemAccExpr) {
                elemAccExpr.object_ = this.main.runPluginsOn(elemAccExpr.object_);
                if (this.isMapOrArrayType(elemAccExpr.object_.getType()))
                    //const right = expr.operator === "=" ? expr.right : new BinaryExpression(<Expression>expr.left.clone(), expr.operator === "+=" ? "+" : "-", expr.right);
                    return new UnresolvedMethodCallExpression(elemAccExpr.object_, "set", new IType[0], new Expression[] { elemAccExpr.elementExpr, binExpr2.right });
            }
            else if (expr is ElementAccessExpression elemAccExpr2) {
                elemAccExpr2.object_ = this.main.runPluginsOn(elemAccExpr2.object_);
                if (this.isMapOrArrayType(elemAccExpr2.object_.getType()))
                    return new UnresolvedMethodCallExpression(elemAccExpr2.object_, "get", new IType[0], new Expression[] { elemAccExpr2.elementExpr });
                else if (elemAccExpr2.elementExpr is StringLiteral strLit)
                    return new PropertyAccessExpression(elemAccExpr2.object_, strLit.stringValue);
            }
            return expr;
        }
    }
}