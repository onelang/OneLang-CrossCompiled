from onelang_core import *
from onelang_file import *
import OneLang.Test.TestCase as testCase

class OneFileTests:
    def __init__(self, base_dir):
        self.name = "OneFileTests"
        self.base_dir = base_dir
    
    def list_xcompiled_native_sources(self):
        console.log("\n".join(OneFile.list_files(f'''{self.base_dir}/xcompiled-src/native''', True)))
    
    def get_test_cases(self):
        return [testCase.SyncTestCase("ListXCompiledNativeSources", lambda _: self.list_xcompiled_native_sources())]