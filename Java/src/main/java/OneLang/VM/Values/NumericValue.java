package OneLang.VM.Values;



import OneLang.VM.Values.IVMValue;

public class NumericValue implements IVMValue {
    public Integer value;
    
    public NumericValue(Integer value)
    {
        this.value = value;
    }
}