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
import OneLang.One.Ast.Types.IInterface;
import OneLang.One.Ast.Types.IResolvedImportable;
import OneLang.One.Ast.Types.ISourceFileMember;
import OneLang.One.Ast.References.IReferencable;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.Types.Field;
import OneLang.One.Ast.Types.Property;
import OneLang.One.Ast.Types.Constructor;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Types.SourceFile;
import java.util.Map;
import java.util.List;
import OneLang.One.Ast.References.ClassReference;
import OneLang.One.Ast.References.ThisReference;
import OneLang.One.Ast.References.StaticThisReference;
import OneLang.One.Ast.References.SuperReference;
import OneLang.One.Ast.AstTypes.ClassType;
import java.util.ArrayList;
import OneLang.One.Ast.AstTypes.GenericsType;
import java.util.Arrays;
import OneLang.One.Ast.References.Reference;

public class Class implements IHasAttributesAndTrivia, IInterface, IResolvedImportable, ISourceFileMember, IReferencable {
    public IType baseClass;
    public Property[] properties;
    public Constructor constructor_;
    public List<ClassReference> classReferences;
    public List<ThisReference> thisReferences;
    public List<StaticThisReference> staticThisReferences;
    public List<SuperReference> superReferences;
    public ClassType type;
    public IInterface[] _baseInterfaceCache;
    
    String name;
    public String getName() { return this.name; }
    public void setName(String value) { this.name = value; }
    
    String[] typeArguments;
    public String[] getTypeArguments() { return this.typeArguments; }
    public void setTypeArguments(String[] value) { this.typeArguments = value; }
    
    IType[] baseInterfaces;
    public IType[] getBaseInterfaces() { return this.baseInterfaces; }
    public void setBaseInterfaces(IType[] value) { this.baseInterfaces = value; }
    
    Field[] fields;
    public Field[] getFields() { return this.fields; }
    public void setFields(Field[] value) { this.fields = value; }
    
    Method[] methods;
    public Method[] getMethods() { return this.methods; }
    public void setMethods(Method[] value) { this.methods = value; }
    
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
    
    public Class(String name, String[] typeArguments, IType baseClass, IType[] baseInterfaces, Field[] fields, Property[] properties, Constructor constructor_, Method[] methods, Boolean isExported, String leadingTrivia)
    {
        this.setName(name);
        this.setTypeArguments(typeArguments);
        this.baseClass = baseClass;
        this.setBaseInterfaces(baseInterfaces);
        this.setFields(fields);
        this.properties = properties;
        this.constructor_ = constructor_;
        this.setMethods(methods);
        this.setIsExported(isExported);
        this.setLeadingTrivia(leadingTrivia);
        this.classReferences = new ArrayList<ClassReference>();
        this.thisReferences = new ArrayList<ThisReference>();
        this.staticThisReferences = new ArrayList<StaticThisReference>();
        this.superReferences = new ArrayList<SuperReference>();
        this.type = new ClassType(this, Arrays.stream(this.getTypeArguments()).map(x -> new GenericsType(x)).toArray(GenericsType[]::new));
        this._baseInterfaceCache = null;
    }
    
    public Reference createReference() {
        return new ClassReference(this);
    }
    
    public IInterface[] getAllBaseInterfaces() {
        if (this._baseInterfaceCache == null)
            this._baseInterfaceCache = AstHelper.collectAllBaseInterfaces(this);
        return this._baseInterfaceCache;
    }
}