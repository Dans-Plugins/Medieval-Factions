package com.dansplugins.factionsystem.legacy

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.area.MfChunkPosition
import com.dansplugins.factionsystem.area.MfCuboidArea
import com.dansplugins.factionsystem.area.MfPosition
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionMember
import com.dansplugins.factionsystem.faction.role.MfFactionRoles
import com.dansplugins.factionsystem.gate.MfGate
import com.dansplugins.factionsystem.gate.MfGateId
import com.dansplugins.factionsystem.gate.MfGateStatus.CLOSED
import com.dansplugins.factionsystem.gate.MfGateStatus.OPEN
import com.dansplugins.factionsystem.locks.MfLockedBlock
import com.dansplugins.factionsystem.locks.MfLockedBlockId
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.relationship.MfFactionRelationship
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.ALLY
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.AT_WAR
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.LIEGE
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.VASSAL
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.forkhandles.result4k.onFailure
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter.ISO_INSTANT
import java.util.logging.Level.SEVERE
import kotlin.math.roundToInt

class MfLegacyDataMigrator(private val plugin: MedievalFactions) {

    fun backup() {
        plugin.logger.info("Backing up Medieval Factions 4 data...")
        val startTime = System.currentTimeMillis()
        val backupFolder = File(plugin.dataFolder, "mf4_backup")
        backupOldFiles(backupFolder)
        plugin.logger.info("Backed up Medieval Factions 4 data (${System.currentTimeMillis() - startTime}ms)")
    }

    fun migrate() {
        plugin.logger.info("Migrating Medieval Factions 4 data...")
        val startTime = System.currentTimeMillis()
        val backupFolder = File(plugin.dataFolder, "mf4_backup")
        migrateConfig(File(backupFolder, "config.yml"))
        val gson = Gson()
        migratePlayers(File(backupFolder, "playerpowerrecords.json"), gson)
        migrateFactions(File(backupFolder, "factions.json"), gson)
        migrateClaimedChunks(File(backupFolder, "claimedchunks.json"), gson)
        migrateLockedBlocks(File(backupFolder, "lockedblocks.json"), gson)
        plugin.logger.info("Migrated Medieval Factions 4 data (${System.currentTimeMillis() - startTime}ms)")
    }

    private fun backupOldFiles(backupFolder: File) {
        val files = plugin.dataFolder.listFiles() ?: return
        plugin.logger.info("Backing up old files to \"${backupFolder.path}\"...")
        val startTime = System.currentTimeMillis()
        if (backupFolder.exists()) {
            backupFolder.renameTo(File("mf4_backup_${ISO_INSTANT.format(Instant.ofEpochMilli(backupFolder.lastModified()))}"))
        }
        backupFolder.mkdirs()
        files.forEach { file ->
            file.renameTo(File(backupFolder, plugin.dataFolder.toURI().relativize(file.toURI()).path))
        }
        plugin.logger.info("Backup complete (${System.currentTimeMillis() - startTime}ms)")
    }

