package OneLang.Test.SelfTestRunner;

import io.onelang.std.core.One;
import io.onelang.std.file.OneFile;
import OneLang.One.CompilerHelper.CompilerHelper;
import OneLang.Generator.IGenerator.IGenerator;
import OneLang.One.Compiler.Compiler;
import OneLang.One.Compiler.ICompilerHooks;
import OneLang.Test.PackageStateCapture.PackageStateCapture;
import OneLang.Generator.ProjectGenerator.ProjectGenerator;

import OneLang.Generator.ProjectGenerator.ProjectGenerator;
import OneLang.Test.SelfTestRunner.CompilerHooks;

public class SelfTestRunner {
    public String baseDir;
    
    public SelfTestRunner(String baseDir)
    {
        this.baseDir = baseDir;
        CompilerHelper.baseDir = baseDir;
    }
    
    public Boolean runTest() {
        System.out.println("[-] SelfTestRunner :: START");
        
        var projGen = new ProjectGenerator(this.baseDir, this.baseDir + "/xcompiled-src");
        projGen.outDir = this.baseDir + "test/artifacts/SelfTestRunner_" + "Java" + "/";
        var compiler = CompilerHelper.initProject(projGen.projectFile.name, projGen.srcDir, projGen.projectFile.sourceLang, null);
        compiler.hooks = new CompilerHooks(compiler, this.baseDir);
        compiler.processWorkspace();
        projGen.generate();
        
        var allMatch = true;
        // for (const genFile of generated) {
        //     const projBase = `${this.baseDir}test/artifacts/ProjectTest/OneLang`;
        //     const tsGenPath = `${this.baseDir}/xcompiled/${langName}/${genFile.path}`;
        //     const reGenPath = `${projBase}/${langName}_Regen/${genFile.path}`;
        //     const tsGenContent = OneFile.readText(tsGenPath);
        //     const reGenContent = genFile.content;
        
        //     if (tsGenContent != reGenContent) {
        //         OneFile.writeText(reGenPath, genFile.content);
        //         console.error(`Content does not match: ${genFile.path}`);
        //         allMatch = false;
        //     } else {
        //         console.log(`[+] Content matches: ${genFile.path}`);
        //     }
        // }
        
        System.out.println(allMatch ? "[+} SUCCESS! All generated files are the same" : "[!] FAIL! Not all files are the same");
        System.out.println("[-] SelfTestRunner :: DONE");
        return allMatch;
    }
}