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

import OneLang.One.Ast.Types.Import;
import OneLang.One.Ast.Types.Interface;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.Enum;
import OneLang.One.Ast.Types.GlobalFunction;
import OneLang.One.Ast.Statements.Block;
import OneLang.One.Ast.Types.SourcePath;
import OneLang.One.Ast.Types.ExportScopeRef;
import OneLang.One.Ast.Types.IImportable;
import java.util.Map;
import OneLang.One.Ast.Types.LiteralTypes;
import OneLang.One.Ast.AstTypes.ClassType;
import java.util.LinkedHashMap;

public class SourceFile {
    public Import[] imports;
    public Interface[] interfaces;
    public Class[] classes;
    public Enum[] enums;
    public GlobalFunction[] funcs;
    public Block mainBlock;
    public SourcePath sourcePath;
    public ExportScopeRef exportScope;
    public Map<String, IImportable> availableSymbols;
    public LiteralTypes literalTypes;
    public ClassType[] arrayTypes;
    
    public SourceFile(Import[] imports, Interface[] interfaces, Class[] classes, Enum[] enums, GlobalFunction[] funcs, Block mainBlock, SourcePath sourcePath, ExportScopeRef exportScope)
    {
        this.imports = imports;
        this.interfaces = interfaces;
        this.classes = classes;
        this.enums = enums;
        this.funcs = funcs;
        this.mainBlock = mainBlock;
        this.sourcePath = sourcePath;
        this.exportScope = exportScope;
        this.availableSymbols = new LinkedHashMap<String, IImportable>();
        this.arrayTypes = new ClassType[0];
        var fileScope = Package.collectExportsFromFile(this, true, null);
        this.addAvailableSymbols(fileScope.getAllExports());
    }
    
    public void addAvailableSymbols(IImportable[] items) {
        for (var item : items)
            this.availableSymbols.put(item.getName(), item);
    }
}