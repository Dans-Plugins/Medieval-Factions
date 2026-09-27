#!/usr/bin/env python3
"""Check that the plugin jar only uses Bukkit API that exists on every supported version.

The plugin is compiled against one spigot-api (build.gradle), but runs on every version listed
in minecraft-versions.json. A reference to a field, method or class that an older (or newer)
API does not have compiles fine and then fails on that server: #2042 was
`PotionType.LONG_POISON`, which only exists from 1.20.5, so the plugin threw NoSuchFieldError
on enable on 1.19.4. A class that changed between enum and interface (`Attribute` in 1.21.3)
fails with IncompatibleClassChangeError instead.

For every supported version, this downloads that version's spigot-api jar and checks each
`org/bukkit` reference in the plugin's own classes (shaded libraries are skipped) the way the
JVM resolves it:

  class    the referenced class exists
  field    the field exists on the class, its superinterfaces or its superclasses
  method   the method exists on the class, its superclasses or its superinterfaces
  kind     a method referenced as a class method is on a class, and one referenced as an
           interface method is on an interface

Usage: check_api_compat.py <plugin.jar> [minecraft-versions.json]
Exits 1 and lists every problem, per version, when any reference does not resolve.
Only the standard library is used.
"""

import json
import os
import struct
import sys
import urllib.request
import zipfile

REPOSITORY = "https://hub.spigotmc.org/nexus/content/repositories/snapshots/org/spigotmc/spigot-api"
PLUGIN_PACKAGE = "com/dansplugins/factionsystem/"
SHADED_PACKAGE = "com/dansplugins/factionsystem/shadow/"
API_PACKAGE = "org/bukkit/"
ACC_INTERFACE = 0x0200
CACHE_DIR = os.environ.get("API_COMPAT_CACHE", os.path.join(os.path.expanduser("~"), ".cache", "mf-api-compat"))


# --- class file parsing --------------------------------------------------------------------

class ClassInfo:
    def __init__(self, name, is_interface, super_name, interfaces, fields, methods, refs, class_refs):
        self.name = name
        self.is_interface = is_interface
        self.super_name = super_name
        self.interfaces = interfaces
        self.fields = fields      # {(name, descriptor)}
        self.methods = methods    # {(name, descriptor)}
        self.refs = refs          # [(kind, owner, name, descriptor)]; kind: field | method | imethod
        self.class_refs = class_refs  # {internal class name}


def parse_class(data):
    """The parts of a class file this check needs (JVMS §4)."""
    if data[:4] != b"\xca\xfe\xba\xbe":
        raise ValueError("not a class file")
    pos = 8
    (count,) = struct.unpack_from(">H", data, pos)
    pos += 2
    pool = [None] * count
    i = 1
    while i < count:
        tag = data[pos]
        pos += 1
        if tag == 1:  # Utf8
            (length,) = struct.unpack_from(">H", data, pos)
            pool[i] = ("utf8", data[pos + 2:pos + 2 + length].decode("utf-8", errors="replace"))
            pos += 2 + length
        elif tag in (3, 4):  # Integer, Float
            pos += 4
        elif tag in (5, 6):  # Long, Double take two slots
            pos += 8
            i += 1
        elif tag == 7:  # Class
            pool[i] = ("class", struct.unpack_from(">H", data, pos)[0])
            pos += 2
        elif tag in (8, 16, 19, 20):  # String, MethodType, Module, Package
            pos += 2
        elif tag in (9, 10, 11):  # Fieldref, Methodref, InterfaceMethodref
            pool[i] = ({9: "field", 10: "method", 11: "imethod"}[tag],) + struct.unpack_from(">HH", data, pos)
            pos += 4
        elif tag == 12:  # NameAndType
            pool[i] = ("nat",) + struct.unpack_from(">HH", data, pos)
            pos += 4
        elif tag == 15:  # MethodHandle
            pos += 3
        elif tag in (17, 18):  # Dynamic, InvokeDynamic
            pos += 4
        else:
            raise ValueError(f"unknown constant pool tag {tag}")
        i += 1

    def utf8(index):
        return pool[index][1]

    def class_name(index):
        return utf8(pool[index][1]) if index else None

    access, this_index, super_index, n_interfaces = struct.unpack_from(">HHHH", data, pos)
    pos += 8
    interfaces = [class_name(struct.unpack_from(">H", data, pos + 2 * k)[0]) for k in range(n_interfaces)]
    pos += 2 * n_interfaces

    def members():
        nonlocal pos
        (n,) = struct.unpack_from(">H", data, pos)
        pos += 2
        out = set()
        for _ in range(n):
            _, name_index, desc_index, n_attributes = struct.unpack_from(">HHHH", data, pos)
            pos += 8
            for _ in range(n_attributes):
                (length,) = struct.unpack_from(">I", data, pos + 2)
                pos += 6 + length
            out.add((utf8(name_index), utf8(desc_index)))
        return out

    fields = members()
    methods = members()

    refs = []
    class_refs = set()
    for entry in pool:
        if not entry:
            continue
        if entry[0] in ("field", "method", "imethod"):
            _, nat_name, nat_desc = pool[entry[2]]
            refs.append((entry[0], class_name(entry[1]), utf8(nat_name), utf8(nat_desc)))
        elif entry[0] == "class":
            name = utf8(entry[1])
            # array descriptors ("[Lorg/bukkit/Material;") name their element class
            if name.startswith("["):
                name = name.lstrip("[")
                name = name[1:-1] if name.startswith("L") else None
            if name:
                class_refs.add(name)
    return ClassInfo(class_name(this_index), bool(access & ACC_INTERFACE), class_name(super_index),
                     interfaces, fields, methods, refs, class_refs)


