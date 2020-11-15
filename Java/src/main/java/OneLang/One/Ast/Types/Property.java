package OneLang.One.Ast.Types;

import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.GenericsType;
import OneLang.One.Ast.AstTypes.EnumType;
import OneLang.One.Ast.AstTypes.InterfaceType;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.References.ClassReference;
import OneLang.One.Ast.References.EnumReference;
import OneLang.One.Ast.References.ThisReference;
import OneLang.One.Ast.References.MethodParameterReference;
import OneLang.One.Ast.References.SuperReference;
import OneLang.One.Ast.References.StaticFieldReference;
import OneLang.One.Ast.References.EnumMemberReference;
import OneLang.One.Ast.References.InstanceFieldReference;
import OneLang.One.Ast.References.StaticPropertyReference;
import OneLang.One.Ast.References.InstancePropertyReference;
import OneLang.One.Ast.References.IReferencable;
import OneLang.One.Ast.References.Reference;
import OneLang.One.Ast.References.GlobalFunctionReference;
import OneLang.One.Ast.References.StaticThisReference;
import OneLang.One.Ast.References.VariableReference;
import OneLang.One.Ast.AstHelper.AstHelper;
import OneLang.One.Ast.Statements.Block;
import OneLang.One.Ast.Interfaces.IType;

import OneLang.One.Ast.Types.IVariable;
import OneLang.One.Ast.Types.IHasAttributesAndTrivia;
import OneLang.One.Ast.Types.IClassMember;
import OneLang.One.Ast.Types.IAstNode;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.Statements.Block;
import OneLang.One.Ast.Types.Class;
import java.util.Map;
import java.util.List;
import OneLang.One.Ast.References.StaticPropertyReference;
import OneLang.One.Ast.References.InstancePropertyReference;
import OneLang.One.Ast.Types.MutabilityInfo;
import java.util.ArrayList;

public class Property implements IVariable, IHasAttributesAndTrivia, IClassMember, IAstNode {
    public Block getter;
    public Block setter;
    public Class parentClass;
    public List<StaticPropertyReference> staticReferences;
    public List<InstancePropertyReference> instanceReferences;
    
    String name;
    public String getName() { return this.name; }
    public void setName(String value) { this.name = value; }
    
    IType type;
    public IType getType() { return this.type; }
    public void setType(IType value) { this.type = value; }
    
    Visibility visibility;
    public Visibility getVisibility() { return this.visibility; }
    public void setVisibility(Visibility value) { this.visibility = value; }
    
    Boolean isStatic = false;
    public Boolean getIsStatic() { return this.isStatic; }
    public void setIsStatic(Boolean value) { this.isStatic = value; }
    
    String leadingTrivia;
    public String getLeadingTrivia() { return this.leadingTrivia; }
    public void setLeadingTrivia(String value) { this.leadingTrivia = value; }
    
    Map<String, String> attributes = null;
    public Map<String, String> getAttributes() { return this.attributes; }
    public void setAttributes(Map<String, String> value) { this.attributes = value; }
    
    MutabilityInfo mutability = null;
    public MutabilityInfo getMutability() { return this.mutability; }
    public void setMutability(MutabilityInfo value) { this.mutability = value; }
    
    public Property(String name, IType type, Block getter, Block setter, Visibility visibility, Boolean isStatic, String leadingTrivia)
    {
        this.setName(name);
        this.setType(type);
        this.getter = getter;
        this.setter = setter;
        this.setVisibility(visibility);
        this.setIsStatic(isStatic);
        this.setLeadingTrivia(leadingTrivia);
        this.parentClass = null;
        this.staticReferences = new ArrayList<StaticPropertyReference>();
        this.instanceReferences = new ArrayList<InstancePropertyReference>();
    }
}