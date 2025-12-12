# Medieval Factions REST API Usage Guide

## Overview

The Medieval Factions REST API provides programmatic access to faction, player, and relationship data. This allows other plugins and external applications to integrate with Medieval Factions seamlessly.

## Configuration

The API can be configured in the `config.yml` file:

```yaml
api:
  enabled: true  # Set to false to disable the API
  port: 8080     # Port the API server listens on
```

## Base URL

By default, the API is available at:
```
http://localhost:8080/api
```

## Authentication

Currently, the API does not require authentication. It is recommended to use this API only in trusted environments or implement additional security measures at the network level (e.g., firewall rules, reverse proxy with authentication).

## Endpoints

### Health Check

Check if the API is running and get the plugin version.

**GET** `/api/health`

**Response:**
```json
{
  "status": "healthy",
  "version": "5.7.0-alpha-1"
}
```

---

### Factions

#### Get All Factions

Retrieve a list of all factions.

**GET** `/api/factions`

**Response:**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "The Kingdom",
    "description": "A mighty kingdom",
    "prefix": "TK",
    "power": 150.0,
    "maxPower": 200.0,
    "memberCount": 5,
    "home": {
      "world": "world",
      "x": 100.5,
      "y": 64.0,
      "z": -200.3
    }
  }
]
```

#### Get Faction by ID

Retrieve a specific faction by its UUID.

**GET** `/api/factions/{id}`

**Parameters:**
- `id` (path): Faction UUID

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "The Kingdom",
  "description": "A mighty kingdom",
  "prefix": "TK",
  "power": 150.0,
  "maxPower": 200.0,
  "memberCount": 5,
  "home": {
    "world": "world",
    "x": 100.5,
    "y": 64.0,
    "z": -200.3
  }
}
```

**Response (404 Not Found):**
```json
{
  "error": "NOT_FOUND",
  "message": "Faction not found"
}
```

**Response (400 Bad Request):**
```json
{
  "error": "INVALID_ID",
  "message": "Invalid faction ID format"
}
```

#### Get Faction by Name

Retrieve a specific faction by its name.

**GET** `/api/factions/name/{name}`

**Parameters:**
- `name` (path): Faction name

**Response:** Same as Get Faction by ID

---

### Players

#### Get All Players

Retrieve a list of all players.

**GET** `/api/players`

**Response:**
```json
[
  {
    "id": "660e8400-e29b-41d4-a716-446655440001",
    "name": "PlayerName",
    "power": 15.0,
    "factionId": "550e8400-e29b-41d4-a716-446655440000"
  }
]
```

#### Get Player by ID

Retrieve a specific player by their UUID.

**GET** `/api/players/{id}`

**Parameters:**
- `id` (path): Player UUID

**Response (200 OK):**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "name": "PlayerName",
  "power": 15.0,
  "factionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response (404 Not Found):**
```json
{
  "error": "NOT_FOUND",
  "message": "Player not found"
}
```

---

### Relationships

#### Get All Relationships

Retrieve all faction relationships.

**GET** `/api/relationships`

**Response:**
```json
[
  {
    "factionId": "550e8400-e29b-41d4-a716-446655440000",
    "targetFactionId": "770e8400-e29b-41d4-a716-446655440002",
    "type": "ALLY"
  },
  {
    "factionId": "550e8400-e29b-41d4-a716-446655440000",
    "targetFactionId": "880e8400-e29b-41d4-a716-446655440003",
    "type": "AT_WAR"
  }
]
```

**Relationship Types:**
- `ALLY` - Allied factions
- `AT_WAR` - Factions at war
- `LIEGE` - Liege faction relationship
- `VASSAL` - Vassal faction relationship

#### Get Relationships for a Faction

Retrieve all relationships for a specific faction.

**GET** `/api/relationships/faction/{id}`

**Parameters:**
- `id` (path): Faction UUID

**Response:** Same as Get All Relationships (filtered by faction)

---

### Claims

#### Get All Claims

Retrieve all claimed chunks across all factions.

**GET** `/api/claims`

**Response:**
```json
[
  {
    "worldId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "x": 10,
    "z": 20,
    "factionId": "550e8400-e29b-41d4-a716-446655440000"
  },
  {
    "worldId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "x": 11,
    "z": 20,
    "factionId": "550e8400-e29b-41d4-a716-446655440000"
  }
]
```

#### Get Claims for a Faction

Retrieve all claimed chunks for a specific faction.

**GET** `/api/claims/faction/{id}`

**Parameters:**
- `id` (path): Faction UUID

**Response:**
```json
[
  {
    "worldId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "x": 10,
    "z": 20,
    "factionId": "550e8400-e29b-41d4-a716-446655440000"
  }
]
```

