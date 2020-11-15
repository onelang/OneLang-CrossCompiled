using StdLib;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace StdLib
{
    public class PackagesFolderSource : PackageSource {
        public string packagesDir;
        
        public PackagesFolderSource(string packagesDir = "packages")
        {
            this.packagesDir = packagesDir;
        }
        
        public Task<PackageBundle> getPackageBundle(PackageId[] ids, bool cachedOnly)
        {
            throw new Error("Method not implemented.");
        }
        
        public async Task<PackageBundle> getAllCached()
        {
            var packages = new Dictionary<string, PackageContent> {};
            var allFiles = OneFile.listFiles(this.packagesDir, true);
            foreach (var fn in allFiles) {
                if (fn == "bundle.json")
                    continue;
                // TODO: hack
                var pathParts = fn.split(new RegExp("/")).ToList();
                // [0]=implementations/interfaces, [1]=package-name, [2:]=path
                var type = pathParts.shift();
                var pkgDir = pathParts.shift();
                if (type != "implementations" && type != "interfaces")
                    continue;
                // skip e.g. bundle.json
                var pkgIdStr = $"{type}/{pkgDir}";
                var pkg = packages.get(pkgIdStr);
                if (pkg == null) {
                    var pkgDirParts = pkgDir.split(new RegExp("-")).ToList();
                    var version = pkgDirParts.pop().replace(new RegExp("^v"), "");
                    var pkgType = type == "implementations" ? PackageType.Implementation : PackageType.Interface;
                    var pkgId = new PackageId(pkgType, pkgDirParts.join("-"), version);
                    pkg = new PackageContent(pkgId, new Dictionary<string, string> {}, true);
                    packages.set(pkgIdStr, pkg);
                }
                pkg.files.set(pathParts.join("/"), OneFile.readText($"{this.packagesDir}/{fn}"));
            }
            return new PackageBundle(Object.values(packages));
        }
    }
}