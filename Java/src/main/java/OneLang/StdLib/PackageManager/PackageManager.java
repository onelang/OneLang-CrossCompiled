package OneLang.StdLib.PackageManager;

import OneStd.OneYaml;
import OneStd.YamlValue;

import java.util.List;
import OneLang.StdLib.PackageManager.InterfacePackage;
import OneLang.StdLib.PackageManager.ImplementationPackage;
import OneLang.StdLib.PackageManager.PackageSource;
import java.util.ArrayList;
import java.util.Arrays;
import OneLang.StdLib.PackageManager.PackageContent;
import OneLang.StdLib.PackageManager.ImplPkgImplementation;
import OneStd.Objects;
import java.util.stream.Collectors;
import OneLang.StdLib.PackageManager.PackageNativeImpl;
import java.util.LinkedHashMap;

public class PackageManager {
    public List<InterfacePackage> interfacesPkgs;
    public List<ImplementationPackage> implementationPkgs;
    public PackageSource source;
    
    public PackageManager(PackageSource source)
    {
        this.source = source;
        this.interfacesPkgs = new ArrayList<InterfacePackage>();
        this.implementationPkgs = new ArrayList<ImplementationPackage>();
    }
    
    public void loadAllCached() {
        var allPackages = this.source.getAllCached();
        
        for (var content : Arrays.stream(allPackages.packages).filter(x -> x.id.type == PackageType.Interface).toArray(PackageContent[]::new))
            this.interfacesPkgs.add(new InterfacePackage(content));
        
        for (var content : Arrays.stream(allPackages.packages).filter(x -> x.id.type == PackageType.Implementation).toArray(PackageContent[]::new))
            this.implementationPkgs.add(new ImplementationPackage(content));
    }
    
    public ImplPkgImplementation[] getLangImpls(String langName) {
        var allImpls = new ArrayList<ImplPkgImplementation>();
        for (var pkg : this.implementationPkgs)
            for (var impl : pkg.implementations)
                allImpls.add(impl);
        return allImpls.stream().filter(x -> Objects.equals(x.language, langName)).toArray(ImplPkgImplementation[]::new);
    }
    
    public String getInterfaceDefinitions() {
        return Arrays.stream(this.interfacesPkgs.stream().map(x -> x.definition).toArray(String[]::new)).collect(Collectors.joining("\n"));
    }
    
    public PackageNativeImpl[] getLangNativeImpls(String langName) {
        var result = new ArrayList<PackageNativeImpl>();
        for (var pkg : this.implementationPkgs)
            for (var pkgImpl : pkg.implementations.stream().filter(x -> Objects.equals(x.language, langName)).toArray(ImplPkgImplementation[]::new)) {
                var fileNamePaths = new LinkedHashMap<String, String>();
                for (var fileName : pkgImpl.nativeIncludes)
                    fileNamePaths.put(fileName, "native/" + fileName);
                
                var incDir = pkgImpl.nativeIncludeDir;
                if (incDir != null) {
                    if (!incDir.endsWith("/"))
                        incDir += "/";
                    var prefix = "native/" + incDir;
                    for (var fn : Arrays.stream(Arrays.stream(pkg.content.files.keySet().toArray(String[]::new)).filter(x -> x.startsWith(prefix)).toArray(String[]::new)).map(x -> x.substring(prefix.length())).toArray(String[]::new))
                        fileNamePaths.put(fn, prefix + fn);
                }
                
                for (var fileName : fileNamePaths.keySet().toArray(String[]::new)) {
                    var path = fileNamePaths.get(fileName);
                    var code = pkg.content.files.get(path);
                    if (code == null)
                        throw new Error("File '" + fileName + "' was not found for package '" + pkg.implementationYaml.name + "'");
                    var impl = new PackageNativeImpl();
                    impl.pkgName = pkg.implementationYaml.name;
                    impl.pkgVendor = pkg.implementationYaml.vendor;
                    impl.pkgVersion = pkg.implementationYaml.version;
                    impl.fileName = fileName;
                    impl.code = code;
                    result.add(impl);
                }
            }
        return result.toArray(PackageNativeImpl[]::new);
    }
}