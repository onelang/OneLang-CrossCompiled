package OneLang.StdLib.PackageManager;

import OneStd.OneYaml;
import OneStd.YamlValue;

import OneLang.StdLib.PackageManager.ImplPkgImplementation;
import OneLang.StdLib.PackageManager.ImplPkgLanguage;
import OneLang.StdLib.PackageManager.ImplPackageYaml;
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
    public ImplPkgLanguage[] languages;
    
    public ImplPackageYaml(Double fileVersion, String vendor, String name, String description, String version, String[] includes, ImplPkgImplementation[] implements_, ImplPkgLanguage[] languages)
    {
        this.fileVersion = fileVersion;
        this.vendor = vendor;
        this.name = name;
        this.description = description;
        this.version = version;
        this.includes = includes;
        this.implements_ = implements_;
        this.languages = languages;
    }
    
    public static ImplPackageYaml fromYaml(YamlValue obj) {
        return new ImplPackageYaml(obj.dbl("file-version"), obj.str("vendor"), obj.str("name"), obj.str("description"), obj.str("version"), obj.strArr("includes"), Arrays.stream(obj.arr("implements")).map(impl -> ImplPkgImplementation.fromYaml(impl)).toArray(ImplPkgImplementation[]::new), Arrays.stream(obj.arr("languages")).map(impl -> ImplPkgLanguage.fromYaml(impl)).toArray(ImplPkgLanguage[]::new));
    }
}