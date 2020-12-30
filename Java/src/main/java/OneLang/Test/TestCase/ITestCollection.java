package OneLang.Test.TestCase;



import OneLang.Test.TestCase.TestCase;

public interface ITestCollection {
    String getName();
    void setName(String value);
    
    TestCase[] getTestCases();
}