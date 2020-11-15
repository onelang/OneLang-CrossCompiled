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

import OneLang.One.Ast.Types.IAstNode;
import OneLang.One.Ast.Types.IHasAttributesAndTrivia;
import OneLang.One.Ast.Types.IResolvedImportable;
import OneLang.One.Ast.Types.ISourceFileMember;
import OneLang.One.Ast.References.IReferencable;
import OneLang.One.Ast.Types.EnumMember;
import OneLang.One.Ast.Types.SourceFile;
import java.util.Map;
import java.util.List;
import OneLang.One.Ast.References.EnumReference;
import OneLang.One.Ast.AstTypes.EnumType;
import java.util.ArrayList;
import OneLang.One.Ast.References.Reference;

public class Enum implements IAstNode, IHasAttributesAndTrivia, IResolvedImportable, ISourceFileMember, IReferencable {
    public EnumMember[] values;
    public List<EnumReference> references;
    public EnumType type;
    
    String name;
    public String getName() { return this.name; }
    public void setName(String value) { this.name = value; }
    
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
    
    public Enum(String name, EnumMember[] values, Boolean isExported, String leadingTrivia)
    {
        this.setName(name);
        this.values = values;
        this.setIsExported(isExported);
        this.setLeadingTrivia(leadingTrivia);
        this.references = new ArrayList<EnumReference>();
        this.type = new EnumType(this);
    }
    
    public Reference createReference() {
        return new EnumReference(this);
    }
}