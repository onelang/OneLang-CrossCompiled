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
import OneLang.One.Ast.References.IInstanceMemberReference;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Types.Field;
import OneLang.One.Ast.Types.IVariable;

public class InstanceFieldReference extends VariableReference implements IInstanceMemberReference {
    public Field field;
    
    Expression object;
    public Expression getObject() { return this.object; }
    public void setObject(Expression value) { this.object = value; }
    
    public InstanceFieldReference(Expression object, Field field)
    {
        super();
        this.setObject(object);
        this.field = field;
        field.instanceReferences.add(this);
    }
    
    public IVariable getVariable() {
        return this.field;
    }
}