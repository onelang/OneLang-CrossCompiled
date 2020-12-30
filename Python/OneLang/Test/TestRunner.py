from onelang_core import *
import OneLang.One.CompilerHelper as compHelp
import OneLang.Test.TestCase as testCase
import OneLang.Test.TestCases.BasicTests as basicTests
import OneLang.Test.TestCases.OneFileTests as oneFileTests
import OneLang.Test.TestCases.ProjectGeneratorTest as projGenTest

class TestRunner:
    def __init__(self, base_dir, args):
        self.tests = []
        self.args_dict = {}
        self.output_dir = None
        self.base_dir = base_dir
        self.args = args
        compHelp.CompilerHelper.base_dir = f'''{base_dir}/'''
        self.tests.append(basicTests.BasicTests())
        self.tests.append(oneFileTests.OneFileTests(self.base_dir))
        self.tests.append(projGenTest.ProjectGeneratorTest(self.base_dir))
        
        self.parse_args()
        self.output_dir = self.args_dict.get("output-dir") or f'''{base_dir}/test/artifacts/TestRunner/{"Python"}'''
    
    def parse_args(self):
        i = 0
        
        while i < len(self.args) - 1:
            if self.args[i].startswith("--"):
                self.args_dict[self.args[i][2:]] = self.args[i + 1]
            i = i + 1
    
    def run_tests(self):
        console.log(f'''### TestRunner -> START ###''')
        
        for coll in self.tests:
            console.log(f'''### TestCollection -> {coll.name} -> START ###''')
            for test in coll.get_test_cases():
                console.log(f'''### TestCase -> {test.name} -> START ###''')
                try:
                    output_dir = f'''{self.output_dir}/{coll.name}/{test.name}/'''
                    console.log(f'''### TestCase -> {test.name} -> OUTPUT-DIR -> {output_dir} ###''')
                    test.action(output_dir)
                except Exception as e:
                    console.log(f'''### TestCase -> {test.name} -> ERROR -> {e.msg} ###''')
                console.log(f'''### TestCase -> {test.name} -> END ###''')
            console.log(f'''### TestCollection -> {coll.name} -> END ###''')
        
        console.log(f'''### TestRunner -> END ###''')