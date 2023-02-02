package com.dansplugins.factionsystem.command.faction.dev

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.role.MfFactionRoles
import com.dansplugins.factionsystem.faction.withRole
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.word.adjectives
import com.dansplugins.factionsystem.word.nouns
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import java.util.*
import java.util.logging.Level.SEVERE
import kotlin.random.Random

class MfFactionDevGenerateCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val players = listOf(
            UUID.fromString("0a9fa342-3139-49d7-8acb-fcf4d9c1f0ef"),
            UUID.fromString("3fcad669-b828-4d6d-be88-1c28b067c5d8"),
            UUID.fromString("128ba8e4-7ba9-44c2-9968-cab2897f758f"),
            UUID.fromString("ef26130a-44c6-4c44-ab56-d35f11fb516e"),
            UUID.fromString("9d44baf4-e390-4d2e-8ba4-d0ffa722e53f"),
            UUID.fromString("3e45785c-2520-46ba-a34d-be448c7fdf82"),
            UUID.fromString("b3637649-0ddb-46a0-9c1f-84bf9225ad91"),
            UUID.fromString("3de2323f-8295-48cf-b46e-156c276b0246"),
            UUID.fromString("880d5589-3bdf-425a-9f0e-a758ce766b5d"),
            UUID.fromString("77ae6081-6495-420e-94e3-042918c11746"),
            UUID.fromString("13039937-663c-43c6-9c85-42c552d96740"),
            UUID.fromString("f2011fd5-d734-4d12-8748-e1a839c1c3a2"),
            UUID.fromString("5469d055-0edd-4c2c-bb4e-64b1dc04295a"),
            UUID.fromString("037fb8ce-8db6-4976-9af3-898ca547e4aa"),
            UUID.fromString("030d9655-b403-441a-8766-adba39881452"),
            UUID.fromString("1cb24001-21f3-411a-afe8-c4629c97436a"),
            UUID.fromString("12f7e421-57bc-4897-a866-131b405bf676"),
            UUID.fromString("be9ee451-7453-4887-9919-cf2db6c9862d"),
            UUID.fromString("697b8978-a214-438e-b313-849a4bcdc743"),
            UUID.fromString("7dab0167-4689-4bd6-9724-132b23e644fc"),
            UUID.fromString("8c1ac1d3-be4d-45ec-8047-e008719a8296")
        ).map { uuid -> plugin.server.getOfflinePlayer(uuid) }
        val spawnLocation = plugin.server.worlds.first().spawnLocation
        val worldId = spawnLocation.world!!.uid
        val chunkX = spawnLocation.chunk.x
        val chunkZ = spawnLocation.chunk.z
        val playerService = plugin.services.playerService
        val factionService = plugin.services.factionService
        val claimService = plugin.services.claimService
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                players.forEach { bukkitPlayer ->
                    val mfPlayer = playerService.getPlayer(bukkitPlayer)
                        ?: playerService.save(MfPlayer(plugin, bukkitPlayer)).onFailure {
                            sender.sendMessage("${RED}Failed to save player while generating data")
                            plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                            return@forEach
                        }
                    val factionId = MfFactionId.generate()
                    val roles = MfFactionRoles.defaults(plugin, factionId)
                    val owner = roles.single { it.name == "Owner" }
                    val faction = MfFaction(plugin, id = factionId, name = "${adjectives.random()} ${adjectives.random()} ${nouns.random()}", roles = roles, members = listOf(mfPlayer.withRole(owner)))
                    val createdFaction = factionService.save(faction).onFailure {
                        sender.sendMessage("${RED}Failed to save faction while generating data")
                        plugin.logger.log(SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
                        return@forEach
                    }
                    sender.sendMessage("${GREEN}Generated faction ${createdFaction.name}")
                    val startX = chunkX + Random.nextInt(1000) - 500
                    val startZ = chunkZ + Random.nextInt(1000) - 500
                    var x = startX
                    var z = startZ
                    for (j in 0..1500) {
                        val direction = Random.nextInt(4)
                        while (claimService.getClaim(worldId, x, z) != null) {
                            when (direction) {
                                0 -> z--
                                1 -> x++
                                2 -> z++
                                3 -> x--
                            }
                        }
                        claimService.save(MfClaimedChunk(worldId, x, z, createdFaction.id)).onFailure {
                            sender.sendMessage("${RED}Failed to save claimed chunk while generating data")
                            plugin.logger.log(SEVERE, "Failed to save claimed chunk: ${it.reason.message}", it.reason.cause)
                            return@forEach
                        }
                    }
                    sender.sendMessage("${GREEN}Claimed chunks for ${createdFaction.name}")
                }
                sender.sendMessage("${GREEN}Completed data generation.")
            }
        )
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = emptyList<String>()
}
