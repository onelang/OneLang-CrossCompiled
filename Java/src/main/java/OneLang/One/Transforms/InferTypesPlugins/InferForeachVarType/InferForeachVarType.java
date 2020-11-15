package OneLang.One.Transforms.InferTypesPlugins.InferForeachVarType;

import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.InterfaceType;
import OneLang.One.Ast.AstTypes.IInterfaceType;
import OneLang.One.Ast.AstTypes.AnyType;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.Statements.ForeachStatement;

import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import OneLang.One.Ast.Statements.ForeachStatement;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.InterfaceType;
import OneLang.One.Ast.AstTypes.IInterfaceType;
import java.util.Arrays;
import OneLang.One.Ast.AstTypes.AnyType;
import OneLang.One.Ast.Statements.Statement;

public class InferForeachVarType extends InferTypesPlugin {
    public InferForeachVarType()
    {
        super("InferForeachVarType");
        
    }
    
    public Boolean handleStatement(Statement stmt) {
        if (stmt instanceof ForeachStatement) {
            ((ForeachStatement)stmt).items = this.main.runPluginsOn(((ForeachStatement)stmt).items);
            var arrayType = ((ForeachStatement)stmt).items.getType();
            var found = false;
            if (arrayType instanceof ClassType || arrayType instanceof InterfaceType) {
                var intfType = ((IInterfaceType)arrayType);
                var isArrayType = Arrays.stream(this.main.currentFile.arrayTypes).anyMatch(x -> x.decl == intfType.getDecl());
                if (isArrayType && intfType.getTypeArguments().length > 0) {
                    ((ForeachStatement)stmt).itemVar.setType(intfType.getTypeArguments()[0]);
                    found = true;
                }
            }
            
            if (!found && !(arrayType instanceof AnyType))
                this.errorMan.throw_("Expected array as Foreach items variable, but got " + arrayType.repr());
            
            this.main.processBlock(((ForeachStatement)stmt).body);
            return true;
        }
        return false;
    }
}