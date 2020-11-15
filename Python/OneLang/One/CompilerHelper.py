from OneLangStdLib import *
from OneFile import *
import OneLang.One.Compiler as comp
import OneLangStdLib as one

@one.static_init
class CompilerHelper:
    @classmethod
    def static_init(cls):
        cls.base_dir = "./"
    
    def __init__(self):
        pass
    
    @classmethod
    def init_project(cls, project_name, source_dir, lang = "ts", packages_dir = None):
        if lang != "ts":
            raise Error("Only typescript is supported.")
        
        compiler = comp.Compiler()
        compiler.init(packages_dir or f'''{CompilerHelper.base_dir}packages/''')
        compiler.setup_native_resolver(OneFile.read_text(f'''{CompilerHelper.base_dir}langs/NativeResolvers/typescript.ts'''))
        compiler.new_workspace(project_name)
        
        for file in list(filter(lambda x: x.endswith(".ts"), OneFile.list_files(source_dir, True))):
            compiler.add_project_file(file, OneFile.read_text(f'''{source_dir}/{file}'''))
        
        return compiler