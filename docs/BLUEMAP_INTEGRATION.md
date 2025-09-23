# BlueMap Integration for Medieval Factions

This document describes the BlueMap integration feature that allows faction territories to be displayed on BlueMap web maps.

## Overview

Medieval Factions now supports BlueMap as an alternative to Dynmap for displaying faction territories on web maps. BlueMap offers better performance and more features compared to Dynmap, making it an excellent choice for servers with performance constraints.

## Features

- **Faction Territory Display**: All claimed chunks are displayed as colored areas on the BlueMap
- **Faction Colors**: Each faction's territories are displayed using the faction's configured color
- **Realm Support**: Optionally display realm boundaries (faction + vassal territories) with different styling
- **Performance Optimized**: Uses BlueMap's efficient marker system for better server performance
- **Configurable**: Extensive configuration options to customize what information is displayed

## Configuration

Add the following configuration section to your `config.yml`:

```yaml
bluemap:
  enableBluemapIntegration: true     # Enable/disable BlueMap integration
  showRealms: false                  # Show realm territories (faction + vassals)
  showDescription: true              # Show faction description in territory popups
  showMembers: false                 # Show faction members in territory popups
  showLiege: false                   # Show liege faction in territory popups
  showVassals: false                 # Show vassal factions in territory popups
  showAllies: false                  # Show allied factions in territory popups
  showAtWarWith: false               # Show enemy factions in territory popups
  showPower: false                   # Show faction power in territory popups
  showDemesne: false                 # Show territory count vs power in territory popups
  onlyRenderTerritoriesUponStartup: false  # Only render territories on server startup
  debug: false                       # Enable debug logging for BlueMap integration
```

## Priority System

Medieval Factions will automatically detect which map plugin is available and use the appropriate service:

1. **BlueMap** - If BlueMap is installed and `bluemap.enableBluemapIntegration` is true
2. **Dynmap** - If Dynmap is installed and `dynmap.enableDynmapIntegration` is true
3. **None** - If neither plugin is available or both integrations are disabled

This means you can have both plugins installed and Medieval Factions will prefer BlueMap over Dynmap.

## Dependencies

- **BlueMap Plugin**: The BlueMap plugin must be installed on your server
- **BlueMap API**: The BlueMap API dependency is included in Medieval Factions

## Territory Display

### Claims
Individual faction claims are displayed as:
- **Fill Color**: Faction color with 50% transparency
- **Border**: No border (transparent)
- **Label**: Faction name
- **Popup**: Configurable faction information

### Realm Borders (Optional)
When `showRealms` is enabled, realm boundaries are displayed as:
- **Fill Color**: Transparent
- **Border**: Faction color with 4px width
- **Label**: Faction name
- **Popup**: Faction information

## Performance Considerations

BlueMap integration is designed for optimal performance:
- Uses efficient shape markers instead of individual chunk markers
- Implements path optimization to reduce the number of markers
- Supports async task scheduling to prevent server lag
- Minimal memory footprint with concurrent data structures

## Troubleshooting

### BlueMap not detected
- Ensure BlueMap plugin is installed and enabled
- Check that `bluemap.enableBluemapIntegration` is set to `true`
- Verify BlueMap is properly configured and working

### Territories not showing
- Enable debug mode: `bluemap.debug: true`
- Check server logs for BlueMap integration messages
- Ensure factions have claimed territories
- Verify BlueMap web interface is accessible

### Performance issues
- Consider enabling `onlyRenderTerritoriesUponStartup` for large servers
- Reduce the number of display options (members, descriptions, etc.)
- Monitor server logs for any error messages

## API Integration

The BlueMap integration uses the BlueMap API to create:
- **Marker Sets**: Organized containers for faction markers
- **Shape Markers**: Polygon markers representing faction territories
- **Territory Popups**: HTML-formatted information about factions

## Migration from Dynmap

If you're migrating from Dynmap to BlueMap:

1. Install BlueMap plugin
2. Configure BlueMap for your worlds
3. Set `bluemap.enableBluemapIntegration: true`
4. Optionally set `dynmap.enableDynmapIntegration: false`
5. Restart the server

Medieval Factions will automatically switch to using BlueMap for territory display.

## Future Enhancements

Planned improvements for BlueMap integration:
- Support for custom marker icons
- Integration with BlueMap's 3D features
- Additional territory styling options
- Performance optimizations for very large faction networks