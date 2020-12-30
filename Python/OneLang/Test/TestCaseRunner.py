from onelang_core import *
import OneLang.index as index
import OneLang.Test.TestCase as testCase
import OneLang.Test.TestCases.BasicTests as basicTests
import OneLang.Test.TestCases.ProjectGeneratorTest as projGenTest
json

class TestResult:
    def __init__(self, collection_name, test_name, result, error):
        self.collection_name = collection_name
        self.test_name = test_name
        self.result = result
        self.error = error

class TestCaseRunner:
    def __init__(self, base_dir):
        self.tests = []
        self.base_dir = base_dir
        self.tests.append(basicTests.BasicTests())
        self.tests.append(projGenTest.ProjectGeneratorTest(self.base_dir))
    
    def run_tests(self):
        results = []
        console.log(f'''### TestCaseRunner -> START ###''')
        
        for coll in self.tests:
            console.log(f'''### Collection -> {coll.name} -> START ###''')
            for test in coll.get_test_cases():
                console.log(f'''### TestCase -> {test.name} -> START ###''')
                try:
                    artifact_dir = f'''{self.base_dir}/test/artifacts/{coll.name}/{test.name}/{"Python"}/'''
                    result = test.action(artifact_dir)
                    console.log(f'''### TestCase -> {test.name} -> RESULT: {json.dumps(result)} ###''')
                    results.append(TestResult(coll.name, test.name, result, None))
                except Exception as e:
                    console.log(f'''### TestCase -> {test.name} -> ERROR: {e} ###''')
                    results.append(TestResult(coll.name, test.name, None, (e).message))
                console.log(f'''### TestCase -> {test.name} -> END ###''')
            console.log(f'''### Collection -> {coll.name} -> END ###''')
        
        console.log(f'''### TestCaseRunner -> SAVE ###''')
        index.OneFile.write_text(f'''{self.base_dir}/test/artifacts/TestCaseRunner_results.json''', json.dumps(results))
        
        console.log(f'''### TestCaseRunner -> END ###''')
        return results