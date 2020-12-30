package OneLang.Test.TestCases.OneFileTests;

import io.onelang.std.file.OneFile;
import OneLang.Test.TestCase.ITestCollection;
import OneLang.Test.TestCase.SyncTestCase;
import OneLang.Test.TestCase.TestCase;

import OneLang.Test.TestCase.ITestCollection;
import java.util.stream.Collectors;
import java.util.Arrays;
import OneLang.Test.TestCase.TestCase;
import OneLang.Test.TestCase.SyncTestCase;

public class OneFileTests implements ITestCollection {
    public String baseDir;
    
    String name = "OneFileTests";
    public String getName() { return this.name; }
    public void setName(String value) { this.name = value; }
    
    public OneFileTests(String baseDir)
    {
        this.baseDir = baseDir;
    }
    
    public void listXCompiledNativeSources() {
        System.out.println(Arrays.stream(OneFile.listFiles(this.baseDir + "/xcompiled-src/native", true)).collect(Collectors.joining("\n")));
    }
    
    public TestCase[] getTestCases() {
        return new SyncTestCase[] { new SyncTestCase("ListXCompiledNativeSources", unused -> this.listXCompiledNativeSources()) };
    }
}