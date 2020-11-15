using One.Ast;
using One;

namespace One.Transforms
{
    public class CollectInheritanceInfo : ITransformer {
        public string name { get; set; }
        
        public CollectInheritanceInfo()
        {
            // C# fix
            this.name = "CollectInheritanceInfo";
        }
        
        public void visitClass(Class cls)
        {
            var allBaseIIntfs = cls.getAllBaseInterfaces();
            var intfs = allBaseIIntfs.map(x => x is Interface int_ ? int_ : null).filter(x => x != null);
            var clses = allBaseIIntfs.map(x => x is Class class_ ? class_ : null).filter(x => x != null && x != cls);
            
            foreach (var field in cls.fields)
                field.interfaceDeclarations = intfs.map(x => x.fields.find(f => f.name == field.name)).filter(x => x != null);
            
            foreach (var method in cls.methods) {
                method.interfaceDeclarations = intfs.map(x => x.methods.find(m => m.name == method.name)).filter(x => x != null);
                method.overrides = clses.map(x => x.methods.find(m => m.name == method.name)).find(x => x != null);
                if (method.overrides != null)
                    method.overrides.overriddenBy.push(method);
            }
        }
        
        public void visitFiles(SourceFile[] files)
        {
            foreach (var file in files)
                foreach (var cls in file.classes)
                    this.visitClass(cls);
        }
    }
}