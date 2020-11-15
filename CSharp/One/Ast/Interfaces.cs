

namespace One.Ast
{
    public interface IType {
        string repr();
    }
    
    public interface IExpression {
        void setActualType(IType actualType, bool allowVoid, bool allowGeneric);
        
        void setExpectedType(IType type, bool allowVoid);
        
        IType getType();
    }
}