"""Search every part of an OOXML package (recursing into embedded packages) for known PII strings.

Usage: python3 -I leaks.py <file>   -> prints "part: value" for each hit, nothing when clean.
"""
import io
import sys
import zipfile
from urllib.parse import unquote

VALUES = [
    "123-45-6789", "987-65-4321", "123456789",
    "jane.doe@example.com", "john@example.org", "george@fake.com",
    "555-0142", "555-0199", "4111 1111 1111 1111",
]


def scan(data, label, hits):
    try:
        z = zipfile.ZipFile(io.BytesIO(data))
    except zipfile.BadZipFile:
        return
    for name in z.namelist():
        blob = z.read(name)
        if blob[:2] == b"PK":
            scan(blob, f"{label}!{name}", hits)
            continue
        text = blob.decode("utf-8", "replace")
        decoded = unquote(text).replace("&amp;", "&")
        # A value may be split across runs; also compare with tags stripped.
        import re
        stripped = re.sub(r"<[^>]+>", "", decoded)
        for v in VALUES:
            if v in decoded or v in stripped:
                hits.add(f"{label}!{name}: {v}")


hits = set()
with open(sys.argv[1], "rb") as f:
    scan(f.read(), "", hits)
for h in sorted(hits):
    print(h)
