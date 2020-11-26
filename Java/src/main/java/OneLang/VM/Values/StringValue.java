package OneLang.VM.Values;



import OneLang.VM.Values.IVMValue;
import OneLang.VM.Values.StringValue;
import io.onelang.std.core.Objects;

public class StringValue implements IVMValue {
    public String value;
    
    public StringValue(String value)
    {
        this.value = value;
    }
    
    public Boolean equals(IVMValue other) {
        return other instanceof StringValue && Objects.equals(((StringValue)other).value, this.value);
    }
}