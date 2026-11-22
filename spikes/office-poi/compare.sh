#!/usr/bin/env bash
# Redacts every fixture with phileas-dotnet and with the Java/POI port, then compares the outputs.
set -u
S="$(cd "$(dirname "$0")" && pwd)"
NET="dotnet $S/dotnet-harness/bin/out/Harness.dll"
JAVA="java -cp $S/java/target/classes:$(cat $S/java/cp.txt) spike.Main"
POLICY=$S/policy.json
rm -rf "$S/out" && mkdir -p "$S/out/net" "$S/out/java" "$S/out/lo-net" "$S/out/lo-java" "$S/out/lo-in" "$S/out/rt"
REPORT=$S/out/report.tsv
printf "fixture\tnet_spans\tjava_spans\tnet_leaks\tjava_leaks\tnet_detect_after\tjava_detect_after\tin_valid_errs\tnet_valid_errs\tjava_valid_errs\tlo_opens_java\tlo_text_same\tparagraphs_same\tinput_leaks\n" > "$REPORT"

for f in "$S"/fixtures/*; do
  name=$(basename "$f")
  ext="${name##*.}"
  n=$($NET redact $POLICY "$f" "$S/out/net/$name" 2>"$S/out/net/$name.err" | tail -1)
  j=$($JAVA redact $POLICY "$f" "$S/out/java/$name" 2>"$S/out/java/$name.err" | grep "^RESULT" | cut -d" " -f2-)
  il=$(python3 -I "$S/leaks.py" "$f" | wc -l)
  nl=$( [ -f "$S/out/net/$name" ] && python3 -I "$S/leaks.py" "$S/out/net/$name" | wc -l || echo ERR)
  jl=$( [ -f "$S/out/java/$name" ] && python3 -I "$S/leaks.py" "$S/out/java/$name" | wc -l || echo ERR)
  # .NET Detect over each output: what the reference implementation still finds.
  nd=$($NET detect $POLICY "$S/out/net/$name" 2>/dev/null | python3 -I -c 'import json,sys; print(len(json.load(sys.stdin)))' 2>/dev/null || echo ERR)
  jd=$($NET detect $POLICY "$S/out/java/$name" 2>/dev/null | python3 -I -c 'import json,sys; print(len(json.load(sys.stdin)))' 2>/dev/null || echo ERR)
  iv=$($NET validate "$f" 2>&1 | wc -l)
  nv=$($NET validate "$S/out/net/$name" 2>&1 | wc -l)
  jv=$($NET validate "$S/out/java/$name" 2>&1 | wc -l)
  # LibreOffice: convert each output to text (docx) or csv (xlsx) and compare the two.
  if [ "$ext" = docx ]; then fmt=txt; else fmt=csv; fi
  (cd "$S/out/lo-net" && timeout 60 soffice --headless --convert-to $fmt "$S/out/net/$name" >/dev/null 2>&1)
  (cd "$S/out/lo-java" && timeout 60 soffice --headless --convert-to $fmt "$S/out/java/$name" >/dev/null 2>&1)
  base="${name%.*}.$fmt"
  if [ -s "$S/out/lo-java/$base" ] || [ -f "$S/out/lo-java/$base" ]; then lo=yes; else lo=NO; fi
  if cmp -s "$S/out/lo-net/$base" "$S/out/lo-java/$base"; then same=yes; else same=NO; fi
  if [ "$ext" = docx ]; then
    pn=$($NET paragraphs "$f" 2>/dev/null); pj=$($JAVA paragraphs "$f" 2>/dev/null | grep "^RESULT" | cut -d" " -f2-)
    pn=$(echo "$pn" | python3 -I -c 'import json,sys; print(json.dumps(json.load(sys.stdin)))' 2>/dev/null)
    pj=$(echo "$pj" | python3 -I -c 'import json,sys; print(json.dumps(json.load(sys.stdin)))' 2>/dev/null)
    [ "$pn" = "$pj" ] && ps=yes || ps=NO
  else ps=-; fi
  printf "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n" "$name" "$n" "$j" "$nl" "$jl" "$nd" "$jd" "$iv" "$nv" "$jv" "$lo" "$same" "$ps" "$il" >> "$REPORT"
done
column -t -s $'\t' "$REPORT"
