package OneLang.One.Transforms.InferTypes;

import OneLang.One.AstTransformer.AstTransformer;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Types.Package;
import OneLang.One.Ast.Types.Property;
import OneLang.One.Ast.Types.Field;
import OneLang.One.Ast.Types.IMethodBase;
import OneLang.One.Ast.Types.IVariableWithInitializer;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.Ast.Types.IVariable;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.SourceFile;
import OneLang.One.Transforms.InferTypesPlugins.BasicTypeInfer.BasicTypeInfer;
import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import OneLang.One.Transforms.InferTypesPlugins.ArrayAndMapLiteralTypeInfer.ArrayAndMapLiteralTypeInfer;
import OneLang.One.Transforms.InferTypesPlugins.ResolveFieldAndPropertyAccess.ResolveFieldAndPropertyAccess;
import OneLang.One.Transforms.InferTypesPlugins.ResolveMethodCalls.ResolveMethodCalls;
import OneLang.One.Transforms.InferTypesPlugins.LambdaResolver.LambdaResolver;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.Statements.Block;
import OneLang.One.Ast.Statements.ReturnStatement;
import OneLang.One.Transforms.InferTypesPlugins.ResolveEnumMemberAccess.ResolveEnumMemberAccess;
import OneLang.One.Transforms.InferTypesPlugins.InferReturnType.InferReturnType;
import OneLang.One.Transforms.InferTypesPlugins.TypeScriptNullCoalesce.TypeScriptNullCoalesce;
import OneLang.One.Transforms.InferTypesPlugins.InferForeachVarType.InferForeachVarType;
import OneLang.One.Transforms.InferTypesPlugins.ResolveFuncCalls.ResolveFuncCalls;
import OneLang.One.Transforms.InferTypesPlugins.NullabilityCheckWithNot.NullabilityCheckWithNot;
import OneLang.One.Transforms.InferTypesPlugins.ResolveNewCall.ResolveNewCalls;
import OneLang.One.Transforms.InferTypesPlugins.ResolveElementAccess.ResolveElementAccess;
import OneLang.One.Ast.AstTypes.ClassType;

public enum InferTypesStage { Invalid, Fields, Properties, Methods }