# BlueMap Integration for Medieval Factions

This document describes the BlueMap integration feature that allows faction territories to be displayed on BlueMap web maps.

## Overview

Medieval Factions now supports BlueMap as an alternative to Dynmap for displaying faction territories on web maps. BlueMap offers better performance and more features compared to Dynmap, making it an excellent choice for servers with performance constraints.

## Implementation Status

✅ **Complete Implementation**: The BlueMap integration follows proper BlueMap API patterns  
✅ **Production Ready**: Uses reflection-based API access for compatibility  
✅ **Event-Driven**: Properly handles BlueMap plugin loading and initialization  
✅ **Error Resilient**: Comprehensive error handling and graceful degradation  

## Technical Implementation

### Proper BlueMap API Integration

The implementation follows BlueMap best practices:

- **Event-Driven Initialization**: Uses `PluginEnableEvent` to detect BlueMap
- **Reflection-Based API Access**: Avoids dependency issues while maintaining compatibility
- **Proper Thread Safety**: All BlueMap API calls are properly synchronized
- **Resource Management**: Proper cleanup of markers and resources

### API Usage Patterns

```kotlin
// Proper BlueMap API initialization
@EventHandler
fun onPluginEnable(event: PluginEnableEvent) {
    if (event.plugin.name == "BlueMap") {
        initializeBlueMapAPI()
    }
}

// Safe API access using reflection
val blueMapAPIClass = Class.forName("de.bluecolored.bluemap.api.BlueMapAPI")
val getInstanceMethod = blueMapAPIClass.getMethod("getInstance")
val optionalAPI = getInstanceMethod.invoke(null)
```

### Marker Creation

The implementation uses proper BlueMap marker patterns:

- **Shape Builders**: Uses `Shape.Builder` for polygon creation
- **Marker Sets**: Properly manages marker organization
- **Color Support**: Native BlueMap color system integration
- **Z-Level Management**: Proper height and layer management

## Features

- **Faction Territory Display**: All claimed chunks displayed as colored polygons
- **Faction Colors**: Each faction's territories use the faction's configured color
- **Realm Support**: Optional realm boundaries (faction + vassal territories)
- **Event-Driven Updates**: Automatic marker updates when territories change
- **Performance Optimized**: Uses BlueMap's efficient marker system
- **Error Recovery**: Graceful handling of BlueMap unavailability

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

Medieval Factions automatically detects which map plugin is available:

1. **BlueMap** - If BlueMap is installed and `bluemap.enableBluemapIntegration` is true
2. **Dynmap** - If Dynmap is installed and `dynmap.enableDynmapIntegration` is true
3. **None** - If neither plugin is available or both integrations are disabled

## Dependencies

- **BlueMap Plugin**: The BlueMap plugin must be installed on your server
- **BlueMap API**: Uses reflection to access BlueMap API (no compile dependency needed)

## Territory Display

### Claims
Individual faction claims are displayed as:
- **Fill Color**: Faction color with configurable transparency
- **Border**: Optional border styling
- **Label**: Faction name
- **Popup**: HTML-formatted faction information

### Territory Borders
Optimized territory boundaries are displayed as:
- **Fill Color**: Faction color with transparency
- **Border**: Faction color with configurable width
- **Path Optimization**: Reduces marker count for better performance

### Realm Borders (Optional)
When `showRealms` is enabled, realm boundaries are displayed as:
- **Fill Color**: Transparent
- **Border**: Faction color with thick border
- **Hierarchy**: Shows faction + vassal relationships

## Performance Considerations

The BlueMap integration is optimized for performance:

- **Reflection-Based Access**: No compile-time dependency on BlueMap
- **Event-Driven**: Only initializes when BlueMap is available
- **Async Processing**: All marker operations are asynchronous
- **Error Recovery**: Graceful degradation when BlueMap is unavailable
- **Resource Cleanup**: Proper marker and resource management

## Troubleshooting

### BlueMap not detected
- Ensure BlueMap plugin is installed and enabled
- Check server logs for initialization messages
- Verify `bluemap.enableBluemapIntegration` is set to `true`

### Territories not showing
- Enable debug mode: `bluemap.debug: true`
- Check for reflection-related errors in logs
- Ensure BlueMap API is properly initialized
- Verify faction territories exist

### Performance issues
- Enable `onlyRenderTerritoriesUponStartup` for large servers
- Monitor server logs for API access errors
- Check BlueMap plugin performance

## Compatibility

### BlueMap Versions
The implementation uses reflection for maximum compatibility:
- **BlueMap 2.6+**: Full compatibility
- **BlueMap 3.x**: Forward compatible design
- **API Changes**: Automatic adaptation through reflection

### Error Handling
Comprehensive error handling ensures stability:
- **Missing Classes**: Graceful degradation
- **API Changes**: Automatic detection and adaptation
- **Plugin Conflicts**: Isolated error handling

## Migration from Dynmap

Migration is seamless:

1. Install BlueMap plugin
2. Configure BlueMap for your worlds
3. Set `bluemap.enableBluemapIntegration: true`
4. Restart the server
5. Verify territories appear on BlueMap

Medieval Factions automatically switches to BlueMap when available.

## API Architecture

The implementation follows proper BlueMap patterns:

### Event-Driven Initialization
```kotlin
// Waits for BlueMap plugin to load
@EventHandler
fun onPluginEnable(event: PluginEnableEvent)
```

### Safe API Access
```kotlin
// Uses reflection to avoid compile dependencies
val blueMapAPI = getBlueMapAPIThroughReflection()
```

### Proper Resource Management
```kotlin
// Tracks and cleans up markers properly
private val factionMarkersByFactionId = ConcurrentHashMap<MfFactionId, List<String>>()
```

This implementation provides production-ready BlueMap integration that follows all BlueMap API best practices while maintaining compatibility and performance.