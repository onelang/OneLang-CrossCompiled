using One.Ast;
using System.Collections.Generic;

namespace One.Ast
{
    public enum Visibility { Public, Protected, Private }
    
    public interface IAstNode {
        
    }
    
    public interface IVariable {
        string name { get; set; }
        IType type { get; set; }
        MutabilityInfo mutability { get; set; }
    }
    
    public interface IClassMember {
        Visibility visibility { get; set; }
        bool isStatic { get; set; }
    }
    
    public interface IVariableWithInitializer : IVariable {
        Expression initializer { get; set; }
    }
    
    public interface IHasAttributesAndTrivia {
        string leadingTrivia { get; set; }
        Dictionary<string, string> attributes { get; set; }
    }
    
    public interface ISourceFileMember {
        SourceFile parentFile { get; set; }
    }
    
    public interface IInterface {
        string name { get; set; }
        string[] typeArguments { get; set; }
        IType[] baseInterfaces { get; set; }
        Field[] fields { get; set; }
        Method[] methods { get; set; }
        string leadingTrivia { get; set; }
        SourceFile parentFile { get; set; }
        
        IInterface[] getAllBaseInterfaces();
    }
    
    public interface IImportable {
        string name { get; set; }
        bool isExported { get; set; }
    }
    
    public interface IResolvedImportable : IImportable {
        SourceFile parentFile { get; set; }
    }
    
    public interface IMethodBase : IAstNode {
        MethodParameter[] parameters { get; set; }
        Block body { get; set; }
        bool throws { get; set; }
    }
    
    public interface IMethodBaseWithTrivia : IMethodBase, IHasAttributesAndTrivia {
        
    }
    
    public class MutabilityInfo {
        public bool unused;
        public bool reassigned;
        public bool mutated;
        
        public MutabilityInfo(bool unused, bool reassigned, bool mutated)
        {
            this.unused = unused;
            this.reassigned = reassigned;
            this.mutated = mutated;
        }
    }
    
    public class ExportedScope {
        public Map<string, IImportable> exports;
        
        public ExportedScope()
        {
            this.exports = new Map<string, IImportable>();
        }
        
        public IImportable getExport(string name)
        {
            var exp = this.exports.get(name);
            if (exp == null)
                throw new Error($"Export {name} was not found in exported symbols.");
            return exp;
        }
        
        public void addExport(string name, IImportable value)
        {
            this.exports.set(name, value);
        }
        
        public IImportable[] getAllExports()
        {
            return Array.from(this.exports.values());
        }
    }
    
    public class Package {
        public string name;
        public bool definitionOnly;
        public static string INDEX = "index";
        public Dictionary<string, SourceFile> files;
        public Dictionary<string, ExportedScope> exportedScopes;
        
        public Package(string name, bool definitionOnly)
        {
            this.name = name;
            this.definitionOnly = definitionOnly;
            this.files = new Dictionary<string, SourceFile> {};
            this.exportedScopes = new Dictionary<string, ExportedScope> {};
        }
        
        public static ExportedScope collectExportsFromFile(SourceFile file, bool exportAll, ExportedScope scope = null)
        {
            if (scope == null)
                scope = new ExportedScope();
            
            foreach (var cls in file.classes.filter(x => x.isExported || exportAll))
                scope.addExport(cls.name, cls);
            
            foreach (var intf in file.interfaces.filter(x => x.isExported || exportAll))
                scope.addExport(intf.name, intf);
            
            foreach (var enum_ in file.enums.filter(x => x.isExported || exportAll))
                scope.addExport(enum_.name, enum_);
            
            foreach (var func in file.funcs.filter(x => x.isExported || exportAll))
                scope.addExport(func.name, func);
            
            return scope;
        }
        
        public void addFile(SourceFile file, bool exportAll = false)
        {
            if (file.sourcePath.pkg != this || file.exportScope.packageName != this.name)
                throw new Error("This file belongs to another package!");
            
            this.files.set(file.sourcePath.path, file);
            var scopeName = file.exportScope.scopeName;
            this.exportedScopes.set(scopeName, Package.collectExportsFromFile(file, exportAll, this.exportedScopes.get(scopeName)));
        }
        
        public ExportedScope getExportedScope(string name)
        {
            var scope = this.exportedScopes.get(name);
            if (scope == null)
                throw new Error($"Scope \"{name}\" was not found in package \"{this.name}\"");
            return scope;
        }
    }
    
    public class Workspace {
        public Dictionary<string, Package> packages;
        
        public Workspace()
        {
            this.packages = new Dictionary<string, Package> {};
        }
        
        public void addPackage(Package pkg)
        {
            this.packages.set(pkg.name, pkg);
        }
        
        public Package getPackage(string name)
        {
            var pkg = this.packages.get(name);
            if (pkg == null)
                throw new Error($"Package was not found: \"{name}\"");
            return pkg;
        }
    }
    
