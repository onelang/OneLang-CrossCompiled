package OneLang.StdLib.PackageManager;

import io.onelang.std.yaml.OneYaml;
import io.onelang.std.yaml.YamlValue;

import OneLang.StdLib.PackageManager.PackageBundle;
import OneLang.StdLib.PackageManager.PackageId;

public interface PackageSource {
    PackageBundle getPackageBundle(PackageId[] ids, Boolean cachedOnly);
    PackageBundle getAllCached();
}