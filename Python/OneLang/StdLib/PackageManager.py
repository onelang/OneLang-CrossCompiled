from OneLangStdLib import *
from enum import Enum
from OneYaml import *

class PACKAGE_TYPE(Enum):
    INTERFACE = 1
    IMPLEMENTATION = 2

class PackageId:
    def __init__(self, type, name, version):
        self.type = type
        self.name = name
        self.version = version

class PackageContent:
    def __init__(self, id, files, from_cache):
        self.id = id
        self.files = files

class PackageBundle:
    def __init__(self, packages):
        self.packages = packages

class PackageNativeImpl:
    def __init__(self):
        self.pkg_name = None
        self.pkg_vendor = None
        self.pkg_version = None
        self.file_name = None
        self.code = None

class InterfaceDependency:
    def __init__(self, name, minver):
        self.name = name
        self.minver = minver

class InterfaceYaml:
    def __init__(self, file_version, vendor, name, version, definition_file, dependencies):
        self.file_version = file_version
        self.vendor = vendor
        self.name = name
        self.version = version
        self.definition_file = definition_file
        self.dependencies = dependencies
    
    @classmethod
    def from_yaml(cls, obj):
        return InterfaceYaml(obj.dbl("file-version"), obj.str("vendor"), obj.str("name"), obj.dbl("version"), obj.str("definition-file"), list(map(lambda dep: InterfaceDependency(dep.str("name"), dep.dbl("minver")), obj.arr("dependencies"))))

class InterfacePackage:
    def __init__(self, content):
        self.interface_yaml = None
        self.definition = None
        self.content = content
        self.interface_yaml = InterfaceYaml.from_yaml(OneYaml.load(content.files.get("interface.yaml")))
        self.definition = content.files.get(self.interface_yaml.definition_file)

class ImplPkgImplIntf:
    def __init__(self, name, minver, maxver):
        self.name = name
        self.minver = minver
        self.maxver = maxver

class ImplPkgImplementation:
    def __init__(self, interface_, language, native_includes, native_include_dir):
        self.interface_ = interface_
        self.language = language
        self.native_includes = native_includes
        self.native_include_dir = native_include_dir

class ImplPackageYaml:
    def __init__(self, file_version, vendor, name, description, version, includes, implements_):
        self.file_version = file_version
        self.vendor = vendor
        self.name = name
        self.description = description
        self.version = version
        self.includes = includes
        self.implements_ = implements_
    
    @classmethod
    def from_yaml(cls, obj):
        return ImplPackageYaml(obj.dbl("file-version"), obj.str("vendor"), obj.str("name"), obj.str("description"), obj.str("version"), obj.str_arr("includes"), list(map(lambda impl: ImplPkgImplementation(ImplPkgImplIntf(impl.obj("interface").str("name"), impl.obj("interface").dbl("minver"), impl.obj("interface").dbl("maxver")), impl.str("language"), impl.str_arr("native-includes"), impl.str("native-include-dir")), obj.arr("implements"))))

class ImplementationPackage:
    def __init__(self, content):
        self.implementation_yaml = None
        self.implementations = []
        self.content = content
        self.implementation_yaml = ImplPackageYaml.from_yaml(OneYaml.load(content.files.get("package.yaml")))
        self.implementations = []
        for impl in self.implementation_yaml.implements_ or []:
            self.implementations.append(impl)
        for include in self.implementation_yaml.includes or []:
            included = ImplPackageYaml.from_yaml(OneYaml.load(content.files.get(include)))
            for impl in included.implements_:
                self.implementations.append(impl)

class PackageManager:
    def __init__(self, source):
        self.interfaces_pkgs = []
        self.implementation_pkgs = []
        self.source = source
    
    def load_all_cached(self):
        all_packages = self.source.get_all_cached()
        
        for content in list(filter(lambda x: x.id.type == PACKAGE_TYPE.INTERFACE, all_packages.packages)):
            self.interfaces_pkgs.append(InterfacePackage(content))
        
        for content in list(filter(lambda x: x.id.type == PACKAGE_TYPE.IMPLEMENTATION, all_packages.packages)):
            self.implementation_pkgs.append(ImplementationPackage(content))
    
    def get_lang_impls(self, lang_name):
        all_impls = []
        for pkg in self.implementation_pkgs:
            for impl in pkg.implementations:
                all_impls.append(impl)
        return list(filter(lambda x: x.language == lang_name, all_impls))
    
    def get_interface_definitions(self):
        return "\n".join(list(map(lambda x: x.definition, self.interfaces_pkgs)))
    
    def get_lang_native_impls(self, lang_name):
        result = []
        for pkg in self.implementation_pkgs:
            for pkg_impl in list(filter(lambda x: x.language == lang_name, pkg.implementations)):
                file_name_paths = {}
                for file_name in pkg_impl.native_includes:
                    file_name_paths[file_name] = f'''native/{file_name}'''
                
                inc_dir = pkg_impl.native_include_dir
                if inc_dir != None:
                    if not inc_dir.endswith("/"):
                        inc_dir += "/"
                    prefix = f'''native/{inc_dir}'''
                    for fn in list(map(lambda x: x[len(prefix):], list(filter(lambda x: x.startswith(prefix), pkg.content.files.keys())))):
                        file_name_paths[fn] = f'''{prefix}{fn}'''
                
                for file_name in file_name_paths.keys():
                    path = file_name_paths.get(file_name)
                    code = pkg.content.files.get(path)
                    if code == None:
                        raise Error(f'''File \'{file_name}\' was not found for package \'{pkg.implementation_yaml.name}\'''')
                    impl = PackageNativeImpl()
                    impl.pkg_name = pkg.implementation_yaml.name
                    impl.pkg_vendor = pkg.implementation_yaml.vendor
                    impl.pkg_version = pkg.implementation_yaml.version
                    impl.file_name = file_name
                    impl.code = code
                    result.append(impl)
        return result