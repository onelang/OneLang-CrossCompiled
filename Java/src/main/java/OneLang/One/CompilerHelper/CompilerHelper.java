package OneLang.One.CompilerHelper;

import OneStd.OneFile;
import OneLang.One.Compiler.Compiler;

import OneLang.One.Compiler.Compiler;
import OneStd.Objects;
import java.util.Arrays;

public class CompilerHelper {
    public static String baseDir = "./";
    
    public static Compiler initProject(String projectName, String sourceDir, String lang, String packagesDir) {
        if (!Objects.equals(lang, "ts"))
            throw new Error("Only typescript is supported.");
        
        var compiler = new Compiler();
        var result = packagesDir;
        compiler.init(result != null ? result : CompilerHelper.baseDir + "packages/");
        compiler.setupNativeResolver(OneFile.readText(CompilerHelper.baseDir + "langs/NativeResolvers/typescript.ts"));
        compiler.newWorkspace(projectName);
        
        for (var file : Arrays.stream(OneFile.listFiles(sourceDir, true)).filter(x -> x.endsWith(".ts")).toArray(String[]::new))
            compiler.addProjectFile(file, OneFile.readText(sourceDir + "/" + file));
        
        return compiler;
    }
}