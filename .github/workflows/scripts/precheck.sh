#!/usr/bin/env bash

failed=0

# Check trailing whitespace
files=$(find . -type f \
    -not -path "./.git/*" \
    -not -path "*/.gradle/*" \
    -not -path "*/.idea/*" \
    -not -path "*/.vscode/*" \
    -not -path "*/build/*" \
    -not -path "*/out/*" \
    -not -path "*/bin/*" \
    -not -name "*.jar" \
    -not -name "*.java" \
    -exec grep -E -l " +$" {} \;)

count=0

for file in $files; do
    ((count++))
    echo "$file"
done

if [ $count -ne 0 ]; then
    failed=1
    echo "Error: trailing whitespace(s) in the above $count file(s)"
fi

# Check newline
files=$(find . -type f -size +0c \
    -not -path "./.git/*" \
    -not -path "*/.gradle/*" \
    -not -path "*/.idea/*" \
    -not -path "*/.vscode/*" \
    -not -path "*/build/*" \
    -not -path "*/out/*" \
    -not -path "*/bin/*" \
    -not -name "*.jar" \
    -not -name "*.java" \
    -exec bash -c 'if [[ $(tail -c1 "$0" | wc -l) -eq 0 ]]; then echo "$0"; fi' {} \;)

count=0

for file in $files; do
    ((count++))
    echo "$file"
done

if [ $count -ne 0 ]; then
    failed=1
    echo "Error: no newline in the above $count file(s)"
fi

exit $failed
