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
import OneLang.StdLib.PackageManager.ImplementationPackage;

import OneLang.Generator.ProjectGenerator.ITemplateNode;
import OneLang.One.Ast.Expressions.Expression;
import OneLang.Generator.ProjectGenerator.TemplateBlock;
import OneLang.Generator.ProjectGenerator.ExprVM;
import OneLang.Generator.ProjectGenerator.ArrayValue;
import OneLang.Generator.ProjectGenerator.ObjectValue;

public class ForNode implements ITemplateNode {
    public String variableName;
    public Expression itemsExpr;
    public TemplateBlock body;
    
    public ForNode(String variableName, Expression itemsExpr, TemplateBlock body)
    {
        this.variableName = variableName;
        this.itemsExpr = itemsExpr;
        this.body = body;
    }
    
    public String format(ObjectValue model) {
        var items = new ExprVM(model).evaluate(this.itemsExpr);
        if (!(items instanceof ArrayValue))
            throw new Error("ForNode items (" + TSOverviewGenerator.preview.expr(this.itemsExpr) + ") return a non-array result!");
        
        var result = "";
        for (var item : (((ArrayValue)items)).items) {
            model.props.put(this.variableName, item);
            result += this.body.format(model);
        }
        /* unset model.props.get(this.variableName); */
        return result;
    }
}