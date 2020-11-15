package OneLang.One.Compiler;

import OneLang.One.Ast.Types.Workspace;
import OneLang.One.Ast.Types.Package;
import OneLang.One.Ast.Types.SourcePath;
import OneLang.One.Ast.Types.SourceFile;
import OneLang.One.Ast.Types.ExportedScope;
import OneLang.One.Ast.Types.LiteralTypes;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.ExportScopeRef;
import OneLang.Parsers.TypeScriptParser2.TypeScriptParser2;
import OneLang.StdLib.PackageManager.PackageManager;
import OneLang.StdLib.PackagesFolderSource.PackagesFolderSource;
import OneLang.One.Transforms.FillParent.FillParent;
import OneLang.One.Transforms.FillAttributesFromTrivia.FillAttributesFromTrivia;
import OneLang.One.Transforms.ResolveGenericTypeIdentifiers.ResolveGenericTypeIdentifiers;
import OneLang.One.Transforms.ResolveUnresolvedTypes.ResolveUnresolvedTypes;
import OneLang.One.Transforms.ResolveImports.ResolveImports;
import OneLang.One.Transforms.ConvertToMethodCall.ConvertToMethodCall;
import OneLang.One.Transforms.ResolveIdentifiers.ResolveIdentifiers;
import OneLang.One.Transforms.InstanceOfImplicitCast.InstanceOfImplicitCast;
import OneLang.One.Transforms.DetectMethodCalls.DetectMethodCalls;
import OneLang.One.Transforms.InferTypes.InferTypes;
import OneLang.One.Transforms.CollectInheritanceInfo.CollectInheritanceInfo;
import OneLang.One.Transforms.FillMutabilityInfo.FillMutabilityInfo;
import OneLang.One.AstTransformer.AstTransformer;
import OneLang.One.ITransformer.ITransformer;
import OneLang.One.Transforms.LambdaCaptureCollector.LambdaCaptureCollector;

public interface ICompilerHooks {
    void afterStage(String stageName);
}