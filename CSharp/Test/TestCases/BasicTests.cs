using System.Collections.Generic;
using Test;

namespace Test.TestCases
{
    public class BasicTests : ITestCollection
    {
        public string name { get; set; } = "BasicTests";
        
        public void printsOneLineString()
        {
            console.log("Hello World!");
        }
        
        public void printsMultiLineString()
        {
            console.log("Hello\nWorld!");
        }
        
        public void printsTwoStrings()
        {
            console.log("Hello");
            console.log("HelloWorld!");
        }
        
        public void printsEscapedString()
        {
            console.log("dollar: $");
            console.log("backslash: \\");
            console.log("newline: \n");
            console.log("escaped newline: \\n");
            console.log("dollar after escape: \\$");
        }
        
        public void printsEscapedTemplateString()
        {
            console.log($"dollar: $");
            console.log($"backslash: \\");
            console.log($"newline: \n");
            console.log($"escaped newline: \\n");
            console.log($"dollar after escape: \\$");
        }
        
        public void regexReplace()
        {
            console.log("a$b$c".replace(new RegExp("\\$"), "x"));
            console.log("Test1: xx".replace(new RegExp("x"), "$"));
            console.log("Test2: yy".replace(new RegExp("y"), "\\"));
            console.log("Test3: zz".replace(new RegExp("z"), "\\$"));
        }
        
        public void json()
        {
            console.log(JSON.stringify(null));
            console.log(JSON.stringify(true));
            console.log(JSON.stringify(false));
            console.log(JSON.stringify("string"));
            console.log(JSON.stringify(0.123));
            console.log(JSON.stringify(123));
            console.log(JSON.stringify(123.456));
            console.log(JSON.stringify(new Dictionary<string, string> { ["a"] = "b" }));
            console.log(JSON.stringify("$"));
            console.log(JSON.stringify("A \\ B"));
            console.log(JSON.stringify("A \\\\ B"));
        }
        
        public void phpGeneratorBugs()
        {
            console.log("Step1: " + "A $ B");
            console.log("Step2: " + JSON.stringify("A $ B"));
            console.log("Step3: " + JSON.stringify("A $ B").replace(new RegExp("\\$"), "\\$"));
            console.log("Step3 w/o JSON: " + "A $ B".replace(new RegExp("\\$"), "\\$"));
        }
        
        public TestCase[] getTestCases()
        {
            return new SyncTestCase[] { new SyncTestCase("PrintsOneLineString", _ => this.printsOneLineString()), new SyncTestCase("PrintsMultiLineString", _ => this.printsMultiLineString()), new SyncTestCase("PrintsTwoStrings", _ => this.printsTwoStrings()), new SyncTestCase("PrintsEscapedString", _ => this.printsEscapedString()), new SyncTestCase("PrintsEscapedTemplateString", _ => this.printsEscapedTemplateString()), new SyncTestCase("RegexReplace", _ => this.regexReplace()), new SyncTestCase("Json", _ => this.json()), new SyncTestCase("PhpGeneratorBugs", _ => this.phpGeneratorBugs()) };
        }
    }
}