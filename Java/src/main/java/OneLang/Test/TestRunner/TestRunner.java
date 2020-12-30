package OneLang.Test.TestRunner;

import io.onelang.std.core.One;
import OneLang.One.CompilerHelper.CompilerHelper;
import OneLang.Test.TestCase.ITestCollection;
import OneLang.Test.TestCase.TestCase;
import OneLang.Test.TestCases.BasicTests.BasicTests;
import OneLang.Test.TestCases.OneFileTests.OneFileTests;
import OneLang.Test.TestCases.ProjectGeneratorTest.ProjectGeneratorTest;

import java.util.List;
import OneLang.Test.TestCase.ITestCollection;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import OneLang.Test.TestCases.BasicTests.BasicTests;
import OneLang.Test.TestCases.OneFileTests.OneFileTests;
import OneLang.Test.TestCases.ProjectGeneratorTest.ProjectGeneratorTest;
import io.onelang.std.core.ExceptionHelper;

public class TestRunner {
    public List<ITestCollection> tests;
    public Map<String, String> argsDict;
    public String outputDir;
    public String baseDir;
    public String[] args;
    
    public TestRunner(String baseDir, String[] args)
    {
        this.baseDir = baseDir;
        this.args = args;
        this.tests = new ArrayList<ITestCollection>();
        this.argsDict = new LinkedHashMap<String, String>();
        CompilerHelper.baseDir = baseDir + "/";
        this.tests.add(new BasicTests());
        this.tests.add(new OneFileTests(this.baseDir));
        this.tests.add(new ProjectGeneratorTest(this.baseDir));
        
        this.parseArgs();
        var getResult = this.argsDict.get("output-dir");
        this.outputDir = (getResult != null ? (getResult) : (baseDir + "/test/artifacts/TestRunner/" + "Java"));
    }
    
    public void parseArgs() {
        for (Integer i = 0; i < this.args.length - 1; i++) {
            if (this.args[i].startsWith("--"))
                this.argsDict.put(this.args[i].substring(2), this.args[i + 1]);
        }
    }
    
    public void runTests() {
        System.out.println("### TestRunner -> START ###");
        
        for (var coll : this.tests) {
            System.out.println("### TestCollection -> " + coll.getName() + " -> START ###");
            for (var test : coll.getTestCases()) {
                System.out.println("### TestCase -> " + test.name + " -> START ###");
                try {
                    var outputDir = this.outputDir + "/" + coll.getName() + "/" + test.name + "/";
                    System.out.println("### TestCase -> " + test.name + " -> OUTPUT-DIR -> " + outputDir + " ###");
                    test.action.accept(outputDir);
                } catch (Exception e)  {
                    System.out.println("### TestCase -> " + test.name + " -> ERROR -> " + ExceptionHelper.toString(e) + " ###");
                }
                System.out.println("### TestCase -> " + test.name + " -> END ###");
            }
            System.out.println("### TestCollection -> " + coll.getName() + " -> END ###");
        }
        
        System.out.println("### TestRunner -> END ###");
    }
}