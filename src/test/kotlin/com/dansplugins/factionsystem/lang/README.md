# Language File Encoding Checker

This directory contains tools to verify the encoding integrity of language files in the Medieval Factions plugin.

## Purpose

Language files use different character encodings depending on the language:
- **German (de_DE)**: ISO-8859-1 (for umlauts: ü, ö, ä, ß)
- **French (fr_FR)**: UTF-8 (for accents: é, è, à, ç)
- **Portuguese (pt_BR)**: Mixed encoding (historically UTF-8 with some Latin-1)
- **English (en_US, en_GB)**: UTF-8 / ASCII

These tools help prevent character corruption when editing language files.

## Tools

### 1. Unit Tests (`LanguageFileEncodingTest.kt`)

Kotlin unit tests that verify encoding integrity as part of the build process.

**Run tests:**
```bash
./gradlew test --tests "com.dansplugins.factionsystem.lang.LanguageFileEncodingTest"
```

**What it checks:**
- All language files can be read without encoding errors
- German file uses ISO-8859-1 and contains umlauts
- French file uses UTF-8 and contains accents
- English files use UTF-8 or ASCII
- No files contain corrupted character sequences (�, HTML entities like <EA>)

### 2. Shell Script (`check-lang-encoding.sh`)

Standalone shell script that can be run locally or in CI.

**Run script:**
```bash
./check-lang-encoding.sh
```

**What it checks:**
- File encoding validity for each language
- Presence of expected special characters
- Absence of corruption patterns

**Exit codes:**
- 0: All checks passed
- 1: One or more checks failed

## CI Integration

The unit tests run automatically as part of the Gradle build in GitHub Actions:
```yaml
- name: Build with Gradle
  uses: gradle/gradle-build-action@v2
  with:
    arguments: build
```

The shell script can also be added to CI workflows:
```yaml
- name: Check language file encoding
  run: ./check-lang-encoding.sh
```

## Editing Language Files

When editing language files, follow these guidelines:

### German (`lang_de_DE.properties`)
- **Encoding**: ISO-8859-1 / Latin-1
- **Tools**: Use an editor that supports ISO-8859-1 (e.g., `iconv`, Python with `encoding='iso-8859-1'`)
- **Characters**: ü, ö, ä, ß, Ü, Ö, Ä

### French (`lang_fr_FR.properties`)
- **Encoding**: UTF-8
- **Tools**: Most modern editors default to UTF-8
- **Characters**: é, è, à, ç, ê, ô, î, û

### Portuguese (`lang_pt_BR.properties`)
- **Encoding**: Mixed (historical)
- **Tools**: Use Latin-1 / ISO-8859-1 for safety
- **Characters**: ã, ç, ê, õ, á, í, ó

### English (`lang_en_US.properties`, `lang_en_GB.properties`)
- **Encoding**: UTF-8 / ASCII
- **Tools**: Any editor
- **Characters**: Standard ASCII

## Troubleshooting

### Problem: Characters display as � or HTML entities like <EA>

**Cause**: File was edited with wrong encoding or encoding was corrupted during save.

**Solution**:
1. Restore the file from git: `git checkout -- src/main/resources/lang/lang_XX_XX.properties`
2. Re-apply your changes using an encoding-aware editor
3. Run `./check-lang-encoding.sh` to verify

### Problem: Tests fail after editing

**Cause**: Encoding corruption was introduced.

**Solution**:
1. Check which file failed: `./gradlew test --tests "com.dansplugins.factionsystem.lang.LanguageFileEncodingTest"`
2. Review the error message to identify the issue
3. Use `hexdump -C file.properties | less` to inspect byte-level encoding
4. Restore and re-edit with correct encoding

## Development Workflow

1. Before editing language files, run: `./check-lang-encoding.sh`
2. Edit files with appropriate encoding (see guidelines above)
3. After editing, run: `./check-lang-encoding.sh`
4. If checks pass, commit your changes
5. CI will verify encoding integrity automatically

## References

- [Java Properties File Encoding](https://docs.oracle.com/javase/8/docs/api/java/util/Properties.html)
- [ISO-8859-1 Character Set](https://en.wikipedia.org/wiki/ISO/IEC_8859-1)
- [UTF-8 Encoding](https://en.wikipedia.org/wiki/UTF-8)
