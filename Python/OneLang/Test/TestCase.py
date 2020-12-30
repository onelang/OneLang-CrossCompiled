from onelang_core import *

class TestCase:
    def __init__(self, name, action):
        self.name = name
        self.action = action

class SyncTestCase(TestCase):
    def __init__(self, name, sync_action):
        self.sync_action = sync_action
        super().__init__(name, None)
        self.action = lambda artifact_dir: self.execute(artifact_dir)
    
    def execute(self, artifact_dir):
        self.sync_action(artifact_dir)