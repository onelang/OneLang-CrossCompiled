package OneLang.One.Ast.AstTypes;

import OneLang.One.Ast.Types.Enum;
import OneLang.One.Ast.Types.Interface;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.MethodParameter;
import OneLang.One.Ast.Types.IInterface;
import OneLang.One.Ast.Interfaces.IType;

import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.Types.Enum;

public class EnumType implements IType {
    public Enum decl;
    
    public EnumType(Enum decl)
    {
        this.decl = decl;
    }
    
    public String repr() {
        return "E:" + this.decl.getName();
    }
}