from OneLangStdLib import *
import OneLang.One.Ast.Types as types
import OneLang.Utils.TSOverviewGenerator as tSOvervGen

class PackageStateCapture:
    def __init__(self, pkg):
        self.overviews = {}
        self.pkg = pkg
        for file in pkg.files.values():
            self.overviews[file.source_path.path] = tSOvervGen.TSOverviewGenerator(False, False).generate(file)
    
    def get_summary(self):
        return "\n\n".join(list(map(lambda file: f'''=== {file} ===\n\n{self.overviews.get(file)}''', self.overviews.keys())))