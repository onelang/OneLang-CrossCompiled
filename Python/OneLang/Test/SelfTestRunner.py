from onelang_core import *
from onelang_file import *
import OneLang.index as index
import OneLang.One.CompilerHelper as compHelp
import OneLang.Generator.IGenerator as iGen
import OneLang.One.Compiler as comp
import OneLang.Test.PackageStateCapture as packStateCapt
import OneLang.Generator.ProjectGenerator as projGen

class CompilerHooks:
    def __init__(self, compiler, base_dir):
        self.stage = 0
        self.compiler = compiler
        self.base_dir = base_dir
    
    def after_stage(self, stage_name):
        state = packStateCapt.PackageStateCapture(self.compiler.project_pkg)
        stage_fn = f'''{self.base_dir}/test/artifacts/ProjectTest/OneLang/stages/{self.stage = self.stage + 1}_{stage_name}.txt'''
        stage_summary = state.get_summary()
        
        expected = index.OneFile.read_text(stage_fn)
        if stage_summary != expected:
            index.OneFile.write_text(stage_fn + "_diff.txt", stage_summary)
            raise Error(f'''Stage result differs from expected: {stage_name} -> {stage_fn}''')
        else:
            console.log(f'''[+] Stage passed: {stage_name}''')

class SelfTestRunner:
    def __init__(self, base_dir):
        self.base_dir = base_dir
        compHelp.CompilerHelper.base_dir = base_dir
    
    def run_test(self):
        console.log("[-] SelfTestRunner :: START")
        
        proj_gen = projGen.ProjectGenerator(self.base_dir, f'''{self.base_dir}/xcompiled-src''')
        proj_gen.out_dir = f'''{self.base_dir}test/artifacts/SelfTestRunner_{"Python"}/'''
        compiler = compHelp.CompilerHelper.init_project(proj_gen.project_file.name, proj_gen.src_dir, proj_gen.project_file.source_lang, None)
        compiler.hooks = CompilerHooks(compiler, self.base_dir)
        compiler.process_workspace()
        proj_gen.generate()
        
        all_match = True
        # for (const genFile of generated) {
        #     const projBase = `${this.baseDir}test/artifacts/ProjectTest/OneLang`;
        #     const tsGenPath = `${this.baseDir}/xcompiled/${langName}/${genFile.path}`;
        #     const reGenPath = `${projBase}/${langName}_Regen/${genFile.path}`;
        #     const tsGenContent = OneFile.readText(tsGenPath);
        #     const reGenContent = genFile.content;
        
        #     if (tsGenContent != reGenContent) {
        #         OneFile.writeText(reGenPath, genFile.content);
        #         console.error(`Content does not match: ${genFile.path}`);
        #         allMatch = false;
        #     } else {
        #         console.log(`[+] Content matches: ${genFile.path}`);
        #     }
        # }
        
        console.log("[+} SUCCESS! All generated files are the same" if all_match else "[!] FAIL! Not all files are the same")
        console.log("[-] SelfTestRunner :: DONE")
        return all_match