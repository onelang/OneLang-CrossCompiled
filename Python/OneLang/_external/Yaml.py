import yaml

class YamlValue:
    def __init__(self, value):
        self.value = value

    def dbl(self, key):
        return self.value.get(key)

    def str(self, key):
        return self.value.get(key)

    def str_arr(self, key):
        return self.value.get(key) or []

    def arr(self, key):
        return map(lambda x: YamlValue(x), self.value.get(key) or [])

    def obj(self, key):
        return YamlValue(self.value.get(key))

class Yaml:
    @staticmethod
    def load(content):
        return YamlValue(yaml.safe_load(content))