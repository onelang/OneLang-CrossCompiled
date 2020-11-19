package OneLang.Generator.ProjectGenerator;

import OneStd.OneFile;
import OneStd.OneYaml;
import OneStd.YamlValue;
import OneStd.OneJObject;
import OneStd.OneJson;
import OneStd.OneJValue;
import OneLang.Parsers.Common.Reader.Reader;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.One.Ast.Expressions.Identifier;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Compiler.Compiler;
import OneLang.Generator.IGenerator.IGenerator;
import OneLang.Parsers.Common.ExpressionParser.ExpressionParser;
import OneLang.Utils.TSOverviewGenerator.TSOverviewGenerator;
import OneLang.Generator.JavaGenerator.JavaGenerator;
import OneLang.Generator.CsharpGenerator.CsharpGenerator;
import OneLang.Generator.PythonGenerator.PythonGenerator;
import OneLang.Generator.PhpGenerator.PhpGenerator;
import OneLang.One.CompilerHelper.CompilerHelper;

import OneLang.Generator.ProjectGenerator.ObjectValue;
import OneLang.Generator.ProjectGenerator.IVMValue;
import OneLang.One.Ast.Expressions.Identifier;
import OneLang.One.Ast.Expressions.PropertyAccessExpression;
import OneLang.One.Ast.Expressions.Expression;

public class ExprVM {
    public ObjectValue model;
    
    public ExprVM(ObjectValue model)
    {
        this.model = model;
    }
    
    public static IVMValue propAccess(IVMValue obj, String propName) {
        if (!(obj instanceof ObjectValue))
            throw new Error("You can only access a property of an object!");
        if (!((((ObjectValue)obj)).props.containsKey(propName)))
            throw new Error("Property '" + propName + "' does not exists on this object!");
        return (((ObjectValue)obj)).props.get(propName);
    }
    
    public IVMValue evaluate(Expression expr) {
        if (expr instanceof Identifier)
            return ExprVM.propAccess(this.model, ((Identifier)expr).text);
        else if (expr instanceof PropertyAccessExpression) {
            var objValue = this.evaluate(((PropertyAccessExpression)expr).object);
            return ExprVM.propAccess(objValue, ((PropertyAccessExpression)expr).propertyName);
        }
        else
            throw new Error("Unsupported expression!");
    }
}