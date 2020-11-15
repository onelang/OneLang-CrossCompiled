from OneLangStdLib import *
import OneLang.One.Ast.Types as types
import OneLang.Parsers.TypeScriptParser2 as typeScrPars
import OneLang.StdLib.PackageManager as packMan
import OneLang.StdLib.PackagesFolderSource as packFoldSourc
import OneLang.One.Transforms.FillParent as fillPar
import OneLang.One.Transforms.FillAttributesFromTrivia as fillAttrFromTriv
import OneLang.One.Transforms.ResolveGenericTypeIdentifiers as resGenTypeIdents
import OneLang.One.Transforms.ResolveUnresolvedTypes as resUnrTypes
import OneLang.One.Transforms.ResolveImports as resImps
import OneLang.One.Transforms.ConvertToMethodCall as convToMethCall
import OneLang.One.Transforms.ResolveIdentifiers as resIdents
import OneLang.One.Transforms.InstanceOfImplicitCast as instOfImplCast
import OneLang.One.Transforms.DetectMethodCalls as detMethCalls
import OneLang.One.Transforms.InferTypes as inferTypes
import OneLang.One.Transforms.CollectInheritanceInfo as collInhInfo
import OneLang.One.Transforms.FillMutabilityInfo as fillMutInfo
import OneLang.One.AstTransformer as astTrans
import OneLang.One.ITransformer as iTrans
import OneLang.One.Transforms.LambdaCaptureCollector as lambdCaptColl

class Compiler:
    def __init__(self):
        self.pac_man = None
        self.workspace = None
        self.native_file = None
        self.native_exports = None
        self.project_pkg = None
        self.hooks = None
    
    def init(self, packages_dir):
        self.pac_man = packMan.PackageManager(packFoldSourc.PackagesFolderSource(packages_dir))
        self.pac_man.load_all_cached()
    
    def get_transformers(self, for_declaration_file):
        transforms = []
        if for_declaration_file:
            transforms.append(fillPar.FillParent())
            transforms.append(fillAttrFromTriv.FillAttributesFromTrivia())
            transforms.append(resImps.ResolveImports(self.workspace))
            transforms.append(resGenTypeIdents.ResolveGenericTypeIdentifiers())
            transforms.append(resUnrTypes.ResolveUnresolvedTypes())
            transforms.append(fillMutInfo.FillMutabilityInfo())
        else:
            transforms.append(fillPar.FillParent())
            transforms.append(fillAttrFromTriv.FillAttributesFromTrivia())
            transforms.append(resImps.ResolveImports(self.workspace))
            transforms.append(resGenTypeIdents.ResolveGenericTypeIdentifiers())
            transforms.append(convToMethCall.ConvertToMethodCall())
            transforms.append(resUnrTypes.ResolveUnresolvedTypes())
            transforms.append(resIdents.ResolveIdentifiers())
            transforms.append(instOfImplCast.InstanceOfImplicitCast())
            transforms.append(detMethCalls.DetectMethodCalls())
            transforms.append(inferTypes.InferTypes())
            transforms.append(collInhInfo.CollectInheritanceInfo())
            transforms.append(fillMutInfo.FillMutabilityInfo())
            transforms.append(lambdCaptColl.LambdaCaptureCollector())
        return transforms
    
    def setup_native_resolver(self, content):
        self.native_file = typeScrPars.TypeScriptParser2.parse_file(content)
        self.native_exports = types.Package.collect_exports_from_file(self.native_file, True)
        for trans in self.get_transformers(True):
            trans.visit_files([self.native_file])
    
    def new_workspace(self, pkg_name = "@"):
        self.workspace = types.Workspace()
        for intf_pkg in self.pac_man.interfaces_pkgs:
            lib_name = f'''{intf_pkg.interface_yaml.vendor}.{intf_pkg.interface_yaml.name}-v{intf_pkg.interface_yaml.version}'''
            self.add_interface_package(lib_name, intf_pkg.definition)
        
        self.project_pkg = types.Package(pkg_name, False)
        self.workspace.add_package(self.project_pkg)
    
    def add_interface_package(self, lib_name, definition_file_content):
        lib_pkg = types.Package(lib_name, True)
        file = typeScrPars.TypeScriptParser2.parse_file(definition_file_content, types.SourcePath(lib_pkg, types.Package.index))
        self.setup_file(file)
        lib_pkg.add_file(file, True)
        self.workspace.add_package(lib_pkg)
    
    def setup_file(self, file):
        file.add_available_symbols(self.native_exports.get_all_exports())
        file.literal_types = types.LiteralTypes((file.available_symbols.get("TsBoolean")).type, (file.available_symbols.get("TsNumber")).type, (file.available_symbols.get("TsString")).type, (file.available_symbols.get("RegExp")).type, (file.available_symbols.get("TsArray")).type, (file.available_symbols.get("TsMap")).type, (file.available_symbols.get("Error")).type, (file.available_symbols.get("Promise")).type)
        file.array_types = [(file.available_symbols.get("TsArray")).type, (file.available_symbols.get("IterableIterator")).type, (file.available_symbols.get("RegExpExecArray")).type, (file.available_symbols.get("TsString")).type, (file.available_symbols.get("Set")).type]
    
    def add_project_file(self, fn, content):
        file = typeScrPars.TypeScriptParser2.parse_file(content, types.SourcePath(self.project_pkg, fn))
        self.setup_file(file)
        self.project_pkg.add_file(file)
    
    def process_files(self, files):
        for trans in self.get_transformers(False):
            trans.visit_files(files)
            if self.hooks != None:
                self.hooks.after_stage(trans.name)
    
    def process_workspace(self):
        for pkg in list(filter(lambda x: x.definition_only, self.workspace.packages.values())):
            for trans in self.get_transformers(True):
                trans.visit_files(pkg.files.values())
        
        self.process_files(self.project_pkg.files.values())