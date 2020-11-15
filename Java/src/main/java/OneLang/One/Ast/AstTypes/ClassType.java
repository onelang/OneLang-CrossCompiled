package OneLang.One.Ast.AstTypes;

import OneLang.One.Ast.Types.Enum;
import OneLang.One.Ast.Types.Interface;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.MethodParameter;
import OneLang.One.Ast.Types.IInterface;
import OneLang.One.Ast.Interfaces.IType;

import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.AstTypes.IHasTypeArguments;
import OneLang.One.Ast.AstTypes.IInterfaceType;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.IInterface;

public class ClassType implements IType, IHasTypeArguments, IInterfaceType {
    public Class decl;
    
    IType[] typeArguments;
    public IType[] getTypeArguments() { return this.typeArguments; }
    public void setTypeArguments(IType[] value) { this.typeArguments = value; }
    
    public ClassType(Class decl, IType[] typeArguments)
    {
        this.decl = decl;
        this.setTypeArguments(typeArguments);
    }
    
    public IInterface getDecl() {
        return this.decl;
    }
    
    public String repr() {
        return "C:" + this.decl.getName() + TypeHelper.argsRepr(this.getTypeArguments());
    }
}