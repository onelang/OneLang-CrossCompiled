package OneLang.VM.Values;



import OneLang.VM.Values.IVMValue;

public class BooleanValue implements IVMValue {
    public Boolean value;
    
    public BooleanValue(Boolean value)
    {
        this.value = value;
    }
}