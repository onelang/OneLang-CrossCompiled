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

import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.Types.Field;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Types.SourceFile;
import OneLang.One.Ast.Types.IInterface;

public interface IInterface {
    String getName();
    void setName(String value);
    
    String[] getTypeArguments();
    void setTypeArguments(String[] value);
    
    IType[] getBaseInterfaces();
    void setBaseInterfaces(IType[] value);
    
    Field[] getFields();
    void setFields(Field[] value);
    
    Method[] getMethods();
    void setMethods(Method[] value);
    
    String getLeadingTrivia();
    void setLeadingTrivia(String value);
    
    SourceFile getParentFile();
    void setParentFile(SourceFile value);
    
    IInterface[] getAllBaseInterfaces();
}