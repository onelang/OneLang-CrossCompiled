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

import OneLang.One.Ast.References.VariableReference;
import OneLang.One.Ast.Types.MethodParameter;
import OneLang.One.Ast.Types.Lambda;
import java.util.Arrays;
import OneLang.One.Ast.Types.Constructor;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.Types.IVariable;

public class MethodParameterReference extends VariableReference {
    public MethodParameter decl;
    
    public MethodParameterReference(MethodParameter decl)
    {
        super();
        this.decl = decl;
        decl.references.add(this);
    }
    
    public void setActualType(IType type, Boolean allowVoid, Boolean allowGeneric) {
        super.setActualType(type, false, this.decl.parentMethod instanceof Lambda ? Arrays.stream(((Lambda)this.decl.parentMethod).getParameters()).anyMatch(x -> TypeHelper.isGeneric(x.getType())) : this.decl.parentMethod instanceof Constructor ? ((Constructor)this.decl.parentMethod).parentClass.getTypeArguments().length > 0 : this.decl.parentMethod instanceof Method ? ((Method)this.decl.parentMethod).typeArguments.length > 0 || ((Method)this.decl.parentMethod).parentInterface.getTypeArguments().length > 0 : false);
    }
    
    public IVariable getVariable() {
        return this.decl;
    }
}