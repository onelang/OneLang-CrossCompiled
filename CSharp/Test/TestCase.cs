using System.Threading.Tasks;
using System;

namespace Test
{
    public interface ITestCollection {
        string name { get; set; }
        
        TestCase[] getTestCases();
    }
    
    public class TestCase
    {
        public string name;
        public Func<string, Task> action;
        
        public TestCase(string name, Func<string, Task> action)
        {
            this.name = name;
            this.action = action;
        }
    }
    
    public class SyncTestCase : TestCase
    {
        public Action<string> syncAction;
        
        public SyncTestCase(string name, Action<string> syncAction): base(name, null)
        {
            this.syncAction = syncAction;
            this.action = artifactDir => this.execute(artifactDir);
        }
        
        public async Task execute(string artifactDir)
        {
            this.syncAction(artifactDir);
        }
    }
}