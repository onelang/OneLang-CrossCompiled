package OneLang.VM.Values;



import OneLang.VM.Values.IVMValue;
import OneLang.VM.Values.NumericValue;

public class NumericValue implements IVMValue {
    public Integer value;
    
    public NumericValue(Integer value)
    {
        this.value = value;
    }
    
    public Boolean equals(IVMValue other) {
        return other instanceof NumericValue && ((NumericValue)other).value == this.value;
    }
}