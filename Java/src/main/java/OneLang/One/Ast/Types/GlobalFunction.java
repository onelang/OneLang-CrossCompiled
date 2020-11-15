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
import OneLang.One.Ast.Types.IResolvedImportable;
import OneLang.One.Ast.References.IReferencable;
import OneLang.One.Ast.Types.MethodParameter;
import OneLang.One.Ast.Statements.Block;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.Types.SourceFile;
import java.util.Map;
import java.util.List;
import OneLang.One.Ast.References.GlobalFunctionReference;
import java.util.ArrayList;
import OneLang.One.Ast.References.Reference;

public class GlobalFunction implements IMethodBaseWithTrivia, IResolvedImportable, IReferencable {
    public IType returns;
    public List<GlobalFunctionReference> references;
    
    String name;
    public String getName() { return this.name; }
    public void setName(String value) { this.name = value; }
    
    MethodParameter[] parameters;
    public MethodParameter[] getParameters() { return this.parameters; }
    public void setParameters(MethodParameter[] value) { this.parameters = value; }
    
    Block body;
    public Block getBody() { return this.body; }
    public void setBody(Block value) { this.body = value; }
    
    Boolean isExported = false;
    public Boolean getIsExported() { return this.isExported; }
    public void setIsExported(Boolean value) { this.isExported = value; }
    
    String leadingTrivia;
    public String getLeadingTrivia() { return this.leadingTrivia; }
    public void setLeadingTrivia(String value) { this.leadingTrivia = value; }
    
    SourceFile parentFile = null;
    public SourceFile getParentFile() { return this.parentFile; }
    public void setParentFile(SourceFile value) { this.parentFile = value; }
    
    Map<String, String> attributes = null;
    public Map<String, String> getAttributes() { return this.attributes; }
    public void setAttributes(Map<String, String> value) { this.attributes = value; }
    
    Boolean throws_ = false;
    public Boolean getThrows() { return this.throws_; }
    public void setThrows(Boolean value) { this.throws_ = value; }
    
    public GlobalFunction(String name, MethodParameter[] parameters, Block body, IType returns, Boolean isExported, String leadingTrivia)
    {
        this.setName(name);
        this.setParameters(parameters);
        this.setBody(body);
        this.returns = returns;
        this.setIsExported(isExported);
        this.setLeadingTrivia(leadingTrivia);
        this.references = new ArrayList<GlobalFunctionReference>();
    }
    
    public Reference createReference() {
        return new GlobalFunctionReference(this);
    }
}