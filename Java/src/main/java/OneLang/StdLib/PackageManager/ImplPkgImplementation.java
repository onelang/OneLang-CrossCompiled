package OneLang.StdLib.PackageManager;

import io.onelang.std.yaml.OneYaml;
import io.onelang.std.yaml.YamlValue;

import OneLang.StdLib.PackageManager.ImplPkgImplIntf;
import OneLang.StdLib.PackageManager.ImplPkgImplementation;
import io.onelang.std.yaml.YamlValue;

public class ImplPkgImplementation {
    public ImplPkgImplIntf interface_;
    public String language;
    public String[] nativeIncludes;
    public String nativeIncludeDir;
    
    public ImplPkgImplementation(ImplPkgImplIntf interface_, String language, String[] nativeIncludes, String nativeIncludeDir)
    {
        this.interface_ = interface_;
        this.language = language;
        this.nativeIncludes = nativeIncludes;
        this.nativeIncludeDir = nativeIncludeDir;
    }
    
    public static ImplPkgImplementation fromYaml(YamlValue obj) {
        return new ImplPkgImplementation(ImplPkgImplIntf.fromYaml(obj.obj("interface")), obj.str("language"), obj.strArr("native-includes"), obj.str("native-include-dir"));
    }
}