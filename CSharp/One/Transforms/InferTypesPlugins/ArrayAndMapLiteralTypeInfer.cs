using One.Ast;
using One.Transforms.InferTypesPlugins.Helpers;
using System.Collections.Generic;

namespace One.Transforms.InferTypesPlugins
{
    public class ArrayAndMapLiteralTypeInfer : InferTypesPlugin {
        public ArrayAndMapLiteralTypeInfer(): base("ArrayAndMapLiteralTypeInfer")
        {
            
        }
        
        protected IType inferArrayOrMapItemType(Expression[] items, IType expectedType, bool isMap)
        {
            var itemTypes = new List<IType>();
            foreach (var item in items) {
                if (!itemTypes.some(t => TypeHelper.equals(t, item.getType())))
                    itemTypes.push(item.getType());
            }
            
            var literalType = isMap ? this.main.currentFile.literalTypes.map : this.main.currentFile.literalTypes.array;
            
            IType itemType = null;
            if (itemTypes.length() == 0) {
                if (expectedType == null) {
                    this.errorMan.warn($"Could not determine the type of an empty {(isMap ? "MapLiteral" : "ArrayLiteral")}, using AnyType instead");
                    itemType = AnyType.instance;
                }
                else if (expectedType is ClassType classType && classType.decl == literalType.decl)
                    itemType = classType.typeArguments.get(0);
                else
                    itemType = AnyType.instance;
            }
            else if (itemTypes.length() == 1)
                itemType = itemTypes.get(0);
            else if (!(expectedType is AnyType)) {
                this.errorMan.warn($"Could not determine the type of {(isMap ? "a MapLiteral" : "an ArrayLiteral")}! Multiple types were found: {itemTypes.map(x => x.repr()).join(", ")}, using AnyType instead");
                itemType = AnyType.instance;
            }
            return itemType;
        }
        
        public override bool canDetectType(Expression expr)
        {
            return expr is ArrayLiteral arrayLit || expr is MapLiteral;
        }
        
        public override bool detectType(Expression expr)
        {
            // make this work: `<{ [name: string]: SomeObject }> {}`
            if (expr.parentNode is CastExpression castExpr)
                expr.setExpectedType(castExpr.newType);
            else if (expr.parentNode is BinaryExpression binExpr && binExpr.operator_ == "=" && binExpr.right == expr)
                expr.setExpectedType(binExpr.left.actualType);
            else if (expr.parentNode is ConditionalExpression condExpr && (condExpr.whenTrue == expr || condExpr.whenFalse == expr))
                expr.setExpectedType(condExpr.whenTrue == expr ? condExpr.whenFalse.actualType : condExpr.whenTrue.actualType);
            
            if (expr is ArrayLiteral arrayLit2) {
                var itemType = this.inferArrayOrMapItemType(arrayLit2.items, arrayLit2.expectedType, false);
                arrayLit2.setActualType(new ClassType(this.main.currentFile.literalTypes.array.decl, new IType[] { itemType }));
            }
            else if (expr is MapLiteral mapLit) {
                var itemType = this.inferArrayOrMapItemType(mapLit.items.map(x => x.value), mapLit.expectedType, true);
                mapLit.setActualType(new ClassType(this.main.currentFile.literalTypes.map.decl, new IType[] { itemType }));
            }
            
            return true;
        }
    }
}