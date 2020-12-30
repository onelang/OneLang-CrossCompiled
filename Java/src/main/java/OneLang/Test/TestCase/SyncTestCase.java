package OneLang.Test.TestCase;



import OneLang.Test.TestCase.TestCase;
import java.util.function.Consumer;

public class SyncTestCase extends TestCase {
    public Consumer<String> syncAction;
    
    public SyncTestCase(String name, Consumer<String> syncAction)
    {
        super(name, null);
        this.syncAction = syncAction;
        this.action = artifactDir -> this.execute(artifactDir);
    }
    
    public void execute(String artifactDir) {
        this.syncAction.accept(artifactDir);
    }
}