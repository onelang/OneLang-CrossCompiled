using One.Ast;
using One;

namespace One.Transforms
{
    public class ResolveUnresolvedTypes : AstTransformer
    {
        public ResolveUnresolvedTypes(): base("ResolveUnresolvedTypes")
        {
            
        }
        
        protected override IType visitType(IType type)
        {
            base.visitType(type);
            if (type is UnresolvedType unrType) {
                if (this.currentInterface != null && this.currentInterface.typeArguments.includes(unrType.typeName))
                    return new GenericsType(unrType.typeName);
                
                var symbol = this.currentFile.availableSymbols.get(unrType.typeName);
                if (symbol == null) {
                    this.errorMan.throw_($"Unresolved type '{unrType.typeName}' was not found in available symbols");
                    return unrType;
                }
                
                if (symbol is Class class_)
                    return new ClassType(class_, unrType.typeArguments);
                else if (symbol is Interface int_)
                    return new InterfaceType(int_, unrType.typeArguments);
                else if (symbol is Enum_ enum_)
                    return new EnumType(enum_);
                else {
                    this.errorMan.throw_($"Unknown symbol type: {symbol}");
                    return unrType;
                }
            }
            else
                return type;
        }
        
        protected override Expression visitExpression(Expression expr)
        {
            if (expr is UnresolvedNewExpression unrNewExpr) {
                var clsType = this.visitType(unrNewExpr.cls);
                if (clsType is ClassType classType) {
                    var newExpr = new NewExpression(classType, unrNewExpr.args);
                    newExpr.parentNode = unrNewExpr.parentNode;
                    base.visitExpression(newExpr);
                    return newExpr;
                }
                else {
                    this.errorMan.throw_($"Excepted ClassType, but got {clsType}");
                    return unrNewExpr;
                }
            }
            else
                return base.visitExpression(expr);
        }
    }
}