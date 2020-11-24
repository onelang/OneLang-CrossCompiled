package OneLang.VM.Values;



import OneLang.VM.Values.IVMValue;

public class ArrayValue implements IVMValue {
    public IVMValue[] items;
    
    public ArrayValue(IVMValue[] items)
    {
        this.items = items;
    }
}