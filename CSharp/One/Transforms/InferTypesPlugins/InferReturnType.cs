using One.Ast;
using One.Transforms.InferTypesPlugins.Helpers;
using One;
using System.Collections.Generic;

namespace One.Transforms.InferTypesPlugins
{
    public class ReturnTypeInferer {
        public bool returnsNull = false;
        public bool throws = false;
        public List<IType> returnTypes;
        public ErrorManager errorMan;
        
        public ReturnTypeInferer(ErrorManager errorMan)
        {
            this.errorMan = errorMan;
            this.returnTypes = new List<IType>();
        }
        
        public void addReturn(Expression returnValue)
        {
            if (returnValue is NullLiteral) {
                this.returnsNull = true;
                return;
            }
            
            var returnType = returnValue.actualType;
            if (returnType == null)
                throw new Error("Return type cannot be null");
            
            if (!this.returnTypes.some(x => TypeHelper.equals(x, returnType)))
                this.returnTypes.push(returnType);
        }
        
        public IType finish(IType declaredType, string errorContext, ClassType asyncType)
        {
            IType inferredType = null;
            
            if (this.returnTypes.length() == 0) {
                if (this.throws)
                    inferredType = declaredType ?? VoidType.instance;
                else if (this.returnsNull) {
                    if (declaredType != null)
                        inferredType = declaredType;
                    else
                        this.errorMan.throw_($"{errorContext} returns only null and it has no declared return type!");
                }
                else
                    inferredType = VoidType.instance;
            }
            else if (this.returnTypes.length() == 1)
                inferredType = this.returnTypes.get(0);
            else if (declaredType != null && this.returnTypes.every((x, i) => TypeHelper.isAssignableTo(x, declaredType)))
                inferredType = declaredType;
            else {
                this.errorMan.throw_($"{errorContext} returns different types: {this.returnTypes.map(x => x.repr()).join(", ")}");
                inferredType = AnyType.instance;
            }
            
            var checkType = declaredType;
            if (checkType != null && asyncType != null && checkType is ClassType classType && classType.decl == asyncType.decl)
                checkType = classType.typeArguments.get(0);
            
            if (checkType != null && !TypeHelper.isAssignableTo(inferredType, checkType))
                this.errorMan.throw_($"{errorContext} returns different type ({inferredType.repr()}) than expected {checkType.repr()}");
            
            this.returnTypes = null;
            return declaredType != null ? declaredType : inferredType;
        }
    }
    
    public class InferReturnType : InferTypesPlugin {
        public List<ReturnTypeInferer> returnTypeInfer;
        
        public ReturnTypeInferer current {
            get {
            
                return this.returnTypeInfer.get(this.returnTypeInfer.length() - 1);
            }
        }
        
        public InferReturnType(): base("InferReturnType")
        {
            this.returnTypeInfer = new List<ReturnTypeInferer>();
        }
        
        public void start()
        {
            this.returnTypeInfer.push(new ReturnTypeInferer(this.errorMan));
        }
        
        public IType finish(IType declaredType, string errorContext, ClassType asyncType)
        {
            return this.returnTypeInfer.pop().finish(declaredType, errorContext, asyncType);
        }
        
        public override bool handleStatement(Statement stmt)
        {
            if (this.returnTypeInfer.length() == 0)
                return false;
            if (stmt is ReturnStatement retStat && retStat.expression != null) {
                this.main.processStatement(retStat);
                this.current.addReturn(retStat.expression);
                return true;
            }
            else if (stmt is ThrowStatement) {
                this.current.throws = true;
                return false;
            }
            else
                return false;
        }
        
        public override bool handleLambda(Lambda lambda)
        {
            this.start();
            this.main.processLambda(lambda);
            lambda.returns = this.finish(lambda.returns, "Lambda", null);
            lambda.setActualType(new LambdaType(lambda.parameters, lambda.returns), false, true);
            return true;
        }
        
        public override bool handleMethod(IMethodBase method)
        {
            if (method is Method meth && meth.body != null) {
                this.start();
                this.main.processMethodBase(meth);
                meth.returns = this.finish(meth.returns, $"Method \"{meth.name}\"", meth.async ? this.main.currentFile.literalTypes.promise : null);
                return true;
            }
            else
                return false;
        }
        
        public override bool handleProperty(Property prop)
        {
            this.main.processVariable(prop);
            
            if (prop.getter != null) {
                this.start();
                this.main.processBlock(prop.getter);
                prop.type = this.finish(prop.type, $"Property \"{prop.name}\" getter", null);
            }
            
            if (prop.setter != null) {
                this.start();
                this.main.processBlock(prop.setter);
                this.finish(VoidType.instance, $"Property \"{prop.name}\" setter", null);
            }
            
            return true;
        }
    }
}