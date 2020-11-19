package OneLang.StdLib.PackageManager;

import OneStd.OneYaml;
import OneStd.YamlValue;

import OneLang.StdLib.PackageManager.ImplPkgNativeDependency;
import OneLang.StdLib.PackageManager.ImplPkgLanguage;
import java.util.Arrays;
import OneStd.YamlValue;

public class ImplPkgLanguage {
    public String id;
    public ImplPkgNativeDependency[] nativeDependencies;
    
    public ImplPkgLanguage(String id, ImplPkgNativeDependency[] nativeDependencies)
    {
        this.id = id;
        this.nativeDependencies = nativeDependencies;
    }
    
    public static ImplPkgLanguage fromYaml(YamlValue obj) {
        return new ImplPkgLanguage(obj.str("id"), Arrays.stream(obj.arr("native-dependencies")).map(impl -> ImplPkgNativeDependency.fromYaml(impl)).toArray(ImplPkgNativeDependency[]::new));
    }
}