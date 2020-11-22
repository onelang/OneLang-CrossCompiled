package OneLang.StdLib.PackageManager;

import io.onelang.std.yaml.OneYaml;
import io.onelang.std.yaml.YamlValue;

public class InterfaceDependency {
    public String name;
    public Double minver;
    
    public InterfaceDependency(String name, Double minver)
    {
        this.name = name;
        this.minver = minver;
    }
}