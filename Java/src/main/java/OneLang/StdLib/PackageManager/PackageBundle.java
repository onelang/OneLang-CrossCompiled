package OneLang.StdLib.PackageManager;

import OneStd.OneYaml;
import OneStd.YamlValue;

import OneLang.StdLib.PackageManager.PackageContent;

public class PackageBundle {
    public PackageContent[] packages;
    
    public PackageBundle(PackageContent[] packages)
    {
        this.packages = packages;
    }
}