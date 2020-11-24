package OneLang.VM.Values;



import OneLang.VM.Values.IVMValue;

public interface ICallableValue extends IVMValue {
    IVMValue call(IVMValue[] args);
}