    private fun migrateConfig(oldConfigFile: File) {
        plugin.logger.info("Migrating config settings...")
        val startTime = System.currentTimeMillis()
        val oldConfig = YamlConfiguration.loadConfiguration(oldConfigFile)
        plugin.config.set("players.maxPower", oldConfig.getDouble("initialMaxPowerLevel"))
        plugin.config.set("players.initialPower", oldConfig.getDouble("initialPowerLevel"))
        plugin.config.set("factions.mobsSpawnInFactionTerritory", oldConfig.getBoolean("mobsSpawnInFactionTerritory"))
        plugin.config.set("players.hoursToReachMaxPower", ((oldConfig.getDouble("initialMaxPowerLevel") / oldConfig.getDouble("powerIncreaseAmount")) * (oldConfig.getDouble("minutesBetweenPowerIncreases") / 60.0)).roundToInt())
        plugin.config.set("factions.laddersPlaceableInEnemyFactionTerritory", oldConfig.getBoolean("laddersPlaceableInEnemyFactionTerritory"))
        plugin.config.set("pvp.warRequiredForPlayersOfDifferentFactions", oldConfig.getBoolean("warsRequiredForPVP"))
        plugin.config.set("factions.maxNameLength", oldConfig.getString("factionMaxNameLength"))
        plugin.config.set("gates.maxPerFaction", oldConfig.getInt("factionMaxNumberGates"))
        plugin.config.set("gates.maxBlocks", oldConfig.getInt("factionMaxGateArea"))
        plugin.config.set("factions.zeroPowerFactionsGetDisbanded", oldConfig.getBoolean("zeroPowerFactionsGetDisbanded"))
        plugin.config.set("factions.vassalPowerContributionMultiplier", oldConfig.getDouble("vassalContributionPercentageMultiplier"))
        plugin.config.set("factions.nonMembersCanInteractWithDoors", oldConfig.getBoolean("nonMembersCanInteractWithDoors"))
        plugin.config.set("chat.enableDefaultChatFormatting", oldConfig.getBoolean("playersChatWithPrefixes"))
        plugin.config.set("factions.maxClaimRadius", oldConfig.getBoolean("maxClaimRadius"))
        plugin.config.set("language", oldConfig.getString("languageid"))
        plugin.config.set("factions.titleTerritoryIndicator", oldConfig.getBoolean("territoryAlertPopUp"))
        plugin.config.set("factions.actionBarTerritoryIndicator", oldConfig.getBoolean("territoryIndicatorActionbar"))
        plugin.config.set("factions.allowNeutrality", oldConfig.getBoolean("allowNeutrality"))
        plugin.config.set("factions.limitLand", oldConfig.getBoolean("limitLand"))
        plugin.config.set("players.powerLostOnDeath", oldConfig.getDouble("powerLostOnDeath"))
        plugin.config.set("players.powerGainedOnKill", oldConfig.getDouble("powerGainedOnKill"))
        plugin.saveConfig()
        plugin.logger.info("Config migration complete (${System.currentTimeMillis() - startTime}ms)")
    }

    private fun migratePlayers(oldPlayerPowerRecordsFile: File, gson: Gson) {
        plugin.logger.info("Migrating player information...")
        val startTime = System.currentTimeMillis()
        val playerService = plugin.services.playerService
        val playerPowerRecords = gson.fromJson<List<MfLegacyPowerRecord>>(
            oldPlayerPowerRecordsFile.readText(),
            TypeToken.getParameterized(List::class.java, MfLegacyPowerRecord::class.java).type
        )
        plugin.server.offlinePlayers.map { bukkitPlayer ->
            val powerRecord = playerPowerRecords.singleOrNull { gson.fromJson(it.playerUUID, String::class.java) == bukkitPlayer.uniqueId.toString() }
            playerService.save(
                MfPlayer(
                    bukkitPlayer,
                    0,
                    powerRecord?.powerLevel?.toDoubleOrNull()
                        ?: plugin.config.getDouble("players.initialPower"),
                    powerRecord?.powerLevel?.toDoubleOrNull()
                        ?: plugin.config.getDouble("players.initialPower"),
                    false,
                    null
                )
            ).onFailure {
                plugin.logger.log(SEVERE, "Failed to save player \"${bukkitPlayer.uniqueId}\": ${it.reason.message}", it.reason.cause)
                return@map
            }
        }
        plugin.logger.info("Player information migrated (${System.currentTimeMillis() - startTime}ms)")
    }

