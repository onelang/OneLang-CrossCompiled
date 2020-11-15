using StdLib;
using System.Threading.Tasks;

namespace StdLib
{
    public class PackageBundleSource : PackageSource {
        public PackageBundle bundle;
        
        public PackageBundleSource(PackageBundle bundle)
        {
            this.bundle = bundle;
        }
        
        public Task<PackageBundle> getPackageBundle(PackageId[] ids, bool cachedOnly)
        {
            throw new Error("Method not implemented.");
        }
        
        public async Task<PackageBundle> getAllCached()
        {
            return this.bundle;
        }
    }
}