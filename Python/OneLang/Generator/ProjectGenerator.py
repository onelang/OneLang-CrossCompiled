from OneLangStdLib import *
from OneFile import *
from OneYaml import *
from OneJson import *
import OneLang.Parsers.Common.Reader as read
import OneLang.One.Ast.Expressions as exprs
import OneLang.One.Compiler as comp
import OneLang.Generator.IGenerator as iGen
import OneLang.Parsers.Common.ExpressionParser as exprPars
import OneLang.Utils.TSOverviewGenerator as tSOvervGen
import OneLang.Generator.JavaGenerator as javaGen
import OneLang.Generator.CsharpGenerator as cshGen
import OneLang.Generator.PythonGenerator as pythGen
import OneLang.Generator.PhpGenerator as phpGen
import OneLang.One.CompilerHelper as compHelp
import re

class ProjectTemplateMeta:
    def __init__(self, language, destionation_dir, template_files):
        self.language = language
        self.destionation_dir = destionation_dir
        self.template_files = template_files
    
    @classmethod
    def from_yaml(cls, obj):
        return ProjectTemplateMeta(obj.str("language"), obj.str("destination-dir"), obj.str_arr("template-files"))

class ObjectValue:
    def __init__(self, props):
        self.props = props

class StringValue:
    def __init__(self, value):
        self.value = value

class ArrayValue:
    def __init__(self, items):
        self.items = items

class TemplateBlock:
    def __init__(self, items):
        self.items = items
    
    def format(self, model):
        return "".join(list(map(lambda x: x.format(model), self.items)))

class LiteralNode:
    def __init__(self, value):
        self.value = value
    
    def format(self, model):
        return self.value

class ExprVM:
    def __init__(self, model):
        self.model = model
    
    @classmethod
    def prop_access(cls, obj, prop_name):
        if not (isinstance(obj, ObjectValue)):
            raise Error("You can only access a property of an object!")
        if not (prop_name in (obj).props):
            raise Error(f'''Property \'{prop_name}\' does not exists on this object!''')
        return (obj).props.get(prop_name)
    
    def evaluate(self, expr):
        if isinstance(expr, exprs.Identifier):
            return ExprVM.prop_access(self.model, expr.text)
        elif isinstance(expr, exprs.PropertyAccessExpression):
            obj_value = self.evaluate(expr.object)
            return ExprVM.prop_access(obj_value, expr.property_name)
        else:
            raise Error("Unsupported expression!")

class ExpressionNode:
    def __init__(self, expr):
        self.expr = expr
    
    def format(self, model):
        result = ExprVM(model).evaluate(self.expr)
        if isinstance(result, StringValue):
            return result.value
        else:
            raise Error(f'''ExpressionNode ({tSOvervGen.TSOverviewGenerator.preview.expr(self.expr)}) return a non-string result!''')

class ForNode:
    def __init__(self, variable_name, items_expr, body):
        self.variable_name = variable_name
        self.items_expr = items_expr
        self.body = body
    
    def format(self, model):
        items = ExprVM(model).evaluate(self.items_expr)
        if not (isinstance(items, ArrayValue)):
            raise Error(f'''ForNode items ({tSOvervGen.TSOverviewGenerator.preview.expr(self.items_expr)}) return a non-array result!''')
        
        result = ""
        for item in (items).items:
            model.props[self.variable_name] = item
            result += self.body.format(model)
        /* unset model.props.get(self.variable_name); */
        return result

class TemplateParser:
    def __init__(self, template):
        self.reader = None
        self.expr_parser = None
        self.template = template
        self.reader = read.Reader(template)
        self.expr_parser = exprPars.ExpressionParser(self.reader)
    
    def parse_block(self):
        items = []
        while not self.reader.get_eof():
            if self.reader.peek_token("{{/"):
                break
            if self.reader.read_token("{{"):
                if self.reader.read_token("for"):
                    var_name = self.reader.read_identifier()
                    self.reader.expect_token("of")
                    items_expr = self.expr_parser.parse()
                    self.reader.expect_token("}}")
                    body = self.parse_block()
                    self.reader.expect_token("{{/for}}")
                    items.append(ForNode(var_name, items_expr, body))
                else:
                    expr = self.expr_parser.parse()
                    items.append(ExpressionNode(expr))
                    self.reader.expect_token("}}")
            else:
                literal = self.reader.read_until("{{", True)
                if literal.endswith("\\"):
                    literal = literal[0:len(literal) - 1] + "{{"
                if literal != "":
                    items.append(LiteralNode(literal))
        return TemplateBlock(items)
    
    def parse(self):
        return self.parse_block()

