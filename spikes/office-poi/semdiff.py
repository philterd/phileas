"""Namespace-aware semantic comparison of two OOXML packages. Element order is significant except in
[Content_Types].xml and .rels parts, whose entries are compared as sets. Recurses into embedded packages."""
import io, sys, zipfile
import xml.etree.ElementTree as ET

def norm(e):
    text = (e.text or "").strip()
    return (e.tag, tuple(sorted(e.attrib.items())), text, tuple(norm(c) for c in e))

def cmp_pkg(a, b, label, out):
    za, zb = zipfile.ZipFile(io.BytesIO(a)), zipfile.ZipFile(io.BytesIO(b))
    na = {n for n in za.namelist() if not n.endswith("/")}
    nb = {n for n in zb.namelist() if not n.endswith("/")}
    for n in sorted(na ^ nb):
        out.append(f"{label}{n}: only in {'a' if n in na else 'b'}")
    for n in sorted(na & nb):
        x, y = za.read(n), zb.read(n)
        if x == y:
            continue
        if x[:2] == b"PK" and y[:2] == b"PK":
            cmp_pkg(x, y, f"{label}{n}!", out); continue
        try:
            ex, ey = ET.fromstring(x), ET.fromstring(y)
        except ET.ParseError:
            out.append(f"{label}{n}: binary differs"); continue
        if n.endswith(".rels") or n == "[Content_Types].xml":
            if (ex.tag, set(norm(c) for c in ex)) != (ey.tag, set(norm(c) for c in ey)):
                out.append(f"{label}{n}: entries differ")
        elif norm(ex) != norm(ey):
            out.append(f"{label}{n}: content differs")

out = []
cmp_pkg(open(sys.argv[1], "rb").read(), open(sys.argv[2], "rb").read(), "", out)
print("\n".join(out))
