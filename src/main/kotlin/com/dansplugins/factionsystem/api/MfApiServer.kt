package com.dansplugins.factionsystem.api

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.api.controller.ClaimController
import com.dansplugins.factionsystem.api.controller.FactionController
import com.dansplugins.factionsystem.api.controller.PlayerController
import com.dansplugins.factionsystem.api.controller.RelationshipController
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.javalin.Javalin
import io.javalin.json.JavalinJackson

class MfApiServer(private val plugin: MedievalFactions) {
    
    companion object {
        private const val OPENAPI_SPEC_PATH = "/openapi.yaml"
    }
    
    private var app: Javalin? = null
    private lateinit var factionController: FactionController
    private lateinit var playerController: PlayerController
    private lateinit var relationshipController: RelationshipController
    private lateinit var claimController: ClaimController
    
    fun start() {
        if (!plugin.config.getBoolean("api.enabled", true)) {
            plugin.logger.info("REST API is disabled in configuration")
            return
        }
        
        val port = plugin.config.getInt("api.port", 8080)
        if (port !in 1..65535) {
            plugin.logger.severe("Invalid API port: $port. Please specify a port between 1 and 65535 in the configuration.")
            return
        }
        
        val host = plugin.config.getString("api.host", "127.0.0.1")
        
        try {
            // Configure Jackson for Kotlin
            val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
            
            // Initialize controllers
            factionController = FactionController(plugin.services.factionService)
            playerController = PlayerController(plugin.services.playerService, plugin.services.factionService)
            relationshipController = RelationshipController(plugin.services.factionRelationshipService, plugin.services.factionService)
            claimController = ClaimController(plugin.services.claimService)
            
            // Create Javalin app
            app = Javalin.create { config ->
                config.jsonMapper(JavalinJackson(objectMapper))
                config.showJavalinBanner = false
            }.start(host, port)
            
            // Register routes
            registerRoutes()
            
            plugin.logger.info("REST API server started on $host:$port")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to start REST API server: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun stop() {
        if (app != null) {
            app?.stop()
            plugin.logger.info("REST API server stopped")
        }
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
            
            // Claim endpoints
            get("/api/claims") { ctx -> claimController.getAll(ctx) }
            get("/api/claims/faction/{id}") { ctx -> claimController.getByFactionId(ctx) }
            
            // OpenAPI spec endpoint
            get("/api/openapi") { ctx -> 
                ctx.contentType("application/x-yaml")
                ctx.result(getOpenApiSpec())
            }
            
            // Health check
            get("/api/health") { ctx ->
                ctx.json(mapOf("status" to "healthy", "version" to plugin.description.version))
            }
        }
    }
    
    private fun getOpenApiSpec(): String {
        // Load OpenAPI spec from resources
        val specTemplate = this::class.java.getResourceAsStream(OPENAPI_SPEC_PATH)?.bufferedReader()?.use { it.readText() }
        
        if (specTemplate == null) {
            plugin.logger.severe("Failed to load OpenAPI specification from resources")
            return """
openapi: 3.0.0
info:
  title: Medieval Factions API
  description: REST API for Medieval Factions plugin (specification could not be loaded)
  version: ${plugin.description.version}
paths: {}
            """.trimIndent()
        }
        
        // Replace placeholders with actual values
        return specTemplate
            .replace("{{VERSION}}", plugin.description.version)
            .replace("{{PORT}}", plugin.config.getInt("api.port", 8080).toString())
    }
}