    public class SourcePath {
        public Package pkg;
        public string path;
        
        public SourcePath(Package pkg, string path)
        {
            this.pkg = pkg;
            this.path = path;
        }
        
        public string toString()
        {
            return $"{this.pkg.name}/{this.path}";
        }
    }
    
    public class LiteralTypes {
        public ClassType boolean;
        public ClassType numeric;
        public ClassType string_;
        public ClassType regex;
        public ClassType array;
        public ClassType map;
        public ClassType error;
        public ClassType promise;
        
        public LiteralTypes(ClassType boolean, ClassType numeric, ClassType string_, ClassType regex, ClassType array, ClassType map, ClassType error, ClassType promise)
        {
            this.boolean = boolean;
            this.numeric = numeric;
            this.string_ = string_;
            this.regex = regex;
            this.array = array;
            this.map = map;
            this.error = error;
            this.promise = promise;
        }
    }
    
    public class SourceFile {
        public Import[] imports;
        public Interface[] interfaces;
        public Class[] classes;
        public Enum_[] enums;
        public GlobalFunction[] funcs;
        public Block mainBlock;
        public SourcePath sourcePath;
        public ExportScopeRef exportScope;
        public Map<string, IImportable> availableSymbols;
        public LiteralTypes literalTypes;
        public ClassType[] arrayTypes;
        
        public SourceFile(Import[] imports, Interface[] interfaces, Class[] classes, Enum_[] enums, GlobalFunction[] funcs, Block mainBlock, SourcePath sourcePath, ExportScopeRef exportScope)
        {
            this.imports = imports;
            this.interfaces = interfaces;
            this.classes = classes;
            this.enums = enums;
            this.funcs = funcs;
            this.mainBlock = mainBlock;
            this.sourcePath = sourcePath;
            this.exportScope = exportScope;
            this.availableSymbols = new Map<string, IImportable>();
            this.arrayTypes = new ClassType[0];
            var fileScope = Package.collectExportsFromFile(this, true);
            this.addAvailableSymbols(fileScope.getAllExports());
        }
        
        public void addAvailableSymbols(IImportable[] items)
        {
            foreach (var item in items)
                this.availableSymbols.set(item.name, item);
        }
    }
    
    public class ExportScopeRef {
        public string packageName;
        public string scopeName;
        
        public ExportScopeRef(string packageName, string scopeName)
        {
            this.packageName = packageName;
            this.scopeName = scopeName;
        }
        
        public string getId()
        {
            return $"{this.packageName}.{this.scopeName}";
        }
    }
    
    public class Import : IHasAttributesAndTrivia, ISourceFileMember {
        public ExportScopeRef exportScope;
        public bool importAll;
        public IImportable[] imports;
        public string importAs;
        public string leadingTrivia { get; set; }
        public SourceFile parentFile { get; set; }
        public Dictionary<string, string> attributes { get; set; }
        
        public Import(ExportScopeRef exportScope, bool importAll, IImportable[] imports, string importAs, string leadingTrivia)
        {
            this.exportScope = exportScope;
            this.importAll = importAll;
            this.imports = imports;
            this.importAs = importAs;
            this.leadingTrivia = leadingTrivia;
            if (importAs != null && !importAll)
                throw new Error("importAs only supported with importAll!");
        }
    }
    
    public class Enum_ : IAstNode, IHasAttributesAndTrivia, IResolvedImportable, ISourceFileMember, IReferencable {
        public string name { get; set; }
        public EnumMember[] values;
        public bool isExported { get; set; }
        public string leadingTrivia { get; set; }
        public SourceFile parentFile { get; set; }
        public Dictionary<string, string> attributes { get; set; }
        public List<EnumReference> references;
        public EnumType type;
        
        public Enum_(string name, EnumMember[] values, bool isExported, string leadingTrivia)
        {
            this.name = name;
            this.values = values;
            this.isExported = isExported;
            this.leadingTrivia = leadingTrivia;
            this.references = new List<EnumReference>();
            this.type = new EnumType(this);
        }
        
        public Reference createReference()
        {
            return new EnumReference(this);
        }
    }
    
    public class EnumMember : IAstNode {
        public string name;
        public Enum_ parentEnum;
        public List<EnumMemberReference> references;
        
        public EnumMember(string name)
        {
            this.name = name;
            this.references = new List<EnumMemberReference>();
        }
    }
    
    public class UnresolvedImport : IImportable {
        public string name { get; set; }
        public bool isExported { get; set; }
        
        public UnresolvedImport(string name)
        {
            this.name = name;
        }
    }
    
