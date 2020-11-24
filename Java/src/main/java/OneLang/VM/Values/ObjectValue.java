package OneLang.VM.Values;



import OneLang.VM.Values.IVMValue;
import java.util.Map;

public class ObjectValue implements IVMValue {
    public Map<String, IVMValue> props;
    
    public ObjectValue(Map<String, IVMValue> props)
    {
        this.props = props;
    }
}