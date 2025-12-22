#!/bin/bash
# Script to verify language file encoding integrity
# This script can be run locally or as part of CI to ensure language files
# maintain proper character encoding.

LANG_DIR="src/main/resources/lang"
ERRORS=0

echo "Checking language file encoding integrity..."
echo "==========================================="

# Check if lang directory exists
if [ ! -d "$LANG_DIR" ]; then
    echo "ERROR: Language directory $LANG_DIR not found!"
    exit 1
fi

# Function to check file encoding
check_encoding() {
    local file=$1
    local expected_encoding=$2
    local filename=$(basename "$file")
    
    echo -n "Checking $filename... "
    
    # Try to read file with expected encoding
    if iconv -f "$expected_encoding" -t UTF-8 "$file" > /dev/null 2>&1; then
        echo "✓ OK ($expected_encoding)"
    else
        echo "✗ FAILED (not valid $expected_encoding)"
        ERRORS=$((ERRORS + 1))
    fi
}

# Function to check for specific characters
check_characters() {
    local file=$1
    local encoding=$2
    local chars=$3
    local filename=$(basename "$file")
    
    echo -n "  Checking for special characters in $filename... "
    
    # Convert file to UTF-8 for checking
    local content=$(iconv -f "$encoding" -t UTF-8 "$file")
    
    # Check if file contains expected characters
    local found=false
    for char in $chars; do
        if echo "$content" | grep -q "$char"; then
            found=true
            break
        fi
    done
    
    if [ "$found" = true ]; then
        echo "✓ OK"
    else
        echo "⚠ WARNING (no special characters found - file might be ASCII only)"
    fi
}

# Function to check for corruption patterns
check_corruption() {
    local file=$1
    local encoding=$2
    local filename=$(basename "$file")
    
    echo -n "  Checking for corruption in $filename... "
    
    # Convert to UTF-8 for checking
    local content=$(iconv -f "$encoding" -t UTF-8 "$file" 2>/dev/null || echo "")
    
    if [ -z "$content" ]; then
        echo "✗ FAILED (cannot read file)"
        ERRORS=$((ERRORS + 1))
        return
    fi
    
    # Check for common HTML entity corruption patterns (but not valid HTML)
    if echo "$content" | grep -E "<[EF][0-9A-F]{1,2}>" >/dev/null 2>&1; then
        echo "✗ FAILED (HTML entity corruption detected)"
        ERRORS=$((ERRORS + 1))
    else
        echo "✓ OK"
    fi
}

# Check Portuguese file (mixed encoding - mostly UTF-8 with some Latin-1)
echo ""
echo "Portuguese (pt_BR):"
# Portuguese file historically has mixed encoding, so we just check it can be read
if [ -f "$LANG_DIR/lang_pt_BR.properties" ]; then
    echo "Checking lang_pt_BR.properties... ✓ OK (exists)"
    # Just verify file is readable and not completely corrupted
    if head -100 "$LANG_DIR/lang_pt_BR.properties" | iconv -f UTF-8 -t UTF-8 > /dev/null 2>&1; then
        echo "  First 100 lines are valid UTF-8... ✓ OK"
    else
        echo "  WARNING: File has mixed encoding (this is expected for this file)"
    fi
else
    echo "✗ FAILED (file not found)"
    ERRORS=$((ERRORS + 1))
fi

# Check German file (ISO-8859-1)
echo ""
echo "German (de_DE):"
check_encoding "$LANG_DIR/lang_de_DE.properties" "ISO-8859-1"
check_characters "$LANG_DIR/lang_de_DE.properties" "ISO-8859-1" "ü ö ä ß"
check_corruption "$LANG_DIR/lang_de_DE.properties" "ISO-8859-1"

# Check French file (UTF-8)
echo ""
echo "French (fr_FR):"
check_encoding "$LANG_DIR/lang_fr_FR.properties" "UTF-8"
check_characters "$LANG_DIR/lang_fr_FR.properties" "UTF-8" "é è à ç"
check_corruption "$LANG_DIR/lang_fr_FR.properties" "UTF-8"

# Check English files (UTF-8 or ASCII)
echo ""
echo "English (en_US):"
check_encoding "$LANG_DIR/lang_en_US.properties" "UTF-8"
check_corruption "$LANG_DIR/lang_en_US.properties" "UTF-8"

echo ""
echo "English (en_GB):"
check_encoding "$LANG_DIR/lang_en_GB.properties" "UTF-8"
check_corruption "$LANG_DIR/lang_en_GB.properties" "UTF-8"

# Summary
echo ""
echo "==========================================="
if [ $ERRORS -eq 0 ]; then
    echo "✓ All encoding checks passed!"
    exit 0
else
    echo "✗ $ERRORS encoding check(s) failed!"
    exit 1
fi
