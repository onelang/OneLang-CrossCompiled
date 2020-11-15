package OneLang.One.Ast.References;

import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.Enum;
import OneLang.One.Ast.Types.MethodParameter;
import OneLang.One.Ast.Types.GlobalFunction;
import OneLang.One.Ast.Types.Field;
import OneLang.One.Ast.Types.Property;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Types.EnumMember;
import OneLang.One.Ast.Types.IMethodBase;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.Ast.Types.Constructor;
import OneLang.One.Ast.Types.IVariable;
import OneLang.One.Ast.Statements.VariableDeclaration;
import OneLang.One.Ast.Statements.ForVariable;
import OneLang.One.Ast.Statements.ForeachVariable;
import OneLang.One.Ast.Statements.CatchVariable;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.TypeRestriction;
import OneLang.One.Ast.AstTypes.EnumType;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.TypeHelper;
import OneLang.One.Ast.Interfaces.IExpression;
import OneLang.One.Ast.Interfaces.IType;

import OneLang.One.Ast.References.Reference;
import OneLang.One.Ast.References.IGetMethodBase;
import OneLang.One.Ast.Types.GlobalFunction;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.Types.IMethodBase;

public class GlobalFunctionReference extends Reference implements IGetMethodBase {
    public GlobalFunction decl;
    
    public GlobalFunctionReference(GlobalFunction decl)
    {
        super();
        this.decl = decl;
        decl.references.add(this);
    }
    
    public void setActualType(IType type, Boolean allowVoid, Boolean allowGeneric) {
        throw new Error("GlobalFunctionReference cannot have a type!");
    }
    
    public IMethodBase getMethodBase() {
        return this.decl;
    }
}