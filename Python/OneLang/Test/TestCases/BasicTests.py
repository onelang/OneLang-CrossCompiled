from onelang_core import *
import OneLang.Test.TestCase as testCase
import re
import json

class BasicTests:
    def __init__(self):
        self.name = "BasicTests"
    
    def prints_one_line_string(self):
        console.log("Hello World!")
    
    def prints_multi_line_string(self):
        console.log("Hello\nWorld!")
    
    def prints_two_strings(self):
        console.log("Hello")
        console.log("HelloWorld!")
    
    def prints_escaped_string(self):
        console.log("dollar: $")
        console.log("backslash: \\")
        console.log("newline: \n")
        console.log("escaped newline: \\n")
        console.log("dollar after escape: \\$")
    
    def prints_escaped_template_string(self):
        console.log(f'''dollar: $''')
        console.log(f'''backslash: \\''')
        console.log(f'''newline: \n''')
        console.log(f'''escaped newline: \\n''')
        console.log(f'''dollar after escape: \\$''')
    
    def regex_replace(self):
        console.log(re.sub("\\$", "x", "a$b$c"))
        console.log(re.sub("x", "$", "Test1: xx"))
        console.log(re.sub("y", "\\\\", "Test2: yy"))
        console.log(re.sub("z", "\\\\$", "Test3: zz"))
    
    def json(self):
        console.log(json.dumps(None, separators=(',', ':')))
        console.log(json.dumps(True, separators=(',', ':')))
        console.log(json.dumps(False, separators=(',', ':')))
        console.log(json.dumps("string", separators=(',', ':')))
        console.log(json.dumps(0.123, separators=(',', ':')))
        console.log(json.dumps(123, separators=(',', ':')))
        console.log(json.dumps(123.456, separators=(',', ':')))
        console.log(json.dumps({
            "a": "b"
        }, separators=(',', ':')))
        console.log(json.dumps("$", separators=(',', ':')))
        console.log(json.dumps("A \\ B", separators=(',', ':')))
        console.log(json.dumps("A \\\\ B", separators=(',', ':')))
    
    def php_generator_bugs(self):
        console.log("Step1: " + "A $ B")
        console.log("Step2: " + json.dumps("A $ B", separators=(',', ':')))
        console.log("Step3: " + re.sub("\\$", "\\\\$", json.dumps("A $ B", separators=(',', ':'))))
        console.log("Step3 w/o JSON: " + re.sub("\\$", "\\\\$", "A $ B"))
    
    def get_test_cases(self):
        return [testCase.SyncTestCase("PrintsOneLineString", lambda _: self.prints_one_line_string()), testCase.SyncTestCase("PrintsMultiLineString", lambda _: self.prints_multi_line_string()), testCase.SyncTestCase("PrintsTwoStrings", lambda _: self.prints_two_strings()), testCase.SyncTestCase("PrintsEscapedString", lambda _: self.prints_escaped_string()), testCase.SyncTestCase("PrintsEscapedTemplateString", lambda _: self.prints_escaped_template_string()), testCase.SyncTestCase("RegexReplace", lambda _: self.regex_replace()), testCase.SyncTestCase("Json", lambda _: self.json()), testCase.SyncTestCase("PhpGeneratorBugs", lambda _: self.php_generator_bugs())]