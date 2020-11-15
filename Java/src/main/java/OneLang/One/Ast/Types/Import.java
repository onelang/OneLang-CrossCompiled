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

import OneLang.One.Ast.Types.IHasAttributesAndTrivia;
import OneLang.One.Ast.Types.ISourceFileMember;
import OneLang.One.Ast.Types.ExportScopeRef;
import OneLang.One.Ast.Types.IImportable;
import OneLang.One.Ast.Types.SourceFile;
import java.util.Map;

public class Import implements IHasAttributesAndTrivia, ISourceFileMember {
    public ExportScopeRef exportScope;
    public Boolean importAll;
    public IImportable[] imports;
    public String importAs;
    
    String leadingTrivia;
    public String getLeadingTrivia() { return this.leadingTrivia; }
    public void setLeadingTrivia(String value) { this.leadingTrivia = value; }
    
    SourceFile parentFile = null;
    public SourceFile getParentFile() { return this.parentFile; }
    public void setParentFile(SourceFile value) { this.parentFile = value; }
    
    Map<String, String> attributes = null;
    public Map<String, String> getAttributes() { return this.attributes; }
    public void setAttributes(Map<String, String> value) { this.attributes = value; }
    
    public Import(ExportScopeRef exportScope, Boolean importAll, IImportable[] imports, String importAs, String leadingTrivia)
    {
        this.exportScope = exportScope;
        this.importAll = importAll;
        this.imports = imports;
        this.importAs = importAs;
        this.setLeadingTrivia(leadingTrivia);
        if (importAs != null && !importAll)
            throw new Error("importAs only supported with importAll!");
    }
}