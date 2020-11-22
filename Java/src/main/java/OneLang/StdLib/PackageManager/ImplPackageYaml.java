package OneLang.StdLib.PackageManager;

import io.onelang.std.yaml.OneYaml;
import io.onelang.std.yaml.YamlValue;

import OneLang.StdLib.PackageManager.ImplPkgImplementation;
import OneLang.StdLib.PackageManager.ImplPkgLanguage;
import java.util.Map;
import OneLang.StdLib.PackageManager.ImplPackageYaml;
import java.util.LinkedHashMap;
import java.util.Arrays;
import io.onelang.std.yaml.YamlValue;

public class ImplPackageYaml {
    public Double fileVersion;
    public String vendor;
    public String name;
    public String description;
    public String version;
    public String[] includes;
    public ImplPkgImplementation[] implements_;
    public Map<String, ImplPkgLanguage> languages;
    
    public ImplPackageYaml(Double fileVersion, String vendor, String name, String description, String version, String[] includes, ImplPkgImplementation[] implements_, Map<String, ImplPkgLanguage> languages)
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
        var languages = new LinkedHashMap<String, ImplPkgLanguage>();
        var langDict = obj.dict("languages");
        if (langDict != null)
            for (var langName : langDict.keySet().toArray(String[]::new))
                languages.put(langName, ImplPkgLanguage.fromYaml(langDict.get(langName)));
        
        return new ImplPackageYaml(obj.dbl("file-version"), obj.str("vendor"), obj.str("name"), obj.str("description"), obj.str("version"), obj.strArr("includes"), Arrays.stream(obj.arr("implements")).map(impl -> ImplPkgImplementation.fromYaml(impl)).toArray(ImplPkgImplementation[]::new), languages);
    }
}