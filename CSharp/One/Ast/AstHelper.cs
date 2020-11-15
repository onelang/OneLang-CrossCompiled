using One.Ast;
using System.Collections.Generic;

namespace One.Ast
{
    public class AstHelper {
        public static IInterface[] collectAllBaseInterfaces(IInterface intf)
        {
            var result = new Set<IInterface>();
            var toBeProcessed = new List<IInterface> { intf };
            
            while (toBeProcessed.length() > 0) {
                var curr = toBeProcessed.pop();
                result.add(curr);
                
                if (curr is Class class_ && class_.baseClass != null)
                    toBeProcessed.push((((ClassType)class_.baseClass)).decl);
                
                foreach (var baseIntf in curr.baseInterfaces)
                    toBeProcessed.push((((InterfaceType)baseIntf)).decl);
            }
            
            return Array.from(result.values());
        }
    }
}