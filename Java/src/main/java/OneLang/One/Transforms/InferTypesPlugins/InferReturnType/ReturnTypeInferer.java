package OneLang.One.Transforms.InferTypesPlugins.InferReturnType;

import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin.InferTypesPlugin;
import OneLang.One.Ast.Types.Property;
import OneLang.One.Ast.Types.Lambda;
import OneLang.One.Ast.Types.Method;
import OneLang.One.Ast.Types.IMethodBase;
import OneLang.One.Ast.AstTypes.VoidType;
import OneLang.One.Ast.AstTypes.AnyType;
import OneLang.One.Ast.AstTypes.LambdaType;
import OneLang.One.Ast.AstTypes.ClassType;
import OneLang.One.Ast.AstTypes.TypeHelper;
import OneLang.One.Ast.Statements.Statement;
import OneLang.One.Ast.Statements.ReturnStatement;
import OneLang.One.Ast.Statements.ThrowStatement;
import OneLang.One.ErrorManager.ErrorManager;
import OneLang.One.Ast.Expressions.NullLiteral;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Interfaces.IType;

import java.util.List;
import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.ErrorManager.ErrorManager;
import java.util.ArrayList;
import OneLang.One.Ast.Expressions.NullLiteral;
import OneLang.One.Ast.Expressions.Expression;
import OneStd.StdArrayHelper;
import java.util.stream.Collectors;
import java.util.Arrays;
import OneLang.One.Ast.AstTypes.ClassType;

public class ReturnTypeInferer {
    public Boolean returnsNull = false;
    public Boolean throws_ = false;
    public List<IType> returnTypes;
    public ErrorManager errorMan;
    
    public ReturnTypeInferer(ErrorManager errorMan)
    {
        this.errorMan = errorMan;
        this.returnTypes = new ArrayList<IType>();
    }
    
    public void addReturn(Expression returnValue) {
        if (returnValue instanceof NullLiteral) {
            this.returnsNull = true;
            return;
        }
        
        var returnType = returnValue.actualType;
        if (returnType == null)
            throw new Error("Return type cannot be null");
        
        if (!this.returnTypes.stream().anyMatch(x -> TypeHelper.equals(x, returnType)))
            this.returnTypes.add(returnType);
    }
    
    public IType finish(IType declaredType, String errorContext, ClassType asyncType) {
        IType inferredType = null;
        
        if (this.returnTypes.size() == 0) {
            if (this.throws_) {
                var result = declaredType;
                inferredType = result != null ? result : VoidType.instance;
            }
            else if (this.returnsNull) {
                if (declaredType != null)
                    inferredType = declaredType;
                else
                    this.errorMan.throw_(errorContext + " returns only null and it has no declared return type!");
            }
            else
                inferredType = VoidType.instance;
        }
        else if (this.returnTypes.size() == 1)
            inferredType = this.returnTypes.get(0);
        else if (declaredType != null && StdArrayHelper.allMatch(this.returnTypes, (x, i) -> TypeHelper.isAssignableTo(x, declaredType)))
            inferredType = declaredType;
        else {
            this.errorMan.throw_(errorContext + " returns different types: " + Arrays.stream(this.returnTypes.stream().map(x -> x.repr()).toArray(String[]::new)).collect(Collectors.joining(", ")));
            inferredType = AnyType.instance;
        }
        
        var checkType = declaredType;
        if (checkType != null && asyncType != null && checkType instanceof ClassType && ((ClassType)checkType).decl == asyncType.decl)
            checkType = ((ClassType)checkType).getTypeArguments()[0];
        
        if (checkType != null && !TypeHelper.isAssignableTo(inferredType, checkType))
            this.errorMan.throw_(errorContext + " returns different type (" + inferredType.repr() + ") than expected " + checkType.repr());
        
        this.returnTypes = null;
        return declaredType != null ? declaredType : inferredType;
    }
}