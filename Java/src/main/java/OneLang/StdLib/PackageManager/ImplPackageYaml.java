package OneLang.StdLib.PackageManager;

import OneStd.OneYaml;
import OneStd.YamlValue;

import OneLang.StdLib.PackageManager.ImplPkgImplementation;
import OneLang.StdLib.PackageManager.ImplPackageYaml;
import OneLang.StdLib.PackageManager.ImplPkgImplIntf;
import java.util.Arrays;
import OneStd.YamlValue;

public class ImplPackageYaml {
    public Double fileVersion;
    public String vendor;
    public String name;
    public String description;
    public String version;
    public String[] includes;
    public ImplPkgImplementation[] implements_;
    
    public ImplPackageYaml(Double fileVersion, String vendor, String name, String description, String version, String[] includes, ImplPkgImplementation[] implements_)
    {
        this.fileVersion = fileVersion;
        this.vendor = vendor;
        this.name = name;
        this.description = description;
        this.version = version;
        this.includes = includes;
        this.implements_ = implements_;
    }
    
    public static ImplPackageYaml fromYaml(YamlValue obj) {
        return new ImplPackageYaml(obj.dbl("file-version"), obj.str("vendor"), obj.str("name"), obj.str("description"), obj.str("version"), obj.strArr("includes"), Arrays.stream(obj.arr("implements")).map(impl -> new ImplPkgImplementation(new ImplPkgImplIntf(impl.obj("interface").str("name"), impl.obj("interface").dbl("minver"), impl.obj("interface").dbl("maxver")), impl.str("language"), impl.strArr("native-includes"), impl.str("native-include-dir"))).toArray(ImplPkgImplementation[]::new));
    }
}