using System.Collections.Generic;

namespace VM
{
    public interface IVMValue {
        bool equals(IVMValue other);
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
        
        public bool equals(IVMValue other)
        {
            return false;
        }
    }
    
    public class StringValue : IVMValue {
        public string value;
        
        public StringValue(string value)
        {
            this.value = value;
        }
        
        public bool equals(IVMValue other)
        {
            return other is StringValue strValue && strValue.value == this.value;
        }
    }
    
    public class NumericValue : IVMValue {
        public int value;
        
        public NumericValue(int value)
        {
            this.value = value;
        }
        
        public bool equals(IVMValue other)
        {
            return other is NumericValue numValue && numValue.value == this.value;
        }
    }
    
    public class BooleanValue : IVMValue {
        public bool value;
        
        public BooleanValue(bool value)
        {
            this.value = value;
        }
        
        public bool equals(IVMValue other)
        {
            return other is BooleanValue boolValue && boolValue.value == this.value;
        }
    }
    
    public class ArrayValue : IVMValue {
        public IVMValue[] items;
        
        public ArrayValue(IVMValue[] items)
        {
            this.items = items;
        }
        
        public bool equals(IVMValue other)
        {
            return false;
        }
    }
}