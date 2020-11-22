package OneLang.StdLib.PackageManager;

import io.onelang.std.yaml.OneYaml;
import io.onelang.std.yaml.YamlValue;

import OneLang.StdLib.PackageManager.ImplPkgImplIntf;
import io.onelang.std.yaml.YamlValue;

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
    
    public static ImplPkgImplIntf fromYaml(YamlValue obj) {
        return new ImplPkgImplIntf(obj.str("name"), obj.dbl("minver"), obj.dbl("maxver"));
    }
}