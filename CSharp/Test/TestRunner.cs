using One;
using System.Collections.Generic;
using System.Threading.Tasks;
using System;
using Test.TestCases;
using Test;

namespace Test
{
    public class TestRunner
    {
        public List<ITestCollection> tests;
        public Dictionary<string, string> argsDict;
        public string outputDir;
        public string baseDir;
        public string[] args;
        
        public TestRunner(string baseDir, string[] args)
        {
            this.baseDir = baseDir;
            this.args = args;
            this.tests = new List<ITestCollection>();
            this.argsDict = new Dictionary<string, string> {};
            CompilerHelper.baseDir = $"{baseDir}/";
            this.tests.push(new BasicTests());
            this.tests.push(new OneFileTests(this.baseDir));
            this.tests.push(new ProjectGeneratorTest(this.baseDir));
            
            this.parseArgs();
            this.outputDir = this.argsDict.get("output-dir") ?? $"{baseDir}/test/artifacts/TestRunner/{"CSharp"}";
        }
        
        public void parseArgs()
        {
            for (int i = 0; i < this.args.length() - 1; i++) {
                if (this.args.get(i).startsWith("--"))
                    this.argsDict.set(this.args.get(i).substr(2), this.args.get(i + 1));
            }
        }
        
        public async Task runTests()
        {
            console.log($"### TestRunner -> START ###");
            
            foreach (var coll in this.tests) {
                console.log($"### TestCollection -> {coll.name} -> START ###");
                foreach (var test in coll.getTestCases()) {
                    console.log($"### TestCase -> {test.name} -> START ###");
                    try {
                        var outputDir = $"{this.outputDir}/{coll.name}/{test.name}/";
                        console.log($"### TestCase -> {test.name} -> OUTPUT-DIR -> {outputDir} ###");
                        await test.action(outputDir);
                    } catch (Exception e)  {
                        console.log($"### TestCase -> {test.name} -> ERROR -> {e.ToString()} ###");
                    }
                    console.log($"### TestCase -> {test.name} -> END ###");
                }
                console.log($"### TestCollection -> {coll.name} -> END ###");
            }
            
            console.log($"### TestRunner -> END ###");
        }
    }
}