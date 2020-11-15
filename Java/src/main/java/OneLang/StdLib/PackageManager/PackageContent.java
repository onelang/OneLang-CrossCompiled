package OneLang.StdLib.PackageManager;

import OneStd.OneYaml;
import OneStd.YamlValue;

import OneLang.StdLib.PackageManager.PackageId;
import java.util.Map;

public class PackageContent {
    public PackageId id;
    public Map<String, String> files;
    
    public PackageContent(PackageId id, Map<String, String> files, Boolean fromCache)
    {
        this.id = id;
        this.files = files;
    }
}