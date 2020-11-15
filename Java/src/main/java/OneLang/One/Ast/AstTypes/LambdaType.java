package OneLang.One.Ast.AstTypes;

import OneLang.One.Ast.Types.Enum;
import OneLang.One.Ast.Types.Interface;
import OneLang.One.Ast.Types.Class;
import OneLang.One.Ast.Types.MethodParameter;
import OneLang.One.Ast.Types.IInterface;
import OneLang.One.Ast.Interfaces.IType;

import OneLang.One.Ast.Interfaces.IType;
import OneLang.One.Ast.Types.MethodParameter;
import java.util.Arrays;
import java.util.stream.Collectors;

public class LambdaType implements IType {
    public MethodParameter[] parameters;
    public IType returnType;
    
    public LambdaType(MethodParameter[] parameters, IType returnType)
    {
        this.parameters = parameters;
        this.returnType = returnType;
    }
    
    public String repr() {
        return "L:(" + Arrays.stream(Arrays.stream(this.parameters).map(x -> x.getType().repr()).toArray(String[]::new)).collect(Collectors.joining(", ")) + ")=>" + this.returnType.repr();
    }
}