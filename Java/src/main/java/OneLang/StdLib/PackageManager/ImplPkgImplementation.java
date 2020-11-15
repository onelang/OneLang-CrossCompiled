package OneLang.StdLib.PackageManager;

import OneStd.OneYaml;
import OneStd.YamlValue;

import OneLang.StdLib.PackageManager.ImplPkgImplIntf;

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
}