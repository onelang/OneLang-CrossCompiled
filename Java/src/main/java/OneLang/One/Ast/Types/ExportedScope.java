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

import OneLang.One.Ast.Types.IImportable;
import java.util.Map;
import java.util.LinkedHashMap;

public class ExportedScope {
    public Map<String, IImportable> exports;
    
    public ExportedScope()
    {
        this.exports = new LinkedHashMap<String, IImportable>();
    }
    
    public IImportable getExport(String name) {
        var exp = this.exports.get(name);
        if (exp == null)
            throw new Error("Export " + name + " was not found in exported symbols.");
        return exp;
    }
    
    public void addExport(String name, IImportable value) {
        this.exports.put(name, value);
    }
    
    public IImportable[] getAllExports() {
        return this.exports.values().toArray(IImportable[]::new);
    }
}