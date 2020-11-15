package OneLang.StdLib.PackageManager;

import OneStd.OneYaml;
import OneStd.YamlValue;

import OneLang.StdLib.PackageManager.InterfaceYaml;
import OneLang.StdLib.PackageManager.PackageContent;

public class InterfacePackage {
    public InterfaceYaml interfaceYaml;
    public String definition;
    public PackageContent content;
    
    public InterfacePackage(PackageContent content)
    {
        this.content = content;
        this.interfaceYaml = InterfaceYaml.fromYaml(OneYaml.load(content.files.get("interface.yaml")));
        this.definition = content.files.get(this.interfaceYaml.definitionFile);
    }
}