class TemplateFile:
    def __init__(self, template):
        self.main = None
        self.template = template
        self.main = TemplateParser(template).parse()
    
    def format(self, model):
        return self.main.format(model)

class ProjectTemplate:
    def __init__(self, template_dir):
        self.meta = None
        self.src_files = None
        self.template_dir = template_dir
        self.meta = ProjectTemplateMeta.from_yaml(OneYaml.load(OneFile.read_text(f'''{template_dir}/index.yaml''')))
        self.src_files = OneFile.list_files(f'''{template_dir}/src''', True)
    
    def generate(self, dst_dir, model):
        for fn in self.src_files:
            src_fn = f'''{self.template_dir}/src/{fn}'''
            dst_fn = f'''{dst_dir}/{fn}'''
            if fn in self.meta.template_files:
                tmpl_file = TemplateFile(OneFile.read_text(src_fn))
                dst_file = tmpl_file.format(model)
                OneFile.write_text(dst_fn, dst_file)
            else:
                OneFile.copy(src_fn, dst_fn)

class ProjectDependency:
    def __init__(self, name):
        self.name = name

class OneProjectFile:
    def __init__(self, name, dependencies, source_dir, source_lang, output_dir, project_templates):
        self.name = name
        self.dependencies = dependencies
        self.source_dir = source_dir
        self.source_lang = source_lang
        self.output_dir = output_dir
        self.project_templates = project_templates
    
    @classmethod
    def from_json(cls, json):
        return OneProjectFile(json.get("name").as_string(), list(map(lambda dep: ProjectDependency(dep.get("name").as_string()), list(map(lambda dep: dep.as_object(), json.get("dependencies").get_array_items())))), json.get("sourceDir").as_string(), json.get("sourceLang").as_string(), json.get("outputDir").as_string(), list(map(lambda x: x.as_string(), json.get("projectTemplates").get_array_items())))

class ProjectGenerator:
    def __init__(self, base_dir, proj_dir):
        self.project_file = None
        self.src_dir = None
        self.out_dir = None
        self.base_dir = base_dir
        self.proj_dir = proj_dir
        self.project_file = OneProjectFile.from_json(OneJson.parse(OneFile.read_text(f'''{proj_dir}/one.json''')).as_object())
        self.src_dir = f'''{self.proj_dir}/{self.project_file.source_dir}'''
        self.out_dir = f'''{self.proj_dir}/{self.project_file.output_dir}'''
    
    def generate(self):
        generators = [javaGen.JavaGenerator(), cshGen.CsharpGenerator(), pythGen.PythonGenerator(), phpGen.PhpGenerator()]
        for tmpl_name in self.project_file.project_templates:
            compiler = compHelp.CompilerHelper.init_project(self.project_file.name, self.src_dir, self.project_file.source_lang, None)
            compiler.process_workspace()
            
            proj_template = ProjectTemplate(f'''{self.base_dir}/project-templates/{tmpl_name}''')
            lang_id = proj_template.meta.language
            generator = next(filter(lambda x: x.get_lang_name().lower() == lang_id, generators), None)
            lang_name = generator.get_lang_name()
            
            for trans in generator.get_transforms():
                trans.visit_files(compiler.project_pkg.files.values())
            
            console.log(f'''Generating {lang_name} code...''')
            files = generator.generate(compiler.project_pkg)
            for file in files:
                OneFile.write_text(f'''{self.out_dir}/{lang_name}/{proj_template.meta.destionation_dir or ""}/{file.path}''', file.content)
            
            native_deps = {}
            for dep in self.project_file.dependencies:
                impl = next(filter(lambda x: x.content.id.name == dep.name, compiler.pac_man.implementation_pkgs), None)
                lang_data = next(filter(lambda x: x.id == lang_id, impl.implementation_yaml.languages), None)
                if lang_data == None:
                    continue
                for nat_dep in lang_data.native_dependencies or []:
                    native_deps[nat_dep.name] = nat_dep.version
            
            one_deps = dict()
            one_deps["OneCore"] = None
            for file in compiler.project_pkg.files.values():
                for imp in list(filter(lambda x: x.export_scope.package_name != compiler.project_pkg.name, file.imports)):
                    one_deps[re.sub("\\.", "", re.split("-", imp.export_scope.package_name)[0])] = None
            
            model = ObjectValue({
                "dependencies": ArrayValue(list(map(lambda name: ObjectValue({
                    "name": StringValue(name),
                    "version": StringValue(native_deps.get(name))
                }), native_deps.keys()))),
                "onepackages": ArrayValue(list(map(lambda dep: ObjectValue({
                    "name": StringValue(dep)
                }), Array.from_(one_deps.keys()))))
            })
            proj_template.generate(f'''{self.out_dir}/{lang_name}''', model)