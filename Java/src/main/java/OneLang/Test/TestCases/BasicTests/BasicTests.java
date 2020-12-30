package OneLang.Test.TestCases.BasicTests;

import OneLang.Test.TestCase.ITestCollection;
import OneLang.Test.TestCase.SyncTestCase;
import OneLang.Test.TestCase.TestCase;

import OneLang.Test.TestCase.ITestCollection;
import java.util.regex.Pattern;
import io.onelang.std.json.JSON;
import java.util.Map;
import java.util.LinkedHashMap;
import OneLang.Test.TestCase.TestCase;
import OneLang.Test.TestCase.SyncTestCase;

public class BasicTests implements ITestCollection {
    String name = "BasicTests";
    public String getName() { return this.name; }
    public void setName(String value) { this.name = value; }
    
    public void printsOneLineString() {
        System.out.println("Hello World!");
    }
    
    public void printsMultiLineString() {
        System.out.println("Hello\nWorld!");
    }
    
    public void printsTwoStrings() {
        System.out.println("Hello");
        System.out.println("HelloWorld!");
    }
    
    public void printsEscapedString() {
        System.out.println("dollar: $");
        System.out.println("backslash: \\");
        System.out.println("newline: \n");
        System.out.println("escaped newline: \\n");
        System.out.println("dollar after escape: \\$");
    }
    
    public void printsEscapedTemplateString() {
        System.out.println("dollar: $");
        System.out.println("backslash: \\");
        System.out.println("newline: \n");
        System.out.println("escaped newline: \\n");
        System.out.println("dollar after escape: \\$");
    }
    
    public void regexReplace() {
        System.out.println("a$b$c".replaceAll("\\$", "x"));
        System.out.println("Test1: xx".replaceAll("x", "\\$"));
        System.out.println("Test2: yy".replaceAll("y", "\\\\"));
        System.out.println("Test3: zz".replaceAll("z", "\\\\\\$"));
    }
    
    public void json() {
        System.out.println(JSON.stringify(null));
        System.out.println(JSON.stringify(true));
        System.out.println(JSON.stringify(false));
        System.out.println(JSON.stringify("string"));
        System.out.println(JSON.stringify(0.123));
        System.out.println(JSON.stringify(123));
        System.out.println(JSON.stringify(123.456));
        System.out.println(JSON.stringify(new LinkedHashMap<>(Map.of("a", "b"))));
        System.out.println(JSON.stringify("$"));
        System.out.println(JSON.stringify("A \\ B"));
        System.out.println(JSON.stringify("A \\\\ B"));
    }
    
    public void phpGeneratorBugs() {
        System.out.println("Step1: " + "A $ B");
        System.out.println("Step2: " + JSON.stringify("A $ B"));
        System.out.println("Step3: " + JSON.stringify("A $ B").replaceAll("\\$", "\\\\\\$"));
        System.out.println("Step3 w/o JSON: " + "A $ B".replaceAll("\\$", "\\\\\\$"));
    }
    
    public TestCase[] getTestCases() {
        return new SyncTestCase[] { new SyncTestCase("PrintsOneLineString", unused -> this.printsOneLineString()), new SyncTestCase("PrintsMultiLineString", unused -> this.printsMultiLineString()), new SyncTestCase("PrintsTwoStrings", unused -> this.printsTwoStrings()), new SyncTestCase("PrintsEscapedString", unused -> this.printsEscapedString()), new SyncTestCase("PrintsEscapedTemplateString", unused -> this.printsEscapedTemplateString()), new SyncTestCase("RegexReplace", unused -> this.regexReplace()), new SyncTestCase("Json", unused -> this.json()), new SyncTestCase("PhpGeneratorBugs", unused -> this.phpGeneratorBugs()) };
    }
}