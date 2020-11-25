using System.Collections.Generic;

namespace VM
{
    public interface IVMValue {
        
    }
    
    public interface ICallableValue : IVMValue {
        IVMValue call(IVMValue[] args);
    }
    
    public class ObjectValue : IVMValue {
        public Dictionary<string, IVMValue> props;
        
        public ObjectValue(Dictionary<string, IVMValue> props)
        {
            this.props = props;
        }
    }
    
    public class StringValue : IVMValue {
        public string value;
        
        public StringValue(string value)
        {
            this.value = value;
        }
    }
    
    public class NumericValue : IVMValue {
        public int value;
        
        public NumericValue(int value)
        {
            this.value = value;
        }
    }
    
    public class BooleanValue : IVMValue {
        public bool value;
        
        public BooleanValue(bool value)
        {
            this.value = value;
        }
    }
    
    public class ArrayValue : IVMValue {
        public IVMValue[] items;
        
        public ArrayValue(IVMValue[] items)
        {
            this.items = items;
        }
    }
}