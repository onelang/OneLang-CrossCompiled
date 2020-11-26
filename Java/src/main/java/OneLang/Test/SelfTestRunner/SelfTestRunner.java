package OneLang.Test.SelfTestRunner;

import io.onelang.std.file.OneFile;
import OneLang.One.CompilerHelper.CompilerHelper;
import OneLang.Generator.IGenerator.IGenerator;
import OneLang.One.Compiler.Compiler;
import OneLang.One.Compiler.ICompilerHooks;
import OneLang.Test.PackageStateCapture.PackageStateCapture;

import OneLang.Test.SelfTestRunner.CompilerHooks;
import io.onelang.std.core.Objects;
import OneLang.Generator.IGenerator.IGenerator;

public class SelfTestRunner {
    public String baseDir;
    
    public SelfTestRunner(String baseDir)
    {
        this.baseDir = baseDir;
        CompilerHelper.baseDir = baseDir;
    }
    
    public Boolean runTest(IGenerator generator) {
        System.out.println("[-] SelfTestRunner :: START");
        var compiler = CompilerHelper.initProject("OneLang", this.baseDir + "src/", "ts", null);
        compiler.hooks = new CompilerHooks(compiler, this.baseDir);
        compiler.processWorkspace();
        var generated = generator.generate(compiler.projectPkg);
        
        var langName = generator.getLangName();
        var ext = "." + generator.getExtension();
        
        var allMatch = true;
        for (var genFile : generated) {
            var projBase = this.baseDir + "test/artifacts/ProjectTest/OneLang";
            var tsGenPath = this.baseDir + "/xcompiled/" + langName + "/" + genFile.path;
            var reGenPath = projBase + "/" + langName + "_Regen/" + genFile.path;
            var tsGenContent = OneFile.readText(tsGenPath);
            var reGenContent = genFile.content;
            
            if (!Objects.equals(tsGenContent, reGenContent)) {
                OneFile.writeText(reGenPath, genFile.content);
                System.err.println("Content does not match: " + genFile.path);
                allMatch = false;
            }
            else
                System.out.println("[+] Content matches: " + genFile.path);
        }
        
        System.out.println(allMatch ? "[+} SUCCESS! All generated files are the same" : "[!] FAIL! Not all files are the same");
        System.out.println("[-] SelfTestRunner :: DONE");
        return allMatch;
    }
}