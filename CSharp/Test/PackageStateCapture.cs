using One.Ast;
using System.Collections.Generic;
using Utils;

namespace Test
{
    public class PackageStateCapture {
        public Dictionary<string, string> overviews;
        public Package pkg;
        
        public PackageStateCapture(Package pkg)
        {
            this.pkg = pkg;
            this.overviews = new Dictionary<string, string> {};
            foreach (var file in Object.values(pkg.files))
                this.overviews.set(file.sourcePath.path, new TSOverviewGenerator(false, false).generate(file));
        }
        
        public string getSummary()
        {
            return Object.keys(this.overviews).map(file => $"=== {file} ===\n\n{this.overviews.get(file)}").join("\n\n");
        }
    }
}