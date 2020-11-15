package OneLang.One.Transforms.ResolveUnresolvedTypes;

import OneLang.One.AstTransformer.AstTransformer;
import OneLang.One.Ast.AstTypes.UnresolvedType;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.InterfaceType;
import OneLang.One.Ast.AstTypes.EnumType;
import OneLang.One.Ast.AstTypes.GenericsType;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.Interface;
import OneLang.One.Ast.Types.Enum;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.UnresolvedNewExpression;
import OneLang.One.Ast.Expressions.NewExpression;
import OneLang.One.Ast.Interfaces.IType;

import OneLang.One.AstTransformer.AstTransformer;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.AstTypes.UnresolvedType;
import java.util.Arrays;
import OneLang.One.Ast.AstTypes.GenericsType;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.Types.Interface;
import OneLang.One.Ast.AstTypes.InterfaceType;
import OneLang.One.Ast.Types.Enum;
import OneLang.One.Ast.AstTypes.EnumType;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.UnresolvedNewExpression;
import OneLang.One.Ast.Expressions.NewExpression;

public class ResolveUnresolvedTypes extends AstTransformer {
    public ResolveUnresolvedTypes()
    {
        super("ResolveUnresolvedTypes");
        
    }
    
    protected IType visitType(IType type) {
        super.visitType(type);
        if (type instanceof UnresolvedType) {
            if (this.currentInterface != null && Arrays.stream(this.currentInterface.getTypeArguments()).anyMatch(((UnresolvedType)type).typeName::equals))
                return new GenericsType(((UnresolvedType)type).typeName);
            
            var symbol = this.currentFile.availableSymbols.get(((UnresolvedType)type).typeName);
            if (symbol == null) {
                this.errorMan.throw_("Unresolved type '" + ((UnresolvedType)type).typeName + "' was not found in available symbols");
                return ((UnresolvedType)type);
            }
            
            if (symbol instanceof Class)
                return new ClassType(((Class)symbol), ((UnresolvedType)type).getTypeArguments());
            else if (symbol instanceof Interface)
                return new InterfaceType(((Interface)symbol), ((UnresolvedType)type).getTypeArguments());
            else if (symbol instanceof Enum)
                return new EnumType(((Enum)symbol));
            else {
                this.errorMan.throw_("Unknown symbol type: " + symbol);
                return ((UnresolvedType)type);
            }
        }
        else
            return type;
    }
    
    protected Expression visitExpression(Expression expr) {
        if (expr instanceof UnresolvedNewExpression) {
            var clsType = this.visitType(((UnresolvedNewExpression)expr).cls);
            if (clsType instanceof ClassType) {
                var newExpr = new NewExpression(((ClassType)clsType), ((UnresolvedNewExpression)expr).args);
                newExpr.parentNode = ((UnresolvedNewExpression)expr).parentNode;
                super.visitExpression(newExpr);
                return newExpr;
            }
            else {
                this.errorMan.throw_("Excepted ClassType, but got " + clsType);
                return ((UnresolvedNewExpression)expr);
            }
        }
        else
            return super.visitExpression(expr);
    }
}