    private fun migrateFactions(oldFactionsFile: File, gson: Gson) {
        plugin.logger.info("Migrating faction information...")
        val startTime = System.currentTimeMillis()
        val factionService = plugin.services.factionService
        val legacyFactions = gson.fromJson<List<MfLegacyFaction>>(
            oldFactionsFile.readText(),
            TypeToken.getParameterized(List::class.java, MfLegacyFaction::class.java).type
        )
        val factions = legacyFactions.associateWith { legacyFaction ->
            val name = gson.fromJson(legacyFaction.name, String::class.java)
            val description = gson.fromJson(legacyFaction.description, String::class.java)
            val memberUuids = gson.fromJson<List<String>>(legacyFaction.members, TypeToken.getParameterized(List::class.java, String::class.java).type)
            val officerUuids = gson.fromJson<List<String>>(legacyFaction.officers, TypeToken.getParameterized(List::class.java, String::class.java).type)
            val ownerUuid = gson.fromJson(legacyFaction.owner, String::class.java)
            val booleanFlagValues = gson.fromJson<Map<String, Boolean>>(
                legacyFaction.booleanFlagValues,
                TypeToken.getParameterized(Map::class.java, String::class.java, Boolean::class.javaObjectType).type
            )
            val stringFlagValues = gson.fromJson<Map<String, String>>(
                legacyFaction.stringFlagValues,
                TypeToken.getParameterized(Map::class.java, String::class.java, String::class.java).type
            )
            val prefix = gson.fromJson(legacyFaction.prefix, String::class.java)
            val location = gson.fromJson(legacyFaction.location, MfLegacyLocation::class.java)
            val worldName = gson.fromJson(location.worldName, String::class.java)
            val x = gson.fromJson(location.x, Double::class.javaObjectType)
            val y = gson.fromJson(location.y, Double::class.javaObjectType)
            val z = gson.fromJson(location.z, Double::class.javaObjectType)
            val bonusPower = gson.fromJson(legacyFaction.bonusPower, String::class.java).toDoubleOrNull() ?: 0.0

            val factionId = MfFactionId.generate()
            val roles = MfFactionRoles.defaults(plugin, factionId)
            return@associateWith factionService.save(
                MfFaction(
                    plugin,
                    factionId,
                    0,
                    name,
                    description,
                    memberUuids.map { uuid ->
                        MfFactionMember(
                            MfPlayerId(uuid),
                            when (uuid) {
                                ownerUuid -> roles.getRole("Owner")
                                in officerUuids -> roles.getRole("Officer")
                                else -> roles.getRole("Member")
                            }!!
                        )
                    },
                    emptyList(),
                    plugin.flags.defaults() + mapOf(
                        plugin.flags.alliesCanInteractWithLand to (booleanFlagValues["alliesCanInteractWithLand"] ?: plugin.flags.alliesCanInteractWithLand.defaultValue),
                        plugin.flags.vassalageTreeCanInteractWithLand to (booleanFlagValues["vassalageTreeCanInteractWithLand"] ?: plugin.flags.vassalageTreeCanInteractWithLand.defaultValue),
                        plugin.flags.isNeutral to (booleanFlagValues["neutral"] ?: plugin.flags.isNeutral.defaultValue),
                        plugin.flags.color to (stringFlagValues["dynmapTerritoryColor"] ?: plugin.flags.color.defaultValue),
                        plugin.flags.allowFriendlyFire to (booleanFlagValues["allowFriendlyFire"] ?: plugin.flags.allowFriendlyFire.defaultValue),
                        plugin.flags.acceptBonusPower to (booleanFlagValues["acceptBonusPower"] ?: plugin.flags.acceptBonusPower.defaultValue),
                        plugin.flags.enableMobProtection to (booleanFlagValues["enableMobProtection"] ?: plugin.flags.enableMobProtection.defaultValue)
                    ),
                    prefix,
                    if (worldName == null || x == null || y == null || z == null) {
                        null
                    } else {
                        plugin.server.getWorld(worldName)?.uid?.let { worldId ->
                            MfPosition(
                                worldId,
                                x,
                                y,
                                z,
                                0f,
                                0f
                            )
                        }
                    },
                    bonusPower,
                    false,
                    roles
                )
            ).onFailure {
                plugin.logger.log(SEVERE, "Failed to save faction \"${legacyFaction.name}\": ${it.reason.message}")
                return@associateWith null
            }
        }
        val factionRelationshipService = plugin.services.factionRelationshipService
        val gateService = plugin.services.gateService
        factions.forEach { (legacyFaction, faction) ->
            if (faction == null) return@forEach
            val enemyFactionNames = gson.fromJson<List<String>>(
                legacyFaction.enemyFactions,
                TypeToken.getParameterized(List::class.java, String::class.java).type
            )
            val enemyFactions = enemyFactionNames.mapNotNull(factionService::getFaction)
            enemyFactions.forEach createRelationships@{ enemyFaction ->
                factionRelationshipService.save(
                    MfFactionRelationship(
                        factionId = faction.id,
                        targetId = enemyFaction.id,
                        type = AT_WAR
                    )
                ).onFailure {
                    plugin.logger.log(SEVERE, "Failed to save faction relationship between \"${faction.name}\" and \"${enemyFaction.name}\": ${it.reason.message}", it.reason.cause)
                    return@createRelationships
                }
            }
            val allyFactionNames = gson.fromJson<List<String>>(
                legacyFaction.allyFactions,
                TypeToken.getParameterized(List::class.java, String::class.java).type
            )
            val allyFactions = allyFactionNames.mapNotNull(factionService::getFaction)
            allyFactions.forEach createRelationships@{ allyFaction ->
                factionRelationshipService.save(
                    MfFactionRelationship(
                        factionId = faction.id,
                        targetId = allyFaction.id,
                        type = ALLY
                    )
                ).onFailure {
                    plugin.logger.log(SEVERE, "Failed to save faction relationship between \"${faction.name}\" and \"${allyFaction.name}\": ${it.reason.message}", it.reason.cause)
                    return@createRelationships
                }
            }
            val vassalFactionNames = gson.fromJson<List<String>>(
                legacyFaction.vassals,
                TypeToken.getParameterized(List::class.java, String::class.java).type
            )
            val vassalFactions = vassalFactionNames.mapNotNull(factionService::getFaction)
            vassalFactions.forEach createRelationships@{ vassalFaction ->
                factionRelationshipService.save(
                    MfFactionRelationship(
                        factionId = faction.id,
                        targetId = vassalFaction.id,
                        type = VASSAL
                    )
                )
            }
            val liegeFactionName = gson.fromJson(legacyFaction.liege, String::class.java)
            val liegeFaction = factionService.getFaction(liegeFactionName)
            if (liegeFaction != null) {
                factionRelationshipService.save(
                    MfFactionRelationship(
                        factionId = faction.id,
                        targetId = liegeFaction.id,
                        type = LIEGE
                    )
                )
            }
            val gateJson = gson.fromJson<List<String>>(legacyFaction.factionGates, TypeToken.getParameterized(List::class.java, String::class.java).type)
            val gates = gateJson.map { gson.fromJson(it, MfLegacyGate::class.java) }
            gates.forEach migrateGate@{ legacyGate ->
                val position1 = parseGateCoord(legacyGate.coord1) ?: return@migrateGate
                val position2 = parseGateCoord(legacyGate.coord2) ?: return@migrateGate
                val triggerPosition = parseGateCoord(legacyGate.triggerCoord) ?: return@migrateGate
                val material = Material.valueOf(legacyGate.material)
                val isOpen = legacyGate.open.toBooleanStrict()
                gateService.save(
                    MfGate(
                        plugin,
                        MfGateId.generate(),
                        0,
                        faction.id,
                        MfCuboidArea(
                            position1,
                            position2
                        ),
                        triggerPosition,
                        material,
                        if (isOpen) OPEN else CLOSED
                    )
                ).onFailure {
                    plugin.logger.log(SEVERE, "Failed to save gate \"${legacyGate.name}\": ${it.reason.message}", it.reason.cause)
                    return@migrateGate
                }
            }
        }
        plugin.logger.info("Faction information migrated (${System.currentTimeMillis() - startTime}ms)")
    }

