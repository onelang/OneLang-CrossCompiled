package OneLang.One.Transforms.ResolveGenericTypeIdentifiers;

import OneLang.One.AstTransformer.AstTransformer;
import OneLang.One.Ast.AstTypes.UnresolvedType;
import OneLang.One.Ast.AstTypes.GenericsType;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Interfaces.IType;

import OneLang.One.AstTransformer.AstTransformer;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.AstTypes.UnresolvedType;
import OneLang.One.Ast.Types.Class;
import java.util.Arrays;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.AstTypes.GenericsType;

public class ResolveGenericTypeIdentifiers extends AstTransformer {
    public ResolveGenericTypeIdentifiers()
    {
        super("ResolveGenericTypeIdentifiers");
        
    }
    
    protected IType visitType(IType type) {
        super.visitType(type);
        
        //console.log(type && type.constructor.name, JSON.stringify(type));
        if (type instanceof UnresolvedType && ((this.currentInterface instanceof Class && Arrays.stream(((Class)this.currentInterface).getTypeArguments()).anyMatch(((UnresolvedType)type).typeName::equals)) || (this.currentMethod instanceof Method && Arrays.stream(((Method)this.currentMethod).typeArguments).anyMatch(((UnresolvedType)type).typeName::equals))))
            return new GenericsType(((UnresolvedType)type).typeName);
        
        return type;
    }
}