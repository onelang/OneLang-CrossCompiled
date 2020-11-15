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

import OneLang.One.Ast.Types.IMethodBaseWithTrivia;
import OneLang.One.Ast.Types.IClassMember;
import OneLang.One.Ast.Types.MethodParameter;
import OneLang.One.Ast.Statements.Block;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.Types.IInterface;
import java.util.Map;
import OneLang.One.Ast.Types.Method;
import java.util.List;
import java.util.ArrayList;

public class Method implements IMethodBaseWithTrivia, IClassMember {
    public String name;
    public String[] typeArguments;
    public IType returns;
    public Boolean async;
    public IInterface parentInterface;
    public Method[] interfaceDeclarations;
    public Method overrides;
    public List<Method> overriddenBy;
    
    MethodParameter[] parameters;
    public MethodParameter[] getParameters() { return this.parameters; }
    public void setParameters(MethodParameter[] value) { this.parameters = value; }
    
    Block body;
    public Block getBody() { return this.body; }
    public void setBody(Block value) { this.body = value; }
    
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
    
    Boolean throws_ = false;
    public Boolean getThrows() { return this.throws_; }
    public void setThrows(Boolean value) { this.throws_ = value; }
    
    public Method(String name, String[] typeArguments, MethodParameter[] parameters, Block body, Visibility visibility, Boolean isStatic, IType returns, Boolean async, String leadingTrivia)
    {
        this.name = name;
        this.typeArguments = typeArguments;
        this.setParameters(parameters);
        this.setBody(body);
        this.setVisibility(visibility);
        this.setIsStatic(isStatic);
        this.returns = returns;
        this.async = async;
        this.setLeadingTrivia(leadingTrivia);
        this.parentInterface = null;
        this.interfaceDeclarations = null;
        this.overrides = null;
        this.overriddenBy = new ArrayList<Method>();
    }
}