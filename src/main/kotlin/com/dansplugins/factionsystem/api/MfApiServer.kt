package com.dansplugins.factionsystem.api

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.api.controller.FactionController
import com.dansplugins.factionsystem.api.controller.PlayerController
import com.dansplugins.factionsystem.api.controller.RelationshipController
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.javalin.Javalin
import io.javalin.json.JavalinJackson

class MfApiServer(private val plugin: MedievalFactions) {
    
    private var app: Javalin? = null
    private lateinit var factionController: FactionController
    private lateinit var playerController: PlayerController
    private lateinit var relationshipController: RelationshipController
    
    fun start() {
        if (!plugin.config.getBoolean("api.enabled", true)) {
            plugin.logger.info("REST API is disabled in configuration")
            return
        }
        
        val port = plugin.config.getInt("api.port", 8080)
        
        try {
            // Configure Jackson for Kotlin
            val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
            
            // Initialize controllers
            factionController = FactionController(plugin.services.factionService)
            playerController = PlayerController(plugin.services.playerService, plugin.services.factionService)
            relationshipController = RelationshipController(plugin.services.factionRelationshipService)
            
            // Create Javalin app
            app = Javalin.create { config ->
                config.jsonMapper(JavalinJackson(objectMapper))
                config.showJavalinBanner = false
            }.start(port)
            
            // Register routes
            registerRoutes()
            
            plugin.logger.info("REST API server started on port $port")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to start REST API server: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun stop() {
        app?.stop()
        plugin.logger.info("REST API server stopped")
    }
    
    private fun registerRoutes() {
        app?.apply {
            // Faction endpoints
            get("/api/factions") { ctx -> factionController.getAll(ctx) }
            get("/api/factions/{id}") { ctx -> factionController.getById(ctx) }
            get("/api/factions/name/{name}") { ctx -> factionController.getByName(ctx) }
            
            // Player endpoints
            get("/api/players") { ctx -> playerController.getAll(ctx) }
            get("/api/players/{id}") { ctx -> playerController.getById(ctx) }
            
            // Relationship endpoints
            get("/api/relationships") { ctx -> relationshipController.getAll(ctx) }
            get("/api/relationships/faction/{id}") { ctx -> relationshipController.getByFactionId(ctx) }
            
            // OpenAPI spec endpoint
            get("/api/openapi") { ctx -> 
                ctx.contentType("text/plain")
                ctx.result(getOpenApiSpec())
            }
            
            // Health check
            get("/api/health") { ctx ->
                ctx.json(mapOf("status" to "healthy", "version" to plugin.description.version))
            }
        }
    }
    
    private fun getOpenApiSpec(): String {
        return """
openapi: 3.0.0
info:
  title: Medieval Factions API
  description: REST API for Medieval Factions plugin
  version: ${plugin.description.version}
  contact:
    name: Medieval Factions
    url: https://github.com/Dans-Plugins/Medieval-Factions
servers:
  - url: http://localhost:${plugin.config.getInt("api.port", 8080)}
    description: Local server
paths:
  /api/health:
    get:
      summary: Health check endpoint
      responses:
        '200':
          description: API is healthy
          content:
            application/json:
              schema:
                type: object
                properties:
                  status:
                    type: string
                  version:
                    type: string
  /api/factions:
    get:
      summary: Get all factions
      responses:
        '200':
          description: List of all factions
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/Faction'
  /api/factions/{id}:
    get:
      summary: Get faction by ID
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Faction found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Faction'
        '404':
          description: Faction not found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Error'
  /api/factions/name/{name}:
    get:
      summary: Get faction by name
      parameters:
        - name: name
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: Faction found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Faction'
        '404':
          description: Faction not found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Error'
  /api/players:
    get:
      summary: Get all players
      responses:
        '200':
          description: List of all players
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/Player'
  /api/players/{id}:
    get:
      summary: Get player by ID
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Player found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Player'
        '404':
          description: Player not found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Error'
  /api/relationships:
    get:
      summary: Get all faction relationships
      responses:
        '200':
          description: List of all relationships
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/Relationship'
  /api/relationships/faction/{id}:
    get:
      summary: Get relationships for a specific faction
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: List of relationships
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/Relationship'
components:
  schemas:
    Faction:
      type: object
      properties:
        id:
          type: string
          format: uuid
        name:
          type: string
        description:
          type: string
        prefix:
          type: string
          nullable: true
        power:
          type: number
          format: double
        maxPower:
          type: number
          format: double
        memberCount:
          type: integer
        home:
          $ref: '#/components/schemas/Position'
    Player:
      type: object
      properties:
        id:
          type: string
          format: uuid
        name:
          type: string
          nullable: true
        power:
          type: number
          format: double
        factionId:
          type: string
          format: uuid
          nullable: true
    Position:
      type: object
      properties:
        world:
          type: string
        x:
          type: number
          format: double
        y:
          type: number
          format: double
        z:
          type: number
          format: double
    Relationship:
      type: object
      properties:
        factionId:
          type: string
          format: uuid
        targetFactionId:
          type: string
          format: uuid
        type:
          type: string
          enum: [ALLY, AT_WAR, LIEGE, VASSAL]
    Error:
      type: object
      properties:
        error:
          type: string
        message:
          type: string
        """.trimIndent()
    }
}
