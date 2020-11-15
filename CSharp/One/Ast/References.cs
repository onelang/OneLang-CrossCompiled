using One.Ast;

namespace One.Ast
{
    public interface IReferencable {
        Reference createReference();
    }
    
    public interface IGetMethodBase {
        IMethodBase getMethodBase();
    }
    
    public class Reference : Expression {
        
    }
    
    public class VariableReference : Reference {
        public virtual IVariable getVariable()
        {
            throw new Error("Abstract method");
        }
    }
    
    public class ClassReference : Reference {
        public Class decl;
        
        public ClassReference(Class decl): base()
        {
            this.decl = decl;
            decl.classReferences.push(this);
        }
        
        public override void setActualType(IType type, bool allowVoid = false, bool allowGeneric = false)
        {
            throw new Error("ClassReference cannot have a type!");
        }
    }
    
    public class GlobalFunctionReference : Reference, IGetMethodBase {
        public GlobalFunction decl;
        
        public GlobalFunctionReference(GlobalFunction decl): base()
        {
            this.decl = decl;
            decl.references.push(this);
        }
        
        public override void setActualType(IType type, bool allowVoid = false, bool allowGeneric = false)
        {
            throw new Error("GlobalFunctionReference cannot have a type!");
        }
        
        public IMethodBase getMethodBase()
        {
            return this.decl;
        }
    }
    
    public class MethodParameterReference : VariableReference {
        public MethodParameter decl;
        
        public MethodParameterReference(MethodParameter decl): base()
        {
            this.decl = decl;
            decl.references.push(this);
        }
        
        public override void setActualType(IType type, bool allowVoid = false, bool allowGeneric = false)
        {
            base.setActualType(type, false, this.decl.parentMethod is Lambda lambd ? lambd.parameters.some(x => TypeHelper.isGeneric(x.type)) : this.decl.parentMethod is Constructor const_ ? const_.parentClass.typeArguments.length() > 0 : this.decl.parentMethod is Method meth ? meth.typeArguments.length() > 0 || meth.parentInterface.typeArguments.length() > 0 : false);
        }
        
        public override IVariable getVariable()
        {
            return this.decl;
        }
    }
    
    public class EnumReference : Reference {
        public Enum_ decl;
        
        public EnumReference(Enum_ decl): base()
        {
            this.decl = decl;
            decl.references.push(this);
        }
        
        public override void setActualType(IType type, bool allowVoid = false, bool allowGeneric = false)
        {
            throw new Error("EnumReference cannot have a type!");
        }
    }
    
    public class EnumMemberReference : Reference {
        public EnumMember decl;
        
        public EnumMemberReference(EnumMember decl): base()
        {
            this.decl = decl;
            decl.references.push(this);
        }
        
        public override void setActualType(IType type, bool allowVoid = false, bool allowGeneric = false)
        {
            if (!(type is EnumType))
                throw new Error("Expected EnumType!");
            base.setActualType(type);
        }
    }
    
    public class StaticThisReference : Reference {
        public Class cls;
        
        public StaticThisReference(Class cls): base()
        {
            this.cls = cls;
            cls.staticThisReferences.push(this);
        }
        
        public override void setActualType(IType type, bool allowVoid = false, bool allowGeneric = false)
        {
            throw new Error("StaticThisReference cannot have a type!");
        }
    }
    
    public class ThisReference : Reference {
        public Class cls;
        
        public ThisReference(Class cls): base()
        {
            this.cls = cls;
            cls.thisReferences.push(this);
        }
        
        public override void setActualType(IType type, bool allowVoid = false, bool allowGeneric = false)
        {
            if (!(type is ClassType))
                throw new Error("Expected ClassType!");
            base.setActualType(type, false, this.cls.typeArguments.length() > 0);
        }
    }
    
    public class SuperReference : Reference {
        public Class cls;
        
        public SuperReference(Class cls): base()
        {
            this.cls = cls;
            cls.superReferences.push(this);
        }
        
        public override void setActualType(IType type, bool allowVoid = false, bool allowGeneric = false)
        {
            if (!(type is ClassType))
                throw new Error("Expected ClassType!");
            base.setActualType(type, false, this.cls.typeArguments.length() > 0);
        }
    }
    
    public class VariableDeclarationReference : VariableReference {
        public VariableDeclaration decl;
        
        public VariableDeclarationReference(VariableDeclaration decl): base()
        {
            this.decl = decl;
            decl.references.push(this);
        }
        
        public override IVariable getVariable()
        {
            return this.decl;
        }
    }
    
    public class ForVariableReference : VariableReference {
        public ForVariable decl;
        
        public ForVariableReference(ForVariable decl): base()
        {
            this.decl = decl;
            decl.references.push(this);
        }
        
        public override IVariable getVariable()
        {
            return this.decl;
        }
    }
    
    public class CatchVariableReference : VariableReference {
        public CatchVariable decl;
        
        public CatchVariableReference(CatchVariable decl): base()
        {
            this.decl = decl;
            decl.references.push(this);
        }
        
        public override IVariable getVariable()
        {
            return this.decl;
        }
    }
    
    public class ForeachVariableReference : VariableReference {
        public ForeachVariable decl;
        
        public ForeachVariableReference(ForeachVariable decl): base()
        {
            this.decl = decl;
            decl.references.push(this);
        }
        
        public override IVariable getVariable()
        {
            return this.decl;
        }
    }
    
    public class StaticFieldReference : VariableReference {
        public Field decl;
        
        public StaticFieldReference(Field decl): base()
        {
            this.decl = decl;
            decl.staticReferences.push(this);
        }
        
        public override void setActualType(IType type, bool allowVoid = false, bool allowGeneric = false)
        {
            if (TypeHelper.isGeneric(type))
                throw new Error("StaticField's type cannot be Generic");
            base.setActualType(type);
        }
        
        public override IVariable getVariable()
        {
            return this.decl;
        }
    }
    
    public class StaticPropertyReference : VariableReference {
        public Property decl;
        
        public StaticPropertyReference(Property decl): base()
        {
            this.decl = decl;
            decl.staticReferences.push(this);
        }
        
        public override void setActualType(IType type, bool allowVoid = false, bool allowGeneric = false)
        {
            if (TypeHelper.isGeneric(type))
                throw new Error("StaticProperty's type cannot be Generic");
            base.setActualType(type);
        }
        
        public override IVariable getVariable()
        {
            return this.decl;
        }
    }
    
    public class InstanceFieldReference : VariableReference {
        public Expression object_;
        public Field field;
        
        public InstanceFieldReference(Expression object_, Field field): base()
        {
            this.object_ = object_;
            this.field = field;
            field.instanceReferences.push(this);
        }
        
        public override IVariable getVariable()
        {
            return this.field;
        }
    }
    
    public class InstancePropertyReference : VariableReference {
        public Expression object_;
        public Property property;
        
        public InstancePropertyReference(Expression object_, Property property): base()
        {
            this.object_ = object_;
            this.property = property;
            property.instanceReferences.push(this);
        }
        
        public override IVariable getVariable()
        {
            return this.property;
        }
    }
}