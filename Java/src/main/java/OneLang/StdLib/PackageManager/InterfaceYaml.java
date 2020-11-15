package OneLang.StdLib.PackageManager;

import OneStd.OneYaml;
import OneStd.YamlValue;

import OneLang.StdLib.PackageManager.InterfaceDependency;
import OneLang.StdLib.PackageManager.InterfaceYaml;
import java.util.Arrays;
import OneStd.YamlValue;

public class InterfaceYaml {
    public Double fileVersion;
    public String vendor;
    public String name;
    public Double version;
    public String definitionFile;
    public InterfaceDependency[] dependencies;
    
    public InterfaceYaml(Double fileVersion, String vendor, String name, Double version, String definitionFile, InterfaceDependency[] dependencies)
    {
        this.fileVersion = fileVersion;
        this.vendor = vendor;
        this.name = name;
        this.version = version;
        this.definitionFile = definitionFile;
        this.dependencies = dependencies;
    }
    
    public static InterfaceYaml fromYaml(YamlValue obj) {
        return new InterfaceYaml(obj.dbl("file-version"), obj.str("vendor"), obj.str("name"), obj.dbl("version"), obj.str("definition-file"), Arrays.stream(obj.arr("dependencies")).map(dep -> new InterfaceDependency(dep.str("name"), dep.dbl("minver"))).toArray(InterfaceDependency[]::new));
    }
}