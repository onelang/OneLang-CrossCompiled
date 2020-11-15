from OneLangStdLib import *
import OneLang.One.Transforms.InferTypesPlugins.Helpers.InferTypesPlugin as inferTypesPlug
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.AstTypes as astTypes
import OneLang.One.Ast.Interfaces as ints

class ArrayAndMapLiteralTypeInfer(inferTypesPlug.InferTypesPlugin):
    def __init__(self):
        super().__init__("ArrayAndMapLiteralTypeInfer")
    
    def infer_array_or_map_item_type(self, items, expected_type, is_map):
        item_types = []
        for item in items:
            if not ArrayHelper.some(lambda t: astTypes.TypeHelper.equals(t, item.get_type()), item_types):
                item_types.append(item.get_type())
        
        literal_type = self.main.current_file.literal_types.map if is_map else self.main.current_file.literal_types.array
        
        item_type = None
        if len(item_types) == 0:
            if expected_type == None:
                self.error_man.warn(f'''Could not determine the type of an empty {("MapLiteral" if is_map else "ArrayLiteral")}, using AnyType instead''')
                item_type = astTypes.AnyType.instance
            elif isinstance(expected_type, astTypes.ClassType) and expected_type.decl == literal_type.decl:
                item_type = expected_type.type_arguments[0]
            else:
                item_type = astTypes.AnyType.instance
        elif len(item_types) == 1:
            item_type = item_types[0]
        elif not (isinstance(expected_type, astTypes.AnyType)):
            self.error_man.warn(f'''Could not determine the type of {("a MapLiteral" if is_map else "an ArrayLiteral")}! Multiple types were found: {", ".join(list(map(lambda x: x.repr(), item_types)))}, using AnyType instead''')
            item_type = astTypes.AnyType.instance
        return item_type
    
    def can_detect_type(self, expr):
        return isinstance(expr, exprs.ArrayLiteral) or isinstance(expr, exprs.MapLiteral)
    
    def detect_type(self, expr):
        # make this work: `<{ [name: string]: SomeObject }> {}`
        if isinstance(expr.parent_node, exprs.CastExpression):
            expr.set_expected_type(expr.parent_node.new_type)
        elif isinstance(expr.parent_node, exprs.BinaryExpression) and expr.parent_node.operator == "=" and expr.parent_node.right == expr:
            expr.set_expected_type(expr.parent_node.left.actual_type)
        elif isinstance(expr.parent_node, exprs.ConditionalExpression) and (expr.parent_node.when_true == expr or expr.parent_node.when_false == expr):
            expr.set_expected_type(expr.parent_node.when_false.actual_type if expr.parent_node.when_true == expr else expr.parent_node.when_true.actual_type)
        
        if isinstance(expr, exprs.ArrayLiteral):
            item_type = self.infer_array_or_map_item_type(expr.items, expr.expected_type, False)
            expr.set_actual_type(astTypes.ClassType(self.main.current_file.literal_types.array.decl, [item_type]))
        elif isinstance(expr, exprs.MapLiteral):
            item_type = self.infer_array_or_map_item_type(list(map(lambda x: x.value, expr.items)), expr.expected_type, True)
            expr.set_actual_type(astTypes.ClassType(self.main.current_file.literal_types.map.decl, [item_type]))
        
        return True