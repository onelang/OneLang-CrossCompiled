from onelang_core import *
from onelang_file import *
import OneLang.Test.TestCase as testCase
import OneLang.One.CompilerHelper as compHelp
import OneLang.One.Compiler as comp
import OneLang.Test.PackageStateCapture as packStateCapt
import OneLang.Generator.ProjectGenerator as projGen

class StageExporter:
    def __init__(self, artifact_dir, compiler):
        self.stage = 0
        self.artifact_dir = artifact_dir
        self.compiler = compiler
    
    def after_stage(self, stage_name):
        console.log(f'''Stage finished: {stage_name}''')
        OneFile.write_text(f'''{self.artifact_dir}/stages/{self.stage}_{stage_name}.txt''', packStateCapt.PackageStateCapture(self.compiler.project_pkg).get_summary())
        self.stage = self.stage + 1

class ProjectGeneratorTest:
    def __init__(self, base_dir):
        self.name = "ProjectGeneratorTest"
        self.base_dir = base_dir
    
    def get_test_cases(self):
        return [testCase.TestCase("OneLang", lambda artifact_dir: self.compile_one_lang(artifact_dir))]
    
    def compile_one_lang(self, artifact_dir):
        console.log("Initalizing project generator...")
        proj_gen = projGen.ProjectGenerator(self.base_dir, f'''{self.base_dir}/xcompiled-src''')
        proj_gen.out_dir = f'''{artifact_dir}/output/'''
        
        console.log("Initalizing project for compiler...")
        compiler = compHelp.CompilerHelper.init_project(proj_gen.project_file.name, proj_gen.src_dir, proj_gen.project_file.source_lang, None)
        compiler.hooks = StageExporter(artifact_dir, compiler)
        
        console.log("Processing workspace...")
        compiler.process_workspace()
        
        console.log("Generating project...")
        proj_gen.generate()