    public class Interface : IHasAttributesAndTrivia, IInterface, IResolvedImportable, ISourceFileMember {
        public string name { get; set; }
        public string[] typeArguments { get; set; }
        public IType[] baseInterfaces { get; set; }
        public Field[] fields { get; set; }
        public Method[] methods { get; set; }
        public bool isExported { get; set; }
        public string leadingTrivia { get; set; }
        public SourceFile parentFile { get; set; }
        public Dictionary<string, string> attributes { get; set; }
        public InterfaceType type;
        public IInterface[] _baseInterfaceCache;
        
        public Interface(string name, string[] typeArguments, IType[] baseInterfaces, Field[] fields, Method[] methods, bool isExported, string leadingTrivia)
        {
            this.name = name;
            this.typeArguments = typeArguments;
            this.baseInterfaces = baseInterfaces;
            this.fields = fields;
            this.methods = methods;
            this.isExported = isExported;
            this.leadingTrivia = leadingTrivia;
            this.type = new InterfaceType(this, this.typeArguments.map(x => new GenericsType(x)));
            this._baseInterfaceCache = null;
        }
        
        public IInterface[] getAllBaseInterfaces()
        {
            if (this._baseInterfaceCache == null)
                this._baseInterfaceCache = AstHelper.collectAllBaseInterfaces(this);
            return this._baseInterfaceCache;
        }
    }
    
    public class Class : IHasAttributesAndTrivia, IInterface, IResolvedImportable, ISourceFileMember, IReferencable {
        public string name { get; set; }
        public string[] typeArguments { get; set; }
        public IType baseClass;
        public IType[] baseInterfaces { get; set; }
        public Field[] fields { get; set; }
        public Property[] properties;
        public Constructor constructor_;
        public Method[] methods { get; set; }
        public bool isExported { get; set; }
        public string leadingTrivia { get; set; }
        public SourceFile parentFile { get; set; }
        public Dictionary<string, string> attributes { get; set; }
        public List<ClassReference> classReferences;
        public List<ThisReference> thisReferences;
        public List<StaticThisReference> staticThisReferences;
        public List<SuperReference> superReferences;
        public ClassType type;
        public IInterface[] _baseInterfaceCache;
        
        public Class(string name, string[] typeArguments, IType baseClass, IType[] baseInterfaces, Field[] fields, Property[] properties, Constructor constructor_, Method[] methods, bool isExported, string leadingTrivia)
        {
            this.name = name;
            this.typeArguments = typeArguments;
            this.baseClass = baseClass;
            this.baseInterfaces = baseInterfaces;
            this.fields = fields;
            this.properties = properties;
            this.constructor_ = constructor_;
            this.methods = methods;
            this.isExported = isExported;
            this.leadingTrivia = leadingTrivia;
            this.classReferences = new List<ClassReference>();
            this.thisReferences = new List<ThisReference>();
            this.staticThisReferences = new List<StaticThisReference>();
            this.superReferences = new List<SuperReference>();
            this.type = new ClassType(this, this.typeArguments.map(x => new GenericsType(x)));
            this._baseInterfaceCache = null;
        }
        
        public Reference createReference()
        {
            return new ClassReference(this);
        }
        
        public IInterface[] getAllBaseInterfaces()
        {
            if (this._baseInterfaceCache == null)
                this._baseInterfaceCache = AstHelper.collectAllBaseInterfaces(this);
            return this._baseInterfaceCache;
        }
    }
    
    public class Field : IVariableWithInitializer, IHasAttributesAndTrivia, IClassMember, IAstNode {
        public string name { get; set; }
        public IType type { get; set; }
        public Expression initializer { get; set; }
        public Visibility visibility { get; set; }
        public bool isStatic { get; set; }
        public MethodParameter constructorParam;
        public string leadingTrivia { get; set; }
        public IInterface parentInterface;
        public Dictionary<string, string> attributes { get; set; }
        public List<StaticFieldReference> staticReferences;
        public List<InstanceFieldReference> instanceReferences;
        public Field[] interfaceDeclarations;
        public MutabilityInfo mutability { get; set; }
        
        public Field(string name, IType type, Expression initializer, Visibility visibility, bool isStatic, MethodParameter constructorParam, string leadingTrivia)
        {
            this.name = name;
            this.type = type;
            this.initializer = initializer;
            this.visibility = visibility;
            this.isStatic = isStatic;
            this.constructorParam = constructorParam;
            this.leadingTrivia = leadingTrivia;
            this.parentInterface = null;
            this.staticReferences = new List<StaticFieldReference>();
            this.instanceReferences = new List<InstanceFieldReference>();
            this.interfaceDeclarations = null;
        }
    }
    
    public class Property : IVariable, IHasAttributesAndTrivia, IClassMember, IAstNode {
        public string name { get; set; }
        public IType type { get; set; }
        public Block getter;
        public Block setter;
        public Visibility visibility { get; set; }
        public bool isStatic { get; set; }
        public string leadingTrivia { get; set; }
        public Class parentClass;
        public Dictionary<string, string> attributes { get; set; }
        public List<StaticPropertyReference> staticReferences;
        public List<InstancePropertyReference> instanceReferences;
        public MutabilityInfo mutability { get; set; }
        
        public Property(string name, IType type, Block getter, Block setter, Visibility visibility, bool isStatic, string leadingTrivia)
        {
            this.name = name;
            this.type = type;
            this.getter = getter;
            this.setter = setter;
            this.visibility = visibility;
            this.isStatic = isStatic;
            this.leadingTrivia = leadingTrivia;
            this.parentClass = null;
            this.staticReferences = new List<StaticPropertyReference>();
            this.instanceReferences = new List<InstancePropertyReference>();
        }
    }
    
    public class MethodParameter : IVariableWithInitializer, IReferencable, IHasAttributesAndTrivia {
        public string name { get; set; }
        public IType type { get; set; }
        public Expression initializer { get; set; }
        public string leadingTrivia { get; set; }
        public Field fieldDecl;
        public IMethodBase parentMethod;
        public Dictionary<string, string> attributes { get; set; }
        public List<MethodParameterReference> references;
        public MutabilityInfo mutability { get; set; }
        
        public MethodParameter(string name, IType type, Expression initializer, string leadingTrivia)
        {
            this.name = name;
            this.type = type;
            this.initializer = initializer;
            this.leadingTrivia = leadingTrivia;
            this.fieldDecl = null;
            this.parentMethod = null;
            this.references = new List<MethodParameterReference>();
        }
        
        public Reference createReference()
        {
            return new MethodParameterReference(this);
        }
    }
    
    public class Constructor : IMethodBaseWithTrivia {
        public MethodParameter[] parameters { get; set; }
        public Block body { get; set; }
        public Expression[] superCallArgs;
        public string leadingTrivia { get; set; }
        public Class parentClass;
        public Dictionary<string, string> attributes { get; set; }
        public bool throws { get; set; }
        
        public Constructor(MethodParameter[] parameters, Block body, Expression[] superCallArgs, string leadingTrivia)
        {
            this.parameters = parameters;
            this.body = body;
            this.superCallArgs = superCallArgs;
            this.leadingTrivia = leadingTrivia;
            this.parentClass = null;
        }
    }
    
    public class Method : IMethodBaseWithTrivia, IClassMember {
        public string name;
        public string[] typeArguments;
        public MethodParameter[] parameters { get; set; }
        public Block body { get; set; }
        public Visibility visibility { get; set; }
        public bool isStatic { get; set; }
        public IType returns;
        public bool async;
        public string leadingTrivia { get; set; }
        public IInterface parentInterface;
        public Dictionary<string, string> attributes { get; set; }
        public Method[] interfaceDeclarations;
        public Method overrides;
        public List<Method> overriddenBy;
        public bool throws { get; set; }
        
        public Method(string name, string[] typeArguments, MethodParameter[] parameters, Block body, Visibility visibility, bool isStatic, IType returns, bool async, string leadingTrivia)
        {
            this.name = name;
            this.typeArguments = typeArguments;
            this.parameters = parameters;
            this.body = body;
            this.visibility = visibility;
            this.isStatic = isStatic;
            this.returns = returns;
            this.async = async;
            this.leadingTrivia = leadingTrivia;
            this.parentInterface = null;
            this.interfaceDeclarations = null;
            this.overrides = null;
            this.overriddenBy = new List<Method>();
        }
    }
    
    public class GlobalFunction : IMethodBaseWithTrivia, IResolvedImportable, IReferencable {
        public string name { get; set; }
        public MethodParameter[] parameters { get; set; }
        public Block body { get; set; }
        public IType returns;
        public bool isExported { get; set; }
        public string leadingTrivia { get; set; }
        public SourceFile parentFile { get; set; }
        public Dictionary<string, string> attributes { get; set; }
        public bool throws { get; set; }
        public List<GlobalFunctionReference> references;
        
        public GlobalFunction(string name, MethodParameter[] parameters, Block body, IType returns, bool isExported, string leadingTrivia)
        {
            this.name = name;
            this.parameters = parameters;
            this.body = body;
            this.returns = returns;
            this.isExported = isExported;
            this.leadingTrivia = leadingTrivia;
            this.references = new List<GlobalFunctionReference>();
        }
        
        public Reference createReference()
        {
            return new GlobalFunctionReference(this);
        }
    }
    
    public class Lambda : Expression, IMethodBase {
        public MethodParameter[] parameters { get; set; }
        public Block body { get; set; }
        public IType returns;
        public bool throws { get; set; }
        public List<IVariable> captures;
        
        public Lambda(MethodParameter[] parameters, Block body): base()
        {
            this.parameters = parameters;
            this.body = body;
            this.returns = null;
            this.captures = null;
        }
    }
}