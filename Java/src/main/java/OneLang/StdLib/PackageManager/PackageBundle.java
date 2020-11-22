package OneLang.StdLib.PackageManager;

import io.onelang.std.yaml.OneYaml;
import io.onelang.std.yaml.YamlValue;

import OneLang.StdLib.PackageManager.PackageContent;

public class PackageBundle {
    public PackageContent[] packages;
    
    public PackageBundle(PackageContent[] packages)
    {
        this.packages = packages;
    }
}