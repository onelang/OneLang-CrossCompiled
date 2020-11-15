package OneLang.Test.SelfTestRunner;

import OneStd.OneFile;
import OneLang.One.CompilerHelper.CompilerHelper;
import OneLang.Generator.IGenerator.IGenerator;
import OneLang.One.Compiler.Compiler;
import OneLang.One.Compiler.ICompilerHooks;
import OneLang.Test.PackageStateCapture.PackageStateCapture;

import OneStd.console;
import OneLang.Test.SelfTestRunner.CompilerHooks;
import OneStd.RegExp;
import java.util.regex.Pattern;
import OneStd.Objects;
import OneLang.Generator.IGenerator.IGenerator;

public class SelfTestRunner {
    public String baseDir;
    
    public SelfTestRunner(String baseDir)
    {
        this.baseDir = baseDir;
        CompilerHelper.baseDir = baseDir;
    }
    
    public Boolean runTest(IGenerator generator) {
        console.log("[-] SelfTestRunner :: START");
        var compiler = CompilerHelper.initProject("OneLang", this.baseDir + "src/", "ts", null);
        compiler.hooks = new CompilerHooks(compiler, this.baseDir);
        compiler.processWorkspace();
        var generated = generator.generate(compiler.projectPkg);
        
        var langName = generator.getLangName();
        var ext = "." + generator.getExtension();
        
        var allMatch = true;
        for (var genFile : generated) {
            var fn = genFile.path.replaceAll("\\.ts$", ext);
            var projBase = this.baseDir + "test/artifacts/ProjectTest/OneLang";
            var tsGenPath = this.baseDir + "/xcompiled/" + langName + "/" + fn;
            var reGenPath = projBase + "/" + langName + "_Regen/" + fn;
            var tsGenContent = OneFile.readText(tsGenPath);
            var reGenContent = genFile.content;
            
            if (!Objects.equals(tsGenContent, reGenContent)) {
                OneFile.writeText(reGenPath, genFile.content);
                console.error("Content does not match: " + genFile.path);
                allMatch = false;
            }
            else
                console.log("[+] Content matches: " + genFile.path);
        }
        
        console.log(allMatch ? "[+} SUCCESS! All generated files are the same" : "[!] FAIL! Not all files are the same");
        console.log("[-] SelfTestRunner :: DONE");
        return allMatch;
    }
}