    private fun parseGateCoord(gateCoord: String): MfBlockPosition? {
        val parts = gateCoord.split(",")
        val world = plugin.server.getWorld(parts[3]) ?: return null
        return MfBlockPosition(
            world.uid,
            parts[0].toInt(),
            parts[1].toInt(),
            parts[2].toInt()
        )
    }

    private fun migrateClaimedChunks(oldClaimedChunksFile: File, gson: Gson) {
        plugin.logger.info("Migrating claimed chunks...")
        val startTime = System.currentTimeMillis()
        val legacyClaimedChunks = gson.fromJson<List<MfLegacyClaimedChunk>>(
            oldClaimedChunksFile.readText(),
            TypeToken.getParameterized(List::class.java, MfLegacyClaimedChunk::class.java).type
        )
        val claimService = plugin.services.claimService
        val factionService = plugin.services.factionService
        legacyClaimedChunks.forEach { legacyClaimedChunk ->
            val worldName = gson.fromJson(legacyClaimedChunk.world, String::class.java)
            val x = gson.fromJson(legacyClaimedChunk.X, Int::class.javaObjectType)
            val z = gson.fromJson(legacyClaimedChunk.Z, Int::class.javaObjectType)
            val world = plugin.server.getWorld(worldName)
            if (world == null) {
                plugin.logger.warning("Could not find world \"${worldName}\", skipping claimed chunk")
                return@forEach
            }
            val factionName = gson.fromJson(legacyClaimedChunk.holder, String::class.java)
            val faction = factionService.getFaction(factionName)
            if (faction == null) {
                plugin.logger.warning("Could not find faction \"${factionName}\", skipping claimed chunk")
                return@forEach
            }
            claimService.save(
                MfClaimedChunk(
                    MfChunkPosition(world.uid, x, z),
                    faction.id
                )
            ).onFailure {
                plugin.logger.log(SEVERE, "Failed to save claimed chunk $worldName, $x, $z: ${it.reason.message}", it.reason.cause)
                return@forEach
            }
        }
        plugin.logger.info("Claimed chunks migrated (${System.currentTimeMillis() - startTime}ms)")
    }

