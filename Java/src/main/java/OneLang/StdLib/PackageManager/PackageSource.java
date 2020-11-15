package OneLang.StdLib.PackageManager;

import OneStd.OneYaml;
import OneStd.YamlValue;

import OneLang.StdLib.PackageManager.PackageBundle;
import OneLang.StdLib.PackageManager.PackageId;

public interface PackageSource {
    PackageBundle getPackageBundle(PackageId[] ids, Boolean cachedOnly);
    PackageBundle getAllCached();
}