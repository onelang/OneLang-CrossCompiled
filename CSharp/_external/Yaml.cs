using System.IO;
using System.Linq;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using YamlDotNet.RepresentationModel;

public class YamlValue {
    public YamlMappingNode node;

    public YamlValue(YamlMappingNode node) { this.node = node; }
    public YamlValue obj(string key) { return new YamlValue((YamlMappingNode) this.node[key]); }
    public double dbl(string key) { return this.node.Children.TryGetValue(key, out var value) ? double.Parse(((YamlScalarNode)value).Value) : double.NaN; }
    public string str(string key) { return this.node.Children.TryGetValue(key, out var value) ? ((YamlScalarNode)value).Value : null; }
    
    public YamlValue[] arr(string key)
    {
        return this.node.Children.TryGetValue(key, out var value) ? 
            ((YamlSequenceNode)this.node[key]).Cast<YamlMappingNode>().Select(x => new YamlValue(x)).ToArray() : new YamlValue[0];
    }
    
    public string[] strArr(string key) 
    {
        return this.node.Children.TryGetValue(key, out var value) ? 
            ((YamlSequenceNode)this.node[key]).Cast<YamlScalarNode>().Select(x => x.Value).ToArray() : new string[0];
    }
}

public class OneYaml {
    public static YamlValue load(string content)
    {
        var yaml = new YamlStream();
        yaml.Load(new StringReader(content));
        return new YamlValue((YamlMappingNode)yaml.Documents[0].RootNode);
    }
}