using One.Ast;
using One.Transforms.InferTypesPlugins.Helpers;

namespace One.Transforms.InferTypesPlugins
{
    public class ResolveFieldAndPropertyAccess : InferTypesPlugin {
        public ResolveFieldAndPropertyAccess(): base("ResolveFieldAndPropertyAccess")
        {
            
        }
        
        protected Reference getStaticRef(Class cls, string memberName)
        {
            var field = cls.fields.find(x => x.name == memberName);
            if (field != null && field.isStatic)
                return new StaticFieldReference(field);
            
            var prop = cls.properties.find(x => x.name == memberName);
            if (prop != null && prop.isStatic)
                return new StaticPropertyReference(prop);
            
            this.errorMan.throw_($"Could not resolve static member access of a class: {cls.name}::{memberName}");
            return null;
        }
        
        protected Reference getInstanceRef(Class cls, string memberName, Expression obj)
        {
            while (true) {
                var field = cls.fields.find(x => x.name == memberName);
                if (field != null && !field.isStatic)
                    return new InstanceFieldReference(obj, field);
                
                var prop = cls.properties.find(x => x.name == memberName);
                if (prop != null && !prop.isStatic)
                    return new InstancePropertyReference(obj, prop);
                
                if (cls.baseClass == null)
                    break;
                
                cls = (((ClassType)cls.baseClass)).decl;
            }
            
            this.errorMan.throw_($"Could not resolve instance member access of a class: {cls.name}::{memberName}");
            return null;
        }
        
        protected Reference getInterfaceRef(Interface intf, string memberName, Expression obj)
        {
            var field = intf.fields.find(x => x.name == memberName);
            if (field != null && !field.isStatic)
                return new InstanceFieldReference(obj, field);
            
            foreach (var baseIntf in intf.baseInterfaces) {
                var res = this.getInterfaceRef((((InterfaceType)baseIntf)).decl, memberName, obj);
                if (res != null)
                    return res;
            }
            return null;
        }
        
        protected Expression transformPA(PropertyAccessExpression expr)
        {
            if (expr.object_ is ClassReference classRef)
                return this.getStaticRef(classRef.decl, expr.propertyName);
            
            if (expr.object_ is StaticThisReference statThisRef)
                return this.getStaticRef(statThisRef.cls, expr.propertyName);
            
            expr.object_ = this.main.runPluginsOn(expr.object_);
            
            if (expr.object_ is ThisReference thisRef)
                return this.getInstanceRef(thisRef.cls, expr.propertyName, thisRef);
            
            var type = expr.object_.getType();
            if (type is ClassType classType)
                return this.getInstanceRef(classType.decl, expr.propertyName, expr.object_);
            else if (type is InterfaceType intType) {
                var ref_ = this.getInterfaceRef(intType.decl, expr.propertyName, expr.object_);
                if (ref_ == null)
                    this.errorMan.throw_($"Could not resolve instance member access of a interface: {intType.repr()}::{expr.propertyName}");
                return ref_;
            }
            else if (type == null)
                this.errorMan.throw_($"Type was not inferred yet (prop=\"{expr.propertyName}\")");
            else if (type is AnyType)
                //this.errorMan.throw(`Object has any type (prop="${expr.propertyName}")`);
                expr.setActualType(AnyType.instance);
            else
                this.errorMan.throw_($"Expected class as variable type, but got: {type.repr()} (prop=\"{expr.propertyName}\")");
            
            return expr;
        }
        
        public override bool canTransform(Expression expr)
        {
            return expr is PropertyAccessExpression propAccExpr && !(propAccExpr.object_ is EnumReference) && !(propAccExpr.parentNode is UnresolvedCallExpression unrCallExpr && unrCallExpr.func == propAccExpr) && !(propAccExpr.actualType is AnyType);
        }
        
        public override Expression transform(Expression expr)
        {
            return this.transformPA(((PropertyAccessExpression)expr));
        }
        
        public override bool canDetectType(Expression expr)
        {
            return expr is InstanceFieldReference instFieldRef || expr is InstancePropertyReference || expr is StaticFieldReference || expr is StaticPropertyReference;
        }
        
        public override bool detectType(Expression expr)
        {
            if (expr is InstanceFieldReference instFieldRef2) {
                var actualType = GenericsResolver.fromObject(instFieldRef2.object_).resolveType(instFieldRef2.field.type, true);
                instFieldRef2.setActualType(actualType, false, TypeHelper.isGeneric(instFieldRef2.object_.actualType));
                return true;
            }
            else if (expr is InstancePropertyReference instPropRef) {
                var actualType = GenericsResolver.fromObject(instPropRef.object_).resolveType(instPropRef.property.type, true);
                instPropRef.setActualType(actualType);
                return true;
            }
            else if (expr is StaticPropertyReference statPropRef) {
                statPropRef.setActualType(statPropRef.decl.type, false, false);
                return true;
            }
            else if (expr is StaticFieldReference statFieldRef) {
                statFieldRef.setActualType(statFieldRef.decl.type, false, false);
                return true;
            }
            
            return false;
        }
    }
}