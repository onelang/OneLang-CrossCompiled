from OneLangStdLib import *
import OneLang.StdLib.PackageManager as packMan

class PackageBundleSource:
    def __init__(self, bundle):
        self.bundle = bundle
    
    def get_package_bundle(self, ids, cached_only):
        raise Error("Method not implemented.")
    
    def get_all_cached(self):
        return self.bundle