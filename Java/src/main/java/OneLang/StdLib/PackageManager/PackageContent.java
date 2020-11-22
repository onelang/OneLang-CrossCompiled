package OneLang.StdLib.PackageManager;

import io.onelang.std.yaml.OneYaml;
import io.onelang.std.yaml.YamlValue;

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