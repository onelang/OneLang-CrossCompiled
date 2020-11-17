using Generator;
using One;
using System.Threading.Tasks;
using Test;

namespace Test
{
    public class CompilerHooks : ICompilerHooks {
        public int stage = 0;
        public Compiler compiler;
        public string baseDir;
        
        public CompilerHooks(Compiler compiler, string baseDir)
        {
            this.compiler = compiler;
            this.baseDir = baseDir;
        }
        
        public void afterStage(string stageName)
        {
            var state = new PackageStateCapture(this.compiler.projectPkg);
            var stageFn = $"{this.baseDir}/test/artifacts/ProjectTest/OneLang/stages/{this.stage}_{stageName}.txt";
            this.stage++;
            var stageSummary = state.getSummary();
            var expected = OneFile.readText(stageFn);
            if (stageSummary != expected) {
                OneFile.writeText(stageFn + "_diff.txt", stageSummary);
                throw new Error($"Stage result differs from expected: {stageName} -> {stageFn}");
            }
            else
                console.log($"[+] Stage passed: {stageName}");
        }
    }
    
    public class SelfTestRunner {
        public string baseDir;
        
        public SelfTestRunner(string baseDir)
        {
            this.baseDir = baseDir;
            CompilerHelper.baseDir = baseDir;
        }
        
        public async Task<bool> runTest(IGenerator generator)
        {
            console.log("[-] SelfTestRunner :: START");
            var compiler = await CompilerHelper.initProject("OneLang", $"{this.baseDir}src/");
            compiler.hooks = new CompilerHooks(compiler, this.baseDir);
            compiler.processWorkspace();
            var generated = generator.generate(compiler.projectPkg);
            
            var langName = generator.getLangName();
            var ext = $".{generator.getExtension()}";
            
            var allMatch = true;
            foreach (var genFile in generated) {
                var projBase = $"{this.baseDir}test/artifacts/ProjectTest/OneLang";
                var tsGenPath = $"{this.baseDir}/xcompiled/{langName}/{genFile.path}";
                var reGenPath = $"{projBase}/{langName}_Regen/{genFile.path}";
                var tsGenContent = OneFile.readText(tsGenPath);
                var reGenContent = genFile.content;
                
                if (tsGenContent != reGenContent) {
                    OneFile.writeText(reGenPath, genFile.content);
                    console.error($"Content does not match: {genFile.path}");
                    allMatch = false;
                }
                else
                    console.log($"[+] Content matches: {genFile.path}");
            }
            
            console.log(allMatch ? "[+} SUCCESS! All generated files are the same" : "[!] FAIL! Not all files are the same");
            console.log("[-] SelfTestRunner :: DONE");
            return allMatch;
        }
    }
}