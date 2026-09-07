package com.dansplugins.factionsystem.dpc

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt
import au.com.dius.pact.consumer.junit5.PactTestFor
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import com.google.gson.Gson
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@ExtendWith(PactConsumerTestExt::class)
@PactTestFor(providerName = "dpc-api", pactVersion = PactSpecVersion.V3)
class MfDpcApiPactConsumerTest {

    private val gson = Gson()

    @Pact(consumer = "medieval-factions")
    fun factionSyncRequiredFields(builder: PactDslWithProvider): RequestResponsePact {
        return builder
            .given("a valid API key exists")
            .uponReceiving("a faction sync with required fields only")
            .method("POST")
            .path("/api/v1/factions")
            .headers(mapOf("Content-Type" to "application/json", "X-API-Key" to "test-api-key"))
            .body(
                gson.toJson(
                    listOf(
                        DpcFactionPayload(
                            name = "Test Faction",
                            serverId = "test-server",
                            memberCount = 5,
                            description = "A test faction"
                        )
                    )
                )
            )
            .willRespondWith()
            .status(200)
            .toPact()
    }

    @Pact(consumer = "medieval-factions")
    fun factionSyncAllFields(builder: PactDslWithProvider): RequestResponsePact {
        return builder
            .given("a valid API key exists")
            .uponReceiving("a faction sync with all optional fields")
            .method("POST")
            .path("/api/v1/factions")
            .headers(mapOf("Content-Type" to "application/json", "X-API-Key" to "test-api-key"))
            .body(
                gson.toJson(
                    listOf(
                        DpcFactionPayload(
                            name = "Full Faction",
                            serverId = "test-server",
                            memberCount = 10,
                            description = "A faction with all optional fields",
                            serverIp = "play.example.com",
                            discordLink = "https://discord.gg/example"
                        )
                    )
                )
            )
            .willRespondWith()
            .status(200)
            .toPact()
    }

    @Test
    @PactTestFor(pactMethod = "factionSyncRequiredFields")
    fun testFactionSyncRequiredFields(mockServer: MockServer) {
        sendSync(
            mockServer,
            listOf(
                DpcFactionPayload(
                    name = "Test Faction",
                    serverId = "test-server",
                    memberCount = 5,
                    description = "A test faction"
                )
            )
        )
    }

    @Test
    @PactTestFor(pactMethod = "factionSyncAllFields")
    fun testFactionSyncAllFields(mockServer: MockServer) {
        sendSync(
            mockServer,
            listOf(
                DpcFactionPayload(
                    name = "Full Faction",
                    serverId = "test-server",
                    memberCount = 10,
                    description = "A faction with all optional fields",
                    serverIp = "play.example.com",
                    discordLink = "https://discord.gg/example"
                )
            )
        )
    }

    private fun sendSync(mockServer: MockServer, payloads: List<DpcFactionPayload>) {
        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
            .uri(URI("${mockServer.getUrl()}/api/v1/factions"))
            .header("Content-Type", "application/json")
            .header("X-API-Key", "test-api-key")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payloads)))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() in 200..299) {
            "Expected 2xx but got ${response.statusCode()}: ${response.body()}"
        }
    }
}
