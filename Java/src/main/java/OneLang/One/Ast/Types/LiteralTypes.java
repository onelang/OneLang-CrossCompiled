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

import OneLang.One.Ast.AstTypes.ClassType;

public class LiteralTypes {
    public ClassType boolean_;
    public ClassType numeric;
    public ClassType string;
    public ClassType regex;
    public ClassType array;
    public ClassType map;
    public ClassType error;
    public ClassType promise;
    
    public LiteralTypes(ClassType boolean_, ClassType numeric, ClassType string, ClassType regex, ClassType array, ClassType map, ClassType error, ClassType promise)
    {
        this.boolean_ = boolean_;
        this.numeric = numeric;
        this.string = string;
        this.regex = regex;
        this.array = array;
        this.map = map;
        this.error = error;
        this.promise = promise;
    }
}