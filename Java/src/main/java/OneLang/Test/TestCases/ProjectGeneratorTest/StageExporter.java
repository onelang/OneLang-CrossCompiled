package OneLang.Test.TestCases.ProjectGeneratorTest;

import io.onelang.std.file.OneFile;
import OneLang.Test.TestCase.ITestCollection;
import OneLang.Test.TestCase.TestCase;
import OneLang.One.CompilerHelper.CompilerHelper;
import OneLang.One.Compiler.Compiler;
import OneLang.One.Compiler.ICompilerHooks;
import OneLang.Test.PackageStateCapture.PackageStateCapture;
import OneLang.Generator.ProjectGenerator.ProjectGenerator;

import OneLang.One.Compiler.ICompilerHooks;
import OneLang.One.Compiler.Compiler;
import OneLang.Test.PackageStateCapture.PackageStateCapture;

public class StageExporter implements ICompilerHooks {
    public Integer stage = 0;
    public String artifactDir;
    public Compiler compiler;
    
    public StageExporter(String artifactDir, Compiler compiler)
    {
        this.artifactDir = artifactDir;
        this.compiler = compiler;
    }
    
    public void afterStage(String stageName) {
        System.out.println("Stage finished: " + stageName);
        OneFile.writeText(this.artifactDir + "/stages/" + this.stage + "_" + stageName + ".txt", new PackageStateCapture(this.compiler.projectPkg).getSummary());
        this.stage++;
    }
}