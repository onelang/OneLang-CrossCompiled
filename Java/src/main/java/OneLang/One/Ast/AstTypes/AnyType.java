package OneLang.One.Ast.AstTypes;

import OneLang.One.Ast.Types.Enum;
import OneLang.One.Ast.Types.Interface;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.MethodParameter;
import OneLang.One.Ast.Types.IInterface;
import OneLang.One.Ast.Interfaces.IType;

import OneLang.One.Ast.AstTypes.IPrimitiveType;
import OneLang.One.Ast.AstTypes.AnyType;

public class AnyType implements IPrimitiveType {
    public static AnyType instance;
    
    static {
        AnyType.instance = new AnyType();
    }
    
    public String repr() {
        return "Any";
    }
}