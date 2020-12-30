using One.Ast;
using One;

namespace One.Transforms
{
    public class ResolveGenericTypeIdentifiers : AstTransformer
    {
        public ResolveGenericTypeIdentifiers(): base("ResolveGenericTypeIdentifiers")
        {
            
        }
        
        protected override IType visitType(IType type)
        {
            base.visitType(type);
            
            //console.log(type && type.constructor.name, JSON.stringify(type));
            if (type is UnresolvedType unrType && ((this.currentInterface is Class class_ && class_.typeArguments.includes(unrType.typeName)) || (this.currentMethod is Method meth && meth.typeArguments.includes(unrType.typeName))))
                return new GenericsType(unrType.typeName);
            
            return type;
        }
    }
}