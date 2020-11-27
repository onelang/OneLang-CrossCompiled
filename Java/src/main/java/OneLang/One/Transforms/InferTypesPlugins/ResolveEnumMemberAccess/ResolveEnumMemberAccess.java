package OneLang.One.Transforms.InferTypesPlugins.ResolveEnumMemberAccess;

import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Ast.References.EnumMemberReference;
import OneLang.One.Ast.References.EnumReference;

import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Ast.References.EnumReference;
import OneLang.One.Ast.Expressions.Expression;
import java.util.Arrays;
import io.onelang.std.core.Objects;
import OneLang.One.Ast.References.EnumMemberReference;

public class ResolveEnumMemberAccess extends InferTypesPlugin {
    public ResolveEnumMemberAccess()
    {
        super("ResolveEnumMemberAccess");
        
    }
    
    public Boolean canTransform(Expression expr) {
        return expr instanceof PropertyAccessExpression && ((PropertyAccessExpression)expr).object instanceof EnumReference;
    }
    
    public Expression transform(Expression expr) {
        var pa = ((PropertyAccessExpression)expr);
        var enumMemberRef = ((EnumReference)pa.object);
        var member = Arrays.stream(enumMemberRef.decl.values).filter(x -> Objects.equals(x.name, pa.propertyName)).findFirst().orElse(null);
        if (member == null) {
            this.errorMan.throw_("Enum member was not found: " + enumMemberRef.decl.getName() + "::" + pa.propertyName);
            return expr;
        }
        return new EnumMemberReference(member);
    }
    
    public Boolean canDetectType(Expression expr) {
        return expr instanceof EnumMemberReference;
    }
    
    public Boolean detectType(Expression expr) {
        expr.setActualType((((EnumMemberReference)expr)).decl.parentEnum.type, false, false);
        return true;
    }
}