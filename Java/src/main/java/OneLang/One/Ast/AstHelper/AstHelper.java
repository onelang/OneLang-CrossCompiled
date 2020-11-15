package OneLang.One.Ast.AstHelper;

import OneLang.One.Ast.Types.IInterface;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.AstTypes.InterfaceType;
import OneLang.One.Ast.AstTypes.ClassType;

import OneLang.One.Ast.Types.IInterface;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ArrayList;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.InterfaceType;

public class AstHelper {
    public static IInterface[] collectAllBaseInterfaces(IInterface intf) {
        var result = new LinkedHashSet<IInterface>();
        var toBeProcessed = new ArrayList<>(List.of(intf));
        
        while (toBeProcessed.size() > 0) {
            var curr = toBeProcessed.remove(toBeProcessed.size() - 1);
            result.add(curr);
            
            if (curr instanceof Class && ((Class)curr).baseClass != null)
                toBeProcessed.add((((ClassType)((Class)curr).baseClass)).decl);
            
            for (var baseIntf : curr.getBaseInterfaces())
                toBeProcessed.add((((InterfaceType)baseIntf)).decl);
        }
        
        return result.toArray(IInterface[]::new);
    }
}