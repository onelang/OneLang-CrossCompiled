package OneLang.StdLib.PackageManager;

import io.onelang.std.yaml.OneYaml;
import io.onelang.std.yaml.YamlValue;

import OneLang.StdLib.PackageManager.ImplPkgNativeDependency;
import io.onelang.std.yaml.YamlValue;

public class ImplPkgNativeDependency {
    public String name;
    public String version;
    
    public ImplPkgNativeDependency(String name, String version)
    {
        this.name = name;
        this.version = version;
    }
    
    public static ImplPkgNativeDependency fromYaml(YamlValue obj) {
        return new ImplPkgNativeDependency(obj.str("name"), obj.str("version"));
    }
}