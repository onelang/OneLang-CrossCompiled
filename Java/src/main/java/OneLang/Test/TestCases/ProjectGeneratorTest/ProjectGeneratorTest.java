package OneLang.Test.TestCases.ProjectGeneratorTest;

import io.onelang.std.file.OneFile;
import OneLang.Test.TestCase.ITestCollection;
import OneLang.Test.TestCase.TestCase;
import OneLang.One.CompilerHelper.CompilerHelper;
import OneLang.One.Compiler.Compiler;
import OneLang.One.Compiler.ICompilerHooks;
import OneLang.Test.PackageStateCapture.PackageStateCapture;
import OneLang.Generator.ProjectGenerator.ProjectGenerator;

import OneLang.Test.TestCase.ITestCollection;
import OneLang.Test.TestCase.TestCase;
import OneLang.Generator.ProjectGenerator.ProjectGenerator;
import OneLang.Test.TestCases.ProjectGeneratorTest.StageExporter;

public class ProjectGeneratorTest implements ITestCollection {
    public String baseDir;
    
    String name = "ProjectGeneratorTest";
    public String getName() { return this.name; }
    public void setName(String value) { this.name = value; }
    
    public ProjectGeneratorTest(String baseDir)
    {
        this.baseDir = baseDir;
    }
    
    public TestCase[] getTestCases() {
        return new TestCase[] { new TestCase("OneLang", artifactDir -> this.compileOneLang(artifactDir)) };
    }
    
    public void compileOneLang(String artifactDir) {
        System.out.println("Initalizing project generator...");
        var projGen = new ProjectGenerator(this.baseDir, this.baseDir + "/xcompiled-src");
        projGen.outDir = artifactDir + "/output/";
        
        System.out.println("Initalizing project for compiler...");
        var compiler = CompilerHelper.initProject(projGen.projectFile.name, projGen.srcDir, projGen.projectFile.sourceLang, null);
        compiler.hooks = new StageExporter(artifactDir, compiler);
        
        System.out.println("Processing workspace...");
        compiler.processWorkspace();
        
        System.out.println("Generating project...");
        projGen.generate();
    }
}