package OneLang.One.Ast.AstTypes;

import OneLang.One.Ast.Types.Enum;
import OneLang.One.Ast.Types.Interface;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.MethodParameter;
import OneLang.One.Ast.Types.IInterface;
import OneLang.One.Ast.Interfaces.IType;

import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.AstTypes.IHasTypeArguments;

public class UnresolvedType implements IType, IHasTypeArguments {
    public String typeName;
    
    IType[] typeArguments;
    public IType[] getTypeArguments() { return this.typeArguments; }
    public void setTypeArguments(IType[] value) { this.typeArguments = value; }
    
    public UnresolvedType(String typeName, IType[] typeArguments)
    {
        this.typeName = typeName;
        this.setTypeArguments(typeArguments);
    }
    
    public String repr() {
        return "X:" + this.typeName + TypeHelper.argsRepr(this.getTypeArguments());
    }
}