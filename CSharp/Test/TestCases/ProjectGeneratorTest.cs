using Generator;
using One;
using System.Threading.Tasks;
using Test;

namespace Test.TestCases
{
    public class StageExporter : ICompilerHooks
    {
        public int stage = 0;
        public string artifactDir;
        public Compiler compiler;
        
        public StageExporter(string artifactDir, Compiler compiler)
        {
            this.artifactDir = artifactDir;
            this.compiler = compiler;
        }
        
        public void afterStage(string stageName)
        {
            console.log($"Stage finished: {stageName}");
            OneFile.writeText($"{this.artifactDir}/stages/{this.stage}_{stageName}.txt", new PackageStateCapture(this.compiler.projectPkg).getSummary());
            this.stage++;
        }
    }
    
    public class ProjectGeneratorTest : ITestCollection
    {
        public string name { get; set; } = "ProjectGeneratorTest";
        public string baseDir;
        
        public ProjectGeneratorTest(string baseDir)
        {
            this.baseDir = baseDir;
        }
        
        public TestCase[] getTestCases()
        {
            return new TestCase[] { new TestCase("OneLang", artifactDir => this.compileOneLang(artifactDir)) };
        }
        
        public async Task compileOneLang(string artifactDir)
        {
            console.log("Initalizing project generator...");
            var projGen = new ProjectGenerator(this.baseDir, $"{this.baseDir}/xcompiled-src");
            projGen.outDir = $"{artifactDir}/output/";
            
            console.log("Initalizing project for compiler...");
            var compiler = await CompilerHelper.initProject(projGen.projectFile.name, projGen.srcDir, projGen.projectFile.sourceLang, null);
            compiler.hooks = new StageExporter(artifactDir, compiler);
            
            console.log("Processing workspace...");
            compiler.processWorkspace();
            
            console.log("Generating project...");
            await projGen.generate();
        }
    }
}