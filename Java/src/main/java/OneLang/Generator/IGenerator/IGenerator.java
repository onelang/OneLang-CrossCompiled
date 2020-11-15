package OneLang.Generator.IGenerator;

import OneLang.Generator.GeneratedFile.GeneratedFile;
import OneLang.One.Ast.Types.Package;
import OneLang.One.ITransformer.ITransformer;

import OneLang.One.ITransformer.ITransformer;
import OneLang.Generator.GeneratedFile.GeneratedFile;
import OneLang.One.Ast.Types.Package;

public interface IGenerator {
    String getLangName();
    String getExtension();
    ITransformer[] getTransforms();
    GeneratedFile[] generate(Package pkg);
}