package OneLang.Test.TestRunner;

import io.onelang.std.core.One;
import io.onelang.std.file.OneFile;
import io.onelang.std.json.OneJson;
import OneLang.One.CompilerHelper.CompilerHelper;
import OneLang.Test.TestCase.ITestCollection;
import OneLang.Test.TestCase.TestCase;
import OneLang.Test.TestCases.BasicTests.BasicTests;
import OneLang.Test.TestCases.ProjectGeneratorTest.ProjectGeneratorTest;

public class TestResult {
    public String collectionName;
    public String testName;
    public Object result;
    public String error;
    
    public TestResult(String collectionName, String testName, Object result, String error)
    {
        this.collectionName = collectionName;
        this.testName = testName;
        this.result = result;
        this.error = error;
    }
}