    private fun migrateLockedBlocks(oldLockedBlocksFile: File, gson: Gson) {
        plugin.logger.info("Migrating locked blocks...")
        val startTime = System.currentTimeMillis()
        val legacyLockedBlocks = gson.fromJson<List<MfLegacyLockedBlock>>(
            oldLockedBlocksFile.readText(),
            TypeToken.getParameterized(List::class.java, MfLegacyLockedBlock::class.java).type
        )
        val lockService = plugin.services.lockService
        val claimService = plugin.services.claimService
        legacyLockedBlocks.forEach { legacyLockedBlock ->
            val worldName = gson.fromJson(legacyLockedBlock.world, String::class.java)
            val x = gson.fromJson(legacyLockedBlock.X, Int::class.javaObjectType)
            val y = gson.fromJson(legacyLockedBlock.Y, Int::class.javaObjectType)
            val z = gson.fromJson(legacyLockedBlock.Z, Int::class.javaObjectType)
            val world = plugin.server.getWorld(worldName)
            if (world == null) {
                plugin.logger.warning("Could not find world \"${worldName}\", skipping locked block")
                return@forEach
            }
            val blockPosition = MfBlockPosition(
                world.uid,
                x,
                y,
                z
            )
            val bukkitBlock = blockPosition.toBukkitBlock()
            if (bukkitBlock == null) {
                plugin.logger.warning("Could not find block at ${blockPosition.worldId}, ${blockPosition.x}, ${blockPosition.y}, ${blockPosition.z}, skipping locked block")
                return@forEach
            }
            val chunk = bukkitBlock.chunk
            val chunkX = chunk.x
            val chunkZ = chunk.z
            val claim = claimService.getClaim(chunk)
            if (claim == null) {
                plugin.logger.warning("Could not find claim at ${blockPosition.worldId}, $chunkX, $chunkZ, skipping locked block")
                return@forEach
            }
            val ownerId = gson.fromJson(legacyLockedBlock.owner, String::class.java)
            val accessList = gson.fromJson<List<String?>>(legacyLockedBlock.accessList, TypeToken.getParameterized(List::class.java, String::class.java).type)
            lockService.save(
                MfLockedBlock(
                    MfLockedBlockId.generate(),
                    0,
                    blockPosition,
                    chunkX,
                    chunkZ,
                    MfPlayerId(ownerId),
                    accessList.filterNotNull().map(::MfPlayerId)
                )
            ).onFailure {
                plugin.logger.log(SEVERE, "Failed to save locked block ${world.name}, $x, $y, $z: ${it.reason.message}", it.reason.cause)
                return@forEach
            }
        }
        plugin.logger.info("Locked blocks migrated (${System.currentTimeMillis() - startTime}ms)")
    }
}
