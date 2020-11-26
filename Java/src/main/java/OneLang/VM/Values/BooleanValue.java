package OneLang.VM.Values;



import OneLang.VM.Values.IVMValue;
import OneLang.VM.Values.BooleanValue;

public class BooleanValue implements IVMValue {
    public Boolean value;
    
    public BooleanValue(Boolean value)
    {
        this.value = value;
    }
    
    public Boolean equals(IVMValue other) {
        return other instanceof BooleanValue && ((BooleanValue)other).value == this.value;
    }
}