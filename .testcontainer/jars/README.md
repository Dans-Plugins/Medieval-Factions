# Test Server Plugin JARs

This directory contains plugin JAR files used by the test server.

## Included Plugins

The following plugins are already included:
- Dynmap
- PlaceholderAPI
- ServerUtils
- BlueMap
- Currencies

## Adding Slimefun

To enable Slimefun support in the test server:

1. Download Slimefun4 from one of these sources:
   - Official builds: https://blob.build/project/Slimefun4/RC
   - GitHub releases: https://github.com/Slimefun/Slimefun4/releases

2. Place the downloaded JAR file in this directory (`.testcontainer/jars/`) with a name matching the pattern `Slimefun*.jar`
   - Example: `Slimefun4-RC-37.jar`

3. Update your `.env` file (copy from `sample.env` if needed) and set:
   ```
   SLIMEFUN_ENABLED=true
   ```

4. Start or restart the test server:
   ```bash
   ./up.sh
   ```

The test server will automatically copy the Slimefun JAR to the plugins directory when enabled.

## Note

Due to licensing and distribution policies, we cannot include the Slimefun JAR file directly in this repository. Users must download it separately from the official sources.
