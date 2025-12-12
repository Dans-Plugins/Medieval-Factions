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