def read_classes(jar_path, prefix, exclude=None):
    out = {}
    with zipfile.ZipFile(jar_path) as jar:
        for entry in jar.namelist():
            if entry.endswith(".class") and entry.startswith(prefix) and not (exclude and entry.startswith(exclude)):
                info = parse_class(jar.read(entry))
                out[info.name] = info
    return out


# --- the API side --------------------------------------------------------------------------

def api_jar(version):
    """The spigot-api jar for `version`, downloaded once into CACHE_DIR."""
    os.makedirs(CACHE_DIR, exist_ok=True)
    path = os.path.join(CACHE_DIR, f"spigot-api-{version}.jar")
    if os.path.exists(path):
        return path
    base = f"{REPOSITORY}/{version}-R0.1-SNAPSHOT"
    metadata = urllib.request.urlopen(f"{base}/maven-metadata.xml", timeout=60).read().decode()
    stamp = metadata.split("<value>", 1)[1].split("</value>", 1)[0]
    urllib.request.urlretrieve(f"{base}/spigot-api-{stamp}.jar", path + ".part")
    os.replace(path + ".part", path)
    return path


class Api:
    def __init__(self, classes):
        self.classes = classes

    def _supertypes(self, name):
        info = self.classes.get(name)
        if not info:
            return [], []
        return ([info.super_name] if info.super_name else []), list(info.interfaces)

    def has_field(self, owner, name, desc, seen=None):
        """JVMS §5.4.3.2: the class, then its superinterfaces, then its superclass."""
        seen = seen if seen is not None else set()
        if owner in seen:
            return False
        seen.add(owner)
        info = self.classes.get(owner)
        if info is None:
            # A supertype outside the API (java.lang.Enum, Object) declares no Bukkit field.
            # Treating it as "might have it" is exactly how an enum constant that is not there
            # would slip through.
            return False
        if (name, desc) in info.fields:
            return True
        supers, interfaces = self._supertypes(owner)
        return any(self.has_field(t, name, desc, seen) for t in interfaces + supers)

    def has_method(self, owner, name, desc, seen=None):
        """JVMS §5.4.3.3/§5.4.3.4: the class and its superclasses, then superinterfaces.
        A supertype outside the API (java.lang.Enum, Object, …) is taken to provide the
        method: those are the JDK's `values`-style members, not the API's."""
        seen = seen if seen is not None else set()
        if owner in seen:
            return False
        seen.add(owner)
        info = self.classes.get(owner)
        if info is None:
            return not owner.startswith(API_PACKAGE)
        if (name, desc) in info.methods:
            return True
        supers, interfaces = self._supertypes(owner)
        return any(self.has_method(t, name, desc, seen) for t in supers + interfaces)


def check(plugin, api):
    """Problems as (plugin class, message), sorted and de-duplicated."""
    problems = set()
    for info in plugin.values():
        where = info.name.rsplit("/", 1)[-1]
        for name in info.class_refs:
            if name.startswith(API_PACKAGE) and name not in api.classes:
                problems.add((where, f"class {name} does not exist"))
        for kind, owner, name, desc in info.refs:
            if not owner or not owner.startswith(API_PACKAGE):
                continue
            target = api.classes.get(owner)
            if target is None:
                continue  # reported as a missing class above
            if kind == "field":
                if not api.has_field(owner, name, desc):
                    problems.add((where, f"field {owner}.{name} does not exist"))
                continue
            if kind == "method" and target.is_interface:
                problems.add((where, f"{owner}.{name} is called as a class method, but {owner} is an interface"))
            elif kind == "imethod" and not target.is_interface:
                problems.add((where, f"{owner}.{name} is called as an interface method, but {owner} is a class"))
            elif not api.has_method(owner, name, desc):
                problems.add((where, f"method {owner}.{name}{desc} does not exist"))
    return sorted(problems)


def main(argv):
    if len(argv) < 2:
        print(__doc__.strip().splitlines()[0])
        print("usage: check_api_compat.py <plugin.jar> [minecraft-versions.json]")
        return 2
    jar = argv[1]
    versions_file = argv[2] if len(argv) > 2 else "minecraft-versions.json"
    with open(versions_file) as f:
        versions = json.load(f)["supported"]
    plugin = read_classes(jar, PLUGIN_PACKAGE, exclude=SHADED_PACKAGE)
    print(f"{os.path.basename(jar)}: {len(plugin)} classes; supported Minecraft versions: {', '.join(versions)}")
    failed = False
    for version in versions:
        api = Api(read_classes(api_jar(version), API_PACKAGE))
        problems = check(plugin, api)
        if problems:
            failed = True
            print(f"\n{version}: {len(problems)} reference(s) that do not resolve")
            for where, message in problems:
                print(f"  {where}: {message}")
        else:
            print(f"{version}: every Bukkit reference resolves")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
