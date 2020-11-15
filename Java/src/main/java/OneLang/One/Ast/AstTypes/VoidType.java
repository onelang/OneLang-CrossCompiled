package OneLang.One.Ast.AstTypes;

import OneLang.One.Ast.Types.Enum;
import OneLang.One.Ast.Types.Interface;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.MethodParameter;
import OneLang.One.Ast.Types.IInterface;
import OneLang.One.Ast.Interfaces.IType;

import OneLang.One.Ast.AstTypes.IPrimitiveType;
import OneLang.One.Ast.AstTypes.VoidType;

public class VoidType implements IPrimitiveType {
    public static VoidType instance;
    
    static {
        VoidType.instance = new VoidType();
    }
    
    public String repr() {
        return "Void";
    }
}