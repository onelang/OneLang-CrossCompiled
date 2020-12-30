using Test;

namespace Test.TestCases
{
    public class OneFileTests : ITestCollection
    {
        public string name { get; set; } = "OneFileTests";
        public string baseDir;
        
        public OneFileTests(string baseDir)
        {
            this.baseDir = baseDir;
        }
        
        public void listXCompiledNativeSources()
        {
            console.log(OneFile.listFiles($"{this.baseDir}/xcompiled-src/native", true).join("\n"));
        }
        
        public TestCase[] getTestCases()
        {
            return new SyncTestCase[] { new SyncTestCase("ListXCompiledNativeSources", _ => this.listXCompiledNativeSources()) };
        }
    }
}