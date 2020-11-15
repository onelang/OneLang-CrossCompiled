package OneLang.StdLib.PackageBundleSource;

import OneLang.StdLib.PackageManager.PackageSource;
import OneLang.StdLib.PackageManager.PackageId;
import OneLang.StdLib.PackageManager.PackageBundle;

import OneLang.StdLib.PackageManager.PackageSource;
import OneLang.StdLib.PackageManager.PackageBundle;
import OneLang.StdLib.PackageManager.PackageId;

public class PackageBundleSource implements PackageSource {
    public PackageBundle bundle;
    
    public PackageBundleSource(PackageBundle bundle)
    {
        this.bundle = bundle;
    }
    
    public PackageBundle getPackageBundle(PackageId[] ids, Boolean cachedOnly) {
        throw new Error("Method not implemented.");
    }
    
    public PackageBundle getAllCached() {
        return this.bundle;
    }
}