**Response (400 Bad Request):**
```json
{
  "error": "INVALID_ID",
  "message": "Invalid faction ID format"
}
```

---

### OpenAPI Specification

Get the OpenAPI specification for the API.

**GET** `/api/openapi`

**Response:** Returns the OpenAPI specification in YAML format.

---

## Example Use Cases

### Use Case 1: Faction Information Display

A web dashboard plugin that displays faction information on a website.

```javascript
// Example using JavaScript/Node.js
const axios = require('axios');

async function getFactionInfo(factionName) {
  try {
    const response = await axios.get(
      `http://localhost:8080/api/factions/name/${factionName}`
    );
    
    const faction = response.data;
    console.log(`Faction: ${faction.name}`);
    console.log(`Power: ${faction.power}/${faction.maxPower}`);
    console.log(`Members: ${faction.memberCount}`);
    
    return faction;
  } catch (error) {
    if (error.response?.status === 404) {
      console.log('Faction not found');
    }
  }
}

getFactionInfo('The Kingdom');
```

### Use Case 2: Power Monitoring Plugin

A monitoring plugin that tracks faction power levels and sends alerts.

```java
// Example using Java/Bukkit plugin
import com.google.gson.Gson;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class PowerMonitor {
    private static final String API_URL = "http://localhost:8080/api";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();
    
    public static class Faction {
        public String id;
        public String name;
        public double power;
        public double maxPower;
    }
    
    public void checkLowPowerFactions() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL + "/factions"))
            .GET()
            .build();
            
        HttpResponse<String> response = client.send(
            request, 
            HttpResponse.BodyHandlers.ofString()
        );
        
        Faction[] factions = gson.fromJson(response.body(), Faction[].class);
        
        for (Faction faction : factions) {
            double powerPercentage = (faction.power / faction.maxPower) * 100;
            
            if (powerPercentage < 25) {
                // Send alert - faction has low power
                System.out.println(
                    "WARNING: " + faction.name + 
                    " has low power (" + powerPercentage + "%)"
                );
            }
        }
    }
}
```

### Use Case 3: War Status Checker

A plugin that checks if two factions are at war.

```python
# Example using Python
import requests

def are_factions_at_war(faction1_id, faction2_id):
    """Check if two factions are at war"""
    try:
        response = requests.get(
            f'http://localhost:8080/api/relationships/faction/{faction1_id}'
        )
        response.raise_for_status()
        
        relationships = response.json()
        
        for rel in relationships:
            if rel['targetFactionId'] == faction2_id and rel['type'] == 'AT_WAR':
                return True
                
        return False
        
    except requests.exceptions.RequestException as e:
        print(f"Error checking war status: {e}")
        return False

# Usage
if are_factions_at_war('550e8400-e29b-41d4-a716-446655440000', 
                        '880e8400-e29b-41d4-a716-446655440003'):
    print("Factions are at war!")
```

### Use Case 4: Player Faction Lookup

A Discord bot that looks up which faction a player belongs to.

```python
# Example Discord bot command
import discord
import requests
from discord.ext import commands

bot = commands.Bot(command_prefix='!')

@bot.command()
async def faction(ctx, player_name: str):
    """Look up a player's faction"""
    try:
        # Get all players and find by name
        response = requests.get('http://localhost:8080/api/players')
        players = response.json()
        
        player = next((p for p in players if p['name'] == player_name), None)
        
        if not player:
            await ctx.send(f"Player {player_name} not found")
            return
            
        if player['factionId']:
            # Get faction info
            faction_response = requests.get(
                f'http://localhost:8080/api/factions/{player["factionId"]}'
            )
            faction = faction_response.json()
            
            embed = discord.Embed(
                title=f"{player_name}'s Faction",
                color=discord.Color.blue()
            )
            embed.add_field(name="Faction", value=faction['name'])
            embed.add_field(name="Power", value=f"{player['power']:.1f}")
            embed.add_field(
                name="Faction Power", 
                value=f"{faction['power']:.1f}/{faction['maxPower']:.1f}"
            )
            
            await ctx.send(embed=embed)
        else:
            await ctx.send(f"{player_name} is not in a faction")
            
    except Exception as e:
        await ctx.send(f"Error: {e}")
```

### Use Case 5: Dynmap/Bluemap Integration

A bridge plugin that displays faction claims on Dynmap or Bluemap.

```java
// Example using Java plugin
import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public class FactionClaimRenderer {
    private static final String API_URL = "http://localhost:8080/api";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();
    private final DynmapAPI dynmap;
    private final MarkerSet markerSet;
    
    public static class Claim {
        public String worldId;
        public int x;
        public int z;
        public String factionId;
    }
    
    public static class Faction {
        public String id;
        public String name;
        public String prefix;
    }
    
    public FactionClaimRenderer(DynmapAPI dynmap) {
        this.dynmap = dynmap;
        MarkerAPI markerAPI = dynmap.getMarkerAPI();
        this.markerSet = markerAPI.createMarkerSet(
            "medievalfactions.claims", 
            "Faction Claims", 
            null, 
            false
        );
    }
    
    public void renderAllClaims() throws Exception {
        // Get all claims
        HttpRequest claimsRequest = HttpRequest.newBuilder()
            .uri(URI.create(API_URL + "/claims"))
            .GET()
            .build();
        HttpResponse<String> claimsResponse = client.send(
            claimsRequest, 
            HttpResponse.BodyHandlers.ofString()
        );
        Claim[] claims = gson.fromJson(claimsResponse.body(), Claim[].class);
        
        // Get all factions
        HttpRequest factionsRequest = HttpRequest.newBuilder()
            .uri(URI.create(API_URL + "/factions"))
            .GET()
            .build();
        HttpResponse<String> factionsResponse = client.send(
            factionsRequest, 
            HttpResponse.BodyHandlers.ofString()
        );
        Faction[] factions = gson.fromJson(factionsResponse.body(), Faction[].class);
        
        // Create a map of faction ID to faction
        Map<String, Faction> factionMap = Arrays.stream(factions)
            .collect(Collectors.toMap(f -> f.id, f -> f));
        
        // Group claims by faction
        Map<String, List<Claim>> claimsByFaction = Arrays.stream(claims)
            .collect(Collectors.groupingBy(c -> c.factionId));
        
        // Clear existing markers
        markerSet.getMarkers().forEach(marker -> marker.deleteMarker());
        
        // Render each faction's claims
        for (Map.Entry<String, List<Claim>> entry : claimsByFaction.entrySet()) {
            String factionId = entry.getKey();
            List<Claim> factionClaims = entry.getValue();
            Faction faction = factionMap.get(factionId);
            
            if (faction == null) continue;
            
            // Group contiguous chunks into regions
            for (Claim claim : factionClaims) {
                String worldName = getWorldNameFromUUID(claim.worldId);
                if (worldName == null) continue;
                
                // Calculate chunk coordinates in blocks
                double[] x = new double[] {
                    claim.x * 16, (claim.x + 1) * 16
                };
                double[] z = new double[] {
                    claim.z * 16, (claim.z + 1) * 16
                };
                
                // Create area marker for this chunk
                String markerId = "claim_" + claim.worldId + "_" + claim.x + "_" + claim.z;
                AreaMarker marker = markerSet.createAreaMarker(
                    markerId,
                    faction.name + " Territory",
                    false,
                    worldName,
                    x,
                    z,
                    false
                );
                
                // Set marker style based on faction
                if (marker != null) {
                    marker.setLineStyle(2, 1.0, 0x00FF00);
                    marker.setFillStyle(0.3, 0x00FF00);
                    marker.setDescription(
                        "<b>" + faction.name + "</b><br/>" +
                        "Chunk: " + claim.x + ", " + claim.z
                    );
                }
            }
        }
    }
    
    private String getWorldNameFromUUID(String worldUUID) {
        try {
            UUID uuid = UUID.fromString(worldUUID);
            return Bukkit.getWorld(uuid) != null ? Bukkit.getWorld(uuid).getName() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
```

## Best Practices

1. **Error Handling**: Always implement proper error handling for API requests. The API may be unavailable or return error responses.

2. **Caching**: Consider caching frequently accessed data to reduce load on the API and improve performance.

3. **Rate Limiting**: If making frequent requests, implement rate limiting to avoid overwhelming the server.

4. **Security**: The API currently doesn't require authentication. If exposing it beyond localhost, implement security measures:
   - Use a reverse proxy with authentication
   - Implement firewall rules to restrict access
   - Use HTTPS/TLS for encrypted communication

5. **Version Compatibility**: Check the plugin version via the `/api/health` endpoint to ensure compatibility with your integration.

## Troubleshooting

### API Not Responding

- Check if the API is enabled in `config.yml`
- Verify the correct port is being used
- Check server logs for errors

### Connection Refused

- Ensure the Minecraft server is running
- Verify firewall settings allow connections on the API port
- Check if another application is using the same port

### Data Not Updating

- The API provides real-time data from the plugin
- If data seems stale, verify the plugin is functioning correctly
- Check for any errors in the server console

## Further Support

For issues or questions:
- GitHub Issues: https://github.com/Dans-Plugins/Medieval-Factions/issues
- Discord: https://discord.gg/xXtuAQ2
