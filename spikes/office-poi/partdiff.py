"""Compare two OOXML packages part by part, after canonicalizing each XML part (C14N 2.0, whitespace
between elements ignored). Prints parts present in only one side and parts whose XML differs.

Usage: python3 -I partdiff.py <a> <b> [--show]
"""
import io
import sys
import zipfile
import xml.etree.ElementTree as ET


def canon(blob):
    try:
        return ET.canonicalize(blob.decode("utf-8-sig"), strip_text=True)
    except Exception:
        return blob


def parts(path):
    z = zipfile.ZipFile(path)
    return {n: z.read(n) for n in z.namelist() if not n.endswith("/")}


a, b = parts(sys.argv[1]), parts(sys.argv[2])
show = "--show" in sys.argv
for n in sorted(set(a) | set(b)):
    if n not in a:
        print(f"only-in-b {n}")
    elif n not in b:
        print(f"only-in-a {n}")
    elif a[n][:2] == b"PK" and b[n][:2] == b"PK":
        if a[n] != b[n]:
            print(f"embedded-differs {n}")
    elif canon(a[n]) != canon(b[n]):
        print(f"differs {n}")
        if show:
            print("  a:", canon(a[n])[:600])
            print("  b:", canon(b[n])[:600])
