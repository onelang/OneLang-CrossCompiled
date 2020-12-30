package OneLang.Test.TestCase;



import java.util.function.Consumer;

public class TestCase {
    public String name;
    public Consumer<String> action;
    
    public TestCase(String name, Consumer<String> action)
    {
        this.name = name;
        this.action = action;
    }
}