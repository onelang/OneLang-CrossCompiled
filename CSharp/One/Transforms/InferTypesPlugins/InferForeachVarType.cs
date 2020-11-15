using One.Ast;
using One.Transforms.InferTypesPlugins.Helpers;

namespace One.Transforms.InferTypesPlugins
{
    public class InferForeachVarType : InferTypesPlugin {
        public InferForeachVarType(): base("InferForeachVarType")
        {
            
        }
        
        public override bool handleStatement(Statement stmt)
        {
            if (stmt is ForeachStatement forStat) {
                forStat.items = this.main.runPluginsOn(forStat.items);
                var arrayType = forStat.items.getType();
                var found = false;
                if (arrayType is ClassType classType || arrayType is InterfaceType) {
                    var intfType = ((IInterfaceType)arrayType);
                    var isArrayType = this.main.currentFile.arrayTypes.some(x => x.decl == intfType.getDecl());
                    if (isArrayType && intfType.typeArguments.length() > 0) {
                        forStat.itemVar.type = intfType.typeArguments.get(0);
                        found = true;
                    }
                }
                
                if (!found && !(arrayType is AnyType))
                    this.errorMan.throw_($"Expected array as Foreach items variable, but got {arrayType.repr()}");
                
                this.main.processBlock(forStat.body);
                return true;
            }
            return false;
        }
    }
}