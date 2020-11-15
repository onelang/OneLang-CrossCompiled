package OneLang.StdLib.PackageManager;

import OneStd.OneYaml;
import OneStd.YamlValue;

public class InterfaceDependency {
    public String name;
    public Double minver;
    
    public InterfaceDependency(String name, Double minver)
    {
        this.name = name;
        this.minver = minver;
    }
}