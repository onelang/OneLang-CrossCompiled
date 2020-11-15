package OneLang.StdLib.PackageManager;

import OneStd.OneYaml;
import OneStd.YamlValue;

public class ImplPkgImplIntf {
    public String name;
    public Double minver;
    public Double maxver;
    
    public ImplPkgImplIntf(String name, Double minver, Double maxver)
    {
        this.name = name;
        this.minver = minver;
        this.maxver = maxver;
    }
}