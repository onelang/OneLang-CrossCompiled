package OneLang.One.Ast.Interfaces;



import OneLang.One.Ast.Interfaces.IType;

public interface IExpression {
    void setActualType(IType actualType, Boolean allowVoid, Boolean allowGeneric);
    void setExpectedType(IType type, Boolean allowVoid);
    IType getType();
}