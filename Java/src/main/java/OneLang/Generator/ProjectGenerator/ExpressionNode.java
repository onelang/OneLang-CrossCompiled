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
import OneLang.Generator.ProjectGenerator.ExprVM;
import OneLang.Generator.ProjectGenerator.StringValue;
import OneLang.Generator.ProjectGenerator.ObjectValue;

public class ExpressionNode implements ITemplateNode {
    public Expression expr;
    
    public ExpressionNode(Expression expr)
    {
        this.expr = expr;
    }
    
    public String format(ObjectValue model) {
        var result = new ExprVM(model).evaluate(this.expr);
        if (result instanceof StringValue)
            return ((StringValue)result).value;
        else
            throw new Error("ExpressionNode (" + TSOverviewGenerator.preview.expr(this.expr) + ") return a non-string result!");
    }
}