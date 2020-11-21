package OneLang.StdLib.PackageManager;

import OneStd.OneYaml;
import OneStd.YamlValue;

import OneLang.StdLib.PackageManager.ImplPkgNativeDependency;
import OneLang.StdLib.PackageManager.ImplPkgLanguage;
import java.util.Arrays;
import OneStd.YamlValue;

public class ImplPkgLanguage {
    public String id;
    public String packageDir;
    public String nativeSrcDir;
    public ImplPkgNativeDependency[] nativeDependencies;
    
    public ImplPkgLanguage(String id, String packageDir, String nativeSrcDir, ImplPkgNativeDependency[] nativeDependencies)
    {
        this.id = id;
        this.packageDir = packageDir;
        this.nativeSrcDir = nativeSrcDir;
        this.nativeDependencies = nativeDependencies;
    }
    
    public static ImplPkgLanguage fromYaml(YamlValue obj) {
        return new ImplPkgLanguage(obj.str("id"), obj.str("package-dir"), obj.str("native-src-dir"), Arrays.stream(obj.arr("native-dependencies")).map(impl -> ImplPkgNativeDependency.fromYaml(impl)).toArray(ImplPkgNativeDependency[]::new));
    }
}