package OneLang.One.Ast.AstTypes;

import OneLang.One.Ast.Types.Enum;
import OneLang.One.Ast.Types.Interface;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.MethodParameter;
import OneLang.One.Ast.Types.IInterface;
import OneLang.One.Ast.Interfaces.IType;

import OneLang.One.Ast.Interfaces.IType;

public class GenericsType implements IType {
    public String typeVarName;
    
    public GenericsType(String typeVarName)
    {
        this.typeVarName = typeVarName;
    }
    
    public String repr() {
        return "G:" + this.typeVarName;
    }
}