from OneLangStdLib import *
from OneFile import *
import OneLang.StdLib.PackageManager as packMan
import re

class PackagesFolderSource:
    def __init__(self, packages_dir = "packages"):
        self.packages_dir = packages_dir
    
    def get_package_bundle(self, ids, cached_only):
        raise Error("Method not implemented.")
    
    def get_all_cached(self):
        packages = {}
        all_files = OneFile.list_files(self.packages_dir, True)
        for fn in all_files:
            if fn == "bundle.json":
                continue
            # TODO: hack
            path_parts = re.split("/", fn)
            # [0]=implementations/interfaces, [1]=package-name, [2:]=path
            type = path_parts.pop(0)
            pkg_dir = path_parts.pop(0)
            if type != "implementations" and type != "interfaces":
                continue
            # skip e.g. bundle.json
            pkg_id_str = f'''{type}/{pkg_dir}'''
            pkg = packages.get(pkg_id_str)
            if pkg == None:
                pkg_dir_parts = re.split("-", pkg_dir)
                version = re.sub("^v", "", pkg_dir_parts.pop())
                pkg_type = packMan.PACKAGE_TYPE.IMPLEMENTATION if type == "implementations" else packMan.PACKAGE_TYPE.INTERFACE
                pkg_id = packMan.PackageId(pkg_type, "-".join(pkg_dir_parts), version)
                pkg = packMan.PackageContent(pkg_id, {}, True)
                packages[pkg_id_str] = pkg
            pkg.files["/".join(path_parts)] = OneFile.read_text(f'''{self.packages_dir}/{fn}''')
        return packMan.PackageBundle(packages.values())