package OneLang.VM.Values;



import OneLang.VM.Values.IVMValue;

public class StringValue implements IVMValue {
    public String value;
    
    public StringValue(String value)
    {
        this.value = value;
    }
}