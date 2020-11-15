package OneLang.One.Ast.AstTypes;

import OneLang.One.Ast.Types.Enum;
import OneLang.One.Ast.Types.Interface;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.MethodParameter;
import OneLang.One.Ast.Types.IInterface;
import OneLang.One.Ast.Interfaces.IType;

import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.Types.IInterface;

public interface IInterfaceType extends IType {
    IType[] getTypeArguments();
    void setTypeArguments(IType[] value);
    
    IInterface getDecl();
}