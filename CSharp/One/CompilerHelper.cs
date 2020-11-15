using One;
using System.Threading.Tasks;

namespace One
{
    public class CompilerHelper {
        public static string baseDir = "./";
        
        public static async Task<Compiler> initProject(string projectName, string sourceDir, string lang = "ts", string packagesDir = null)
        {
            if (lang != "ts")
                throw new Error("Only typescript is supported.");
            
            var compiler = new Compiler();
            await compiler.init(packagesDir ?? $"{CompilerHelper.baseDir}packages/");
            compiler.setupNativeResolver(OneFile.readText($"{CompilerHelper.baseDir}langs/NativeResolvers/typescript.ts"));
            compiler.newWorkspace(projectName);
            
            foreach (var file in OneFile.listFiles(sourceDir, true).filter(x => x.endsWith(".ts")))
                compiler.addProjectFile(file, OneFile.readText($"{sourceDir}/{file}"));
            
            return compiler;
        }
    }
}