"""Reads open-check.ps1's results folder and compares how Word and Excel handled each file's input,
phileas-dotnet output, and POI-port output.

Usage: python3 -I office-compare.py <results-dir>

Per file it reports the Office status of each copy, whether Office read the same text from the two
outputs, and any known PII values in the text Office read from each output (the values in leaks.py).
"""
import ast
import csv
import sys
from pathlib import Path

results = Path(sys.argv[1])
leaks_py = Path(__file__).resolve().parent.parent / "leaks.py"
VALUES = next(ast.literal_eval(n.value) for n in ast.parse(leaks_py.read_text()).body
              if isinstance(n, ast.Assign) and getattr(n.targets[0], "id", "") == "VALUES")

rows = {}
with open(results / "results.csv", encoding="utf-8-sig") as f:
    for r in csv.DictReader(f):
        side, name = r["file"].replace("\\", "/").split("/", 1)
        status = r["status"] + (" REPAIRED" if r["repaired"] == "yes" else "")
        rows.setdefault(name, {})[side] = (status, r["error"])


def text(side, name):
    p = results / "text" / side / (name + ".txt")
    return p.read_text(encoding="utf-8", errors="replace") if p.exists() else None


def leaks(t):
    return sorted(v for v in VALUES if t and v in t)


problems = 0
print(f"{'file':40} {'input':10} {'net':10} {'java':10} {'same text':10} java leaks / net leaks")
for name in sorted(rows):
    st = {s: rows[name].get(s, ("missing", ""))[0] for s in ("input", "net", "java")}
    tn, tj = text("net", name), text("java", name)
    same = "yes" if tn is not None and tn == tj else "NO"
    lj, ln = leaks(tj), leaks(tn)
    flag = st["java"] != st["net"] or same == "NO" or set(lj) - set(ln)
    problems += bool(flag)
    print(f"{name:40} {st['input']:10} {st['net']:10} {st['java']:10} {same:10} {lj} / {ln}{'  <-- check' if flag else ''}")
    for side in ("net", "java"):
        err = rows[name].get(side, ("", ""))[1]
        if err:
            print(f"    {side}: {err}")

print(f"\n{len(rows)} files, {problems} where the POI output differs from phileas-dotnet's in Office.")
