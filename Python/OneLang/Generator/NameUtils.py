from OneLangStdLib import *

class NameUtils:
    def __init__(self):
        pass
    
    @classmethod
    def short_name(cls, full_name):
        name_parts = []
        part_start_idx = 0
        i = 1
        
        while i < len(full_name):
            chr_code = ord(full_name[i])
            chr_is_upper = 65 <= chr_code and chr_code <= 90
            if chr_is_upper:
                name_parts.append(full_name[part_start_idx:i])
                part_start_idx = i
            i = i + 1
        name_parts.append(full_name[part_start_idx:])
        
        short_name_parts = []
        i = 0
        
        while i < len(name_parts):
            p = name_parts[i]
            if len(p) > 5:
                cut_point = 3
                
                while cut_point <= 4:
                    if p[cut_point] in "aeoiu":
                        break
                    cut_point = cut_point + 1
                p = p[0:0 + cut_point]
            short_name_parts.append(p.lower() if i == 0 else p)
            i = i + 1
        
        short_name = "".join(short_name_parts)
        if full_name.endswith("s") and not short_name.endswith("s"):
            short_name += "s"
        return short_name