package OneLang.One.Serialization.JsonSerializer;

import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.EnumType;
import OneLang.One.Ast.AstTypes.IInterfaceType;
import OneLang.One.Ast.AstTypes.InterfaceType;
import OneLang.One.Ast.AstTypes.TypeHelper;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.LiteralTypes;
import OneLang.One.Ast.Types.Package;
import OneStd.ReflectedValue;

import java.util.Map;
import OneLang.One.Ast.Types.LiteralTypes;
import java.util.LinkedHashMap;
import OneStd.RegExp;
import java.util.Arrays;
import java.util.stream.Collectors;
import OneLang.One.Ast.Interfaces.IType;
import OneStd.JSON;
import java.util.ArrayList;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.InterfaceType;
import OneLang.One.Ast.AstTypes.IInterfaceType;
import OneLang.One.Ast.Types.Field;
import java.util.List;
import OneLang.One.Ast.AstTypes.EnumType;
import OneStd.ReflectedValue;

public class JsonSerializer {
    public Map<Object, String> circleDetector;
    public LiteralTypes litTypes;
    
    public JsonSerializer(LiteralTypes litTypes)
    {
        this.litTypes = litTypes;
        this.circleDetector = new LinkedHashMap<Object, String>();
    }
    
    public String pad(String str) {
        return Arrays.stream(Arrays.stream(str.split("\\n", -1)).map(x -> "    " + x).toArray(String[]::new)).collect(Collectors.joining("\n"));
    }
    
    public String serialize(ReflectedValue obj) {
        var declType = ((IType)obj.getDeclaredType());
        if (obj.isNull())
            return "null";
        else if (TypeHelper.equals(declType, this.litTypes.string))
            return JSON.stringify(obj.getStringValue());
        else if (TypeHelper.equals(declType, this.litTypes.boolean_))
            return obj.getBooleanValue() ? "true" : "false";
        else if (TypeHelper.isAssignableTo(declType, this.litTypes.array)) {
            var items = new ArrayList<String>();
            for (var item : obj.getArrayItems())
                items.add(this.serialize(item));
            return items.size() == 0 ? "[]" : "[\n" + this.pad(items.stream().collect(Collectors.joining(",\n"))) + "\n]";
        }
        else if (TypeHelper.isAssignableTo(declType, this.litTypes.map)) {
            var items = new ArrayList<String>();
            for (var key : obj.getMapKeys()) {
                var value = obj.getMapValue(key);
                items.add("\"" + key + "\": " + this.serialize(value));
            }
            return items.size() == 0 ? "{}" : "{\n" + this.pad(items.stream().collect(Collectors.joining(",\n"))) + "\n}";
        }
        else if (declType instanceof ClassType || declType instanceof InterfaceType) {
            var rawValue = obj.getUniqueIdentifier();
            if (this.circleDetector.containsKey(rawValue))
                return "{\"$ref\":\"" + this.circleDetector.get(rawValue) + "\"}";
            var id = "id_" + this.circleDetector.size();
            this.circleDetector.put(rawValue, id);
            
            var valueType = ((IType)obj.getValueType());
            var decl = (((IInterfaceType)declType)).getDecl();
            
            var members = new ArrayList<String>();
            
            members.add("\"$id\": \"" + id + "\"");
            
            if (valueType != null && !TypeHelper.equals(valueType, declType))
                members.add("\"$type\": \"" + valueType.repr() + "\"");
            
            for (var field : Arrays.stream(decl.getFields()).filter(x -> !x.getIsStatic()).toArray(Field[]::new)) {
                if (field.getAttributes().containsKey("json-ignore"))
                    continue;
                //console.log(`processing ${field.parentInterface.name}::${field.name}`);
                var value = obj.getField(field.getName());
                var serializedValue = this.serialize(value);
                if (!new ArrayList<>(List.of("[]", "{}", "null", "false", "\"\"")).stream().anyMatch(serializedValue::equals))
                    members.add("\"" + field.getName() + "\": " + serializedValue);
            }
            return members.size() == 0 ? "{}" : "{\n" + this.pad(members.stream().collect(Collectors.joining(",\n"))) + "\n}";
        }
        else if (declType instanceof EnumType)
            return "\"" + obj.getEnumValueAsString() + "\"";
        return "\"<UNKNOWN-TYPE>\"";
    }
}