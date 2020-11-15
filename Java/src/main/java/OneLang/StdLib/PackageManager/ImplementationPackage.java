package OneLang.StdLib.PackageManager;

import OneStd.OneYaml;
import OneStd.YamlValue;

import OneLang.StdLib.PackageManager.ImplPackageYaml;
import java.util.List;
import OneLang.StdLib.PackageManager.ImplPkgImplementation;
import OneLang.StdLib.PackageManager.PackageContent;
import java.util.ArrayList;

public class ImplementationPackage {
    public ImplPackageYaml implementationYaml;
    public List<ImplPkgImplementation> implementations;
    public PackageContent content;
    
    public ImplementationPackage(PackageContent content)
    {
        this.content = content;
        this.implementations = new ArrayList<ImplPkgImplementation>();
        this.implementationYaml = ImplPackageYaml.fromYaml(OneYaml.load(content.files.get("package.yaml")));
        this.implementations = new ArrayList<ImplPkgImplementation>();
        for (var impl : this.implementationYaml.implements_ != null ? this.implementationYaml.implements_ : new ImplPkgImplementation[0])
            this.implementations.add(impl);
        for (var include : this.implementationYaml.includes != null ? this.implementationYaml.includes : new String[0]) {
            var included = ImplPackageYaml.fromYaml(OneYaml.load(content.files.get(include)));
            for (var impl : included.implements_)
                this.implementations.add(impl);
        }
    }
}