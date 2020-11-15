from OneLangStdLib import *
import OneLang.One.AstTransformer as astTrans
import OneLang.One.Ast.Statements as stats
import OneLang.Utils.TSOverviewGenerator as tSOvervGen
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.Types as types

class StatementDebugger(astTrans.AstTransformer):
    def __init__(self, stmt_filter_regex):
        self.stmt_filter_regex = stmt_filter_regex
        super().__init__("StatementDebugger")
    
    def visit_expression(self, expr):
        # optimization: no need to process these...
        return None
    
    def visit_field(self, field):
        #if (field.name === "type" && field.parentInterface.name === "Interface") debugger;
        super().visit_field(field)
    
    def visit_statement(self, stmt):
        stmt_repr = tSOvervGen.TSOverviewGenerator.preview.stmt(stmt)
        # if (new RegExp(this.stmtFilterRegex).test(stmtRepr))
        #     debugger;
        return super().visit_statement(stmt)