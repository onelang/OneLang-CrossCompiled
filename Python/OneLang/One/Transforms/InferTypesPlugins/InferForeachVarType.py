from onelang_core import *
import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin as inferTypesPlug
import OneLang.One.Ast.AstTypes as astTypes
import OneLang.One.Ast.Statements as stats

class InferForeachVarType(inferTypesPlug.InferTypesPlugin):
    def __init__(self):
        super().__init__("InferForeachVarType")
    
    def handle_statement(self, stmt):
        if isinstance(stmt, stats.ForeachStatement):
            stmt.items = self.main.run_plugins_on(stmt.items)
            array_type = stmt.items.get_type()
            found = False
            if isinstance(array_type, astTypes.ClassType) or isinstance(array_type, astTypes.InterfaceType):
                intf_type = array_type
                is_array_type = ArrayHelper.some(lambda x: x.decl == intf_type.get_decl(), self.main.current_file.array_types)
                if is_array_type and len(intf_type.type_arguments) > 0:
                    stmt.item_var.type = intf_type.type_arguments[0]
                    found = True
            
            if not found and not (isinstance(array_type, astTypes.AnyType)):
                self.error_man.throw(f'''Expected array as Foreach items variable, but got {array_type.repr()}''')
            
            self.main.process_block(stmt.body)
            return True
        return False