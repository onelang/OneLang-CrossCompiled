package OneLang.Generator.IGenerator;

import OneLang.Generator.GeneratedFile.GeneratedFile;
import OneLang.One.Ast.Types.Package;
import OneLang.One.ITransformer.ITransformer;
import OneLang.Generator.IGeneratorPlugin.IGeneratorPlugin;
import OneLang.One.Ast.Interfaces.IExpression;

import OneLang.One.ITransformer.ITransformer;
import OneLang.Generator.IGeneratorPlugin.IGeneratorPlugin;
import OneLang.One.Ast.Interfaces.IExpression;
import OneLang.Generator.GeneratedFile.GeneratedFile;
import OneLang.One.Ast.Types.Package;

public interface IGenerator {
    String getLangName();
    String getExtension();
    ITransformer[] getTransforms();
    void addPlugin(IGeneratorPlugin plugin);
    void addInclude(String include);
    String expr(IExpression expr);
    GeneratedFile[] generate(Package pkg);
}