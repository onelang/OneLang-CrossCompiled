using One.Ast;
using System.Collections.Generic;

namespace One.Serialization
{
    public class JsonSerializer {
        public Map<object, string> circleDetector;
        public LiteralTypes litTypes;
        
        public JsonSerializer(LiteralTypes litTypes)
        {
            this.litTypes = litTypes;
            this.circleDetector = new Map<object, string>();
        }
        
        public string pad(string str)
        {
            return str.split(new RegExp("\\n")).map(x => $"    {x}").join("\n");
        }
        
        public string serialize(ReflectedValue obj)
        {
            var declType = ((IType)obj.getDeclaredType());
            if (obj.isNull())
                return "null";
            else if (TypeHelper.equals(declType, this.litTypes.string_))
                return JSON.stringify(obj.getStringValue());
            else if (TypeHelper.equals(declType, this.litTypes.boolean))
                return obj.getBooleanValue() ? "true" : "false";
            else if (TypeHelper.isAssignableTo(declType, this.litTypes.array)) {
                var items = new List<string>();
                foreach (var item in obj.getArrayItems())
                    items.push(this.serialize(item));
                return items.length() == 0 ? "[]" : $"[\n{this.pad(items.join(",\n"))}\n]";
            }
            else if (TypeHelper.isAssignableTo(declType, this.litTypes.map)) {
                var items = new List<string>();
                foreach (var key in obj.getMapKeys()) {
                    var value = obj.getMapValue(key);
                    items.push($"\"{key}\": {this.serialize(value)}");
                }
                return items.length() == 0 ? "{}" : $"{{\n{this.pad(items.join(",\n"))}\n}}";
            }
            else if (declType is ClassType classType || declType is InterfaceType) {
                var rawValue = obj.getUniqueIdentifier();
                if (this.circleDetector.has(rawValue))
                    return $"{{\"$ref\":\"{this.circleDetector.get(rawValue)}\"}}";
                var id = $"id_{this.circleDetector.size()}";
                this.circleDetector.set(rawValue, id);
                
                var valueType = ((IType)obj.getValueType());
                var decl = (((IInterfaceType)declType)).getDecl();
                
                var members = new List<string>();
                
                members.push($"\"$id\": \"{id}\"");
                
                if (valueType != null && !TypeHelper.equals(valueType, declType))
                    members.push($"\"$type\": \"{valueType.repr()}\"");
                
                foreach (var field in decl.fields.filter(x => !x.isStatic)) {
                    if (field.attributes.hasKey("json-ignore"))
                        continue;
                    //console.log(`processing ${field.parentInterface.name}::${field.name}`);
                    var value = obj.getField(field.name);
                    var serializedValue = this.serialize(value);
                    if (!new List<string> { "[]", "{}", "null", "false", "\"\"" }.includes(serializedValue))
                        members.push($"\"{field.name}\": {serializedValue}");
                }
                return members.length() == 0 ? "{}" : $"{{\n{this.pad(members.join(",\n"))}\n}}";
            }
            else if (declType is EnumType)
                return $"\"{obj.getEnumValueAsString()}\"";
            return "\"<UNKNOWN-TYPE>\"";
        }
    }
}