from OneLangStdLib import *
import OneLang.One.AstTransformer as astTrans
import OneLang.One.Ast.Types as types
import OneLang.One.ErrorManager as errorMan
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Ast.References as refs
import OneLang.One.Ast.Statements as stats
import OneLang.One.Ast.AstTypes as astTypes

class SymbolLookup:
    def __init__(self):
        self.error_man = errorMan.ErrorManager()
        self.level_stack = []
        self.level_names = []
        self.curr_level = None
        self.symbols = Map()
    
    def throw(self, msg):
        self.error_man.throw(f'''{msg} (context: {" > ".join(self.level_names)})''')
    
    def push_context(self, name):
        self.level_stack.append(self.curr_level)
        self.level_names.append(name)
        self.curr_level = []
    
    def add_symbol(self, name, ref):
        if self.symbols.has(name):
            self.throw(f'''Symbol shadowing: {name}''')
        self.symbols.set(name, ref)
        self.curr_level.append(name)
    
    def pop_context(self):
        for name in self.curr_level:
            self.symbols.delete(name)
        self.level_names.pop()
        self.curr_level = self.level_stack.pop() if len(self.level_stack) > 0 else None
    
    def get_symbol(self, name):
        return self.symbols.get(name)

class ResolveIdentifiers(astTrans.AstTransformer):
    def __init__(self):
        self.symbol_lookup = None
        super().__init__("ResolveIdentifiers")
        self.symbol_lookup = SymbolLookup()
    
    def visit_identifier(self, id):
        super().visit_identifier(id)
        symbol = self.symbol_lookup.get_symbol(id.text)
        if symbol == None:
            self.error_man.throw(f'''Identifier \'{id.text}\' was not found in available symbols''')
            return id
        
        ref = None
        if isinstance(symbol, types.Class) and id.text == "this":
            within_static_method = isinstance(self.current_method, types.Method) and self.current_method.is_static
            ref = refs.StaticThisReference(symbol) if within_static_method else refs.ThisReference(symbol)
        elif isinstance(symbol, types.Class) and id.text == "super":
            ref = refs.SuperReference(symbol)
        else:
            ref = symbol.create_reference()
            if ref == None:
                self.error_man.throw("createReference() should not return null!")
        ref.parent_node = id.parent_node
        return ref
    
    def visit_statement(self, stmt):
        if isinstance(stmt, stats.ForStatement):
            self.symbol_lookup.push_context(f'''For''')
            if stmt.item_var != None:
                self.symbol_lookup.add_symbol(stmt.item_var.name, stmt.item_var)
            super().visit_statement(stmt)
            self.symbol_lookup.pop_context()
        elif isinstance(stmt, stats.ForeachStatement):
            self.symbol_lookup.push_context(f'''Foreach''')
            self.symbol_lookup.add_symbol(stmt.item_var.name, stmt.item_var)
            super().visit_statement(stmt)
            self.symbol_lookup.pop_context()
        elif isinstance(stmt, stats.TryStatement):
            self.symbol_lookup.push_context(f'''Try''')
            self.visit_block(stmt.try_body)
            if stmt.catch_body != None:
                self.symbol_lookup.add_symbol(stmt.catch_var.name, stmt.catch_var)
                self.visit_block(stmt.catch_body)
                self.symbol_lookup.pop_context()
            if stmt.finally_body != None:
                self.visit_block(stmt.finally_body)
        else:
            return super().visit_statement(stmt)
        return stmt
    
    def visit_lambda(self, lambda_):
        self.symbol_lookup.push_context(f'''Lambda''')
        for param in lambda_.parameters:
            self.symbol_lookup.add_symbol(param.name, param)
        super().visit_block(lambda_.body)
        # directly process method's body without opening a new scope again
        self.symbol_lookup.pop_context()
        return lambda_
    
    def visit_block(self, block):
        self.symbol_lookup.push_context("block")
        super().visit_block(block)
        self.symbol_lookup.pop_context()
        return block
    
    def visit_variable_declaration(self, stmt):
        self.symbol_lookup.add_symbol(stmt.name, stmt)
        return super().visit_variable_declaration(stmt)
    
    def visit_method_base(self, method):
        self.symbol_lookup.push_context(f'''Method: {method.name}''' if isinstance(method, types.Method) else "constructor" if isinstance(method, types.Constructor) else "???")
        
        for param in method.parameters:
            self.symbol_lookup.add_symbol(param.name, param)
            if param.initializer != None:
                self.visit_expression(param.initializer)
        
        if method.body != None:
            super().visit_block(method.body)
        # directly process method's body without opening a new scope again
        
        self.symbol_lookup.pop_context()
    
    def visit_class(self, cls_):
        self.symbol_lookup.push_context(f'''Class: {cls_.name}''')
        self.symbol_lookup.add_symbol("this", cls_)
        if isinstance(cls_.base_class, astTypes.ClassType):
            self.symbol_lookup.add_symbol("super", cls_.base_class.decl)
        super().visit_class(cls_)
        self.symbol_lookup.pop_context()
    
    def visit_file(self, source_file):
        self.error_man.reset_context(self)
        self.symbol_lookup.push_context(f'''File: {source_file.source_path.to_string()}''')
        
        for symbol in source_file.available_symbols.values():
            if isinstance(symbol, types.Class):
                self.symbol_lookup.add_symbol(symbol.name, symbol)
            elif isinstance(symbol, types.Interface):
                pass
            elif isinstance(symbol, types.Enum):
                self.symbol_lookup.add_symbol(symbol.name, symbol)
            elif isinstance(symbol, types.GlobalFunction):
                self.symbol_lookup.add_symbol(symbol.name, symbol)
            else:
                pass
        
        super().visit_file(source_file)
        
        self.symbol_lookup.pop_context()
        self.error_man.reset_context()