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

import OneLang.One.Ast.Types.SourceFile;
import java.util.Map;
import OneLang.One.Ast.Types.ExportedScope;
import java.util.LinkedHashMap;
import java.util.Arrays;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.Interface;
import OneLang.One.Ast.Types.Enum;
import OneLang.One.Ast.Types.GlobalFunction;
import OneStd.Objects;

public class Package {
    public String name;
    public Boolean definitionOnly;
    public static String INDEX = "index";
    public Map<String, SourceFile> files;
    public Map<String, ExportedScope> exportedScopes;
    
    public Package(String name, Boolean definitionOnly)
    {
        this.name = name;
        this.definitionOnly = definitionOnly;
        this.files = new LinkedHashMap<String, SourceFile>();
        this.exportedScopes = new LinkedHashMap<String, ExportedScope>();
    }
    
    public static ExportedScope collectExportsFromFile(SourceFile file, Boolean exportAll, ExportedScope scope) {
        if (scope == null)
            scope = new ExportedScope();
        
        for (var cls : Arrays.stream(file.classes).filter(x -> x.getIsExported() || exportAll).toArray(Class[]::new))
            scope.addExport(cls.getName(), cls);
        
        for (var intf : Arrays.stream(file.interfaces).filter(x -> x.getIsExported() || exportAll).toArray(Interface[]::new))
            scope.addExport(intf.getName(), intf);
        
        for (var enum_ : Arrays.stream(file.enums).filter(x -> x.getIsExported() || exportAll).toArray(Enum[]::new))
            scope.addExport(enum_.getName(), enum_);
        
        for (var func : Arrays.stream(file.funcs).filter(x -> x.getIsExported() || exportAll).toArray(GlobalFunction[]::new))
            scope.addExport(func.getName(), func);
        
        return scope;
    }
    
    public void addFile(SourceFile file, Boolean exportAll) {
        if (file.sourcePath.pkg != this || !Objects.equals(file.exportScope.packageName, this.name))
            throw new Error("This file belongs to another package!");
        
        this.files.put(file.sourcePath.path, file);
        var scopeName = file.exportScope.scopeName;
        this.exportedScopes.put(scopeName, Package.collectExportsFromFile(file, exportAll, this.exportedScopes.get(scopeName)));
    }
    
    public ExportedScope getExportedScope(String name) {
        var scope = this.exportedScopes.get(name);
        if (scope == null)
            throw new Error("Scope \"" + name + "\" was not found in package \"" + this.name + "\"");
        return scope;
    }
}