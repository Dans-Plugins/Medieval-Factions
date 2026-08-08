package com.dansplugins.factionsystem.storage.migration

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.chat.MfChatChannelMessageRepository
import com.dansplugins.factionsystem.claim.MfClaimedChunkRepository
import com.dansplugins.factionsystem.duel.MfDuelInviteRepository
import com.dansplugins.factionsystem.duel.MfDuelRepository
import com.dansplugins.factionsystem.faction.MfFactionRepository
import com.dansplugins.factionsystem.gate.MfGateCreationContextRepository
import com.dansplugins.factionsystem.gate.MfGateRepository
import com.dansplugins.factionsystem.interaction.MfInteractionStatusRepository
import com.dansplugins.factionsystem.law.MfLawRepository
import com.dansplugins.factionsystem.locks.MfLockRepository
import com.dansplugins.factionsystem.player.MfPlayerRepository
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipRepository

/**
 * Migrates data from JSON storage to database storage
 */
class JsonToDatabaseMigrator(
    private val plugin: MedievalFactions,
    private val sourcePlayerRepo: MfPlayerRepository,
    private val sourceFactionRepo: MfFactionRepository,
    private val sourceLawRepo: MfLawRepository,
    private val sourceRelationshipRepo: MfFactionRelationshipRepository,
    private val sourceClaimRepo: MfClaimedChunkRepository,
    private val sourceLockRepo: MfLockRepository,
    private val sourceInteractionRepo: MfInteractionStatusRepository,
    private val sourceGateRepo: MfGateRepository,
    private val sourceGateContextRepo: MfGateCreationContextRepository,
    private val sourceChatRepo: MfChatChannelMessageRepository,
    private val sourceDuelRepo: MfDuelRepository,
    private val sourceDuelInviteRepo: MfDuelInviteRepository,
    private val targetPlayerRepo: MfPlayerRepository,
    private val targetFactionRepo: MfFactionRepository,
    private val targetLawRepo: MfLawRepository,
    private val targetRelationshipRepo: MfFactionRelationshipRepository,
    private val targetClaimRepo: MfClaimedChunkRepository,
    private val targetLockRepo: MfLockRepository,
    private val targetInteractionRepo: MfInteractionStatusRepository,
    private val targetGateRepo: MfGateRepository,
    private val targetGateContextRepo: MfGateCreationContextRepository,
    private val targetChatRepo: MfChatChannelMessageRepository,
    private val targetDuelRepo: MfDuelRepository,
    private val targetDuelInviteRepo: MfDuelInviteRepository
) {

    fun migrate(): MigrationResult {
        plugin.logger.info("Starting migration from JSON to database...")
        val startTime = System.currentTimeMillis()

        val existingTargetData = describeExistingTargetData()
        if (existingTargetData.isNotEmpty()) {
            val message = "Target storage already contains ${existingTargetData.joinToString(", ")}. " +
                "Migrating into a non-empty target is not supported. Stop the server, drop or empty the Medieval Factions tables in the target database, then run the migration again."
            plugin.logger.severe(message)
            return MigrationResult(
                success = false,
                itemsMigrated = 0,
                durationMs = System.currentTimeMillis() - startTime,
                message = message
            )
        }

        var totalMigrated = 0
        try {
            // Migrate players
            plugin.logger.info("Migrating players...")
            val players = sourcePlayerRepo.getPlayers()
            players.forEach { player ->
                targetPlayerRepo.upsert(player)
            }
            totalMigrated += players.size
            plugin.logger.info("Migrated ${players.size} players")

            // Migrate factions
            plugin.logger.info("Migrating factions...")
            val factions = sourceFactionRepo.getFactions()
            factions.forEach { faction ->
                targetFactionRepo.upsert(faction)
            }
            totalMigrated += factions.size
            plugin.logger.info("Migrated ${factions.size} factions")

            // Migrate laws
            plugin.logger.info("Migrating laws...")
            var lawCount = 0
            factions.forEach { faction ->
                val laws = sourceLawRepo.getLaws(faction.id)
                laws.forEach { law ->
                    targetLawRepo.upsert(law)
                    lawCount++
                }
            }
            totalMigrated += lawCount
            plugin.logger.info("Migrated $lawCount laws")

            // Migrate relationships
            plugin.logger.info("Migrating relationships...")
            val relationships = sourceRelationshipRepo.getFactionRelationships()
            relationships.forEach { relationship ->
                targetRelationshipRepo.upsert(relationship)
            }
            totalMigrated += relationships.size
            plugin.logger.info("Migrated ${relationships.size} relationships")

            // Migrate claims
            plugin.logger.info("Migrating claims...")
            val claims = sourceClaimRepo.getClaims()
            claims.forEach { claim ->
                targetClaimRepo.upsert(claim)
            }
            totalMigrated += claims.size
            plugin.logger.info("Migrated ${claims.size} claims")

            // Migrate locks
            plugin.logger.info("Migrating locks...")
            val locks = sourceLockRepo.getLockedBlocks()
            locks.forEach { lock ->
                targetLockRepo.upsert(lock)
            }
            totalMigrated += locks.size
            plugin.logger.info("Migrated ${locks.size} locked blocks")

            // Migrate interaction statuses
            plugin.logger.info("Migrating interaction statuses...")
            var interactionCount = 0
            players.forEach { player ->
                val status = sourceInteractionRepo.getInteractionStatus(player.id)
                if (status != null) {
                    targetInteractionRepo.setInteractionStatus(player.id, status)
                    interactionCount++
                }
            }
            totalMigrated += interactionCount
            plugin.logger.info("Migrated $interactionCount interaction statuses")

            // Migrate gates
            plugin.logger.info("Migrating gates...")
            val gates = sourceGateRepo.getGates()
            gates.forEach { gate ->
                targetGateRepo.upsert(gate)
            }
            totalMigrated += gates.size
            plugin.logger.info("Migrated ${gates.size} gates")

            // Migrate gate creation contexts
            plugin.logger.info("Migrating gate creation contexts...")
            var contextCount = 0
            players.forEach { player ->
                val context = sourceGateContextRepo.getContext(player.id)
                if (context != null) {
                    targetGateContextRepo.upsert(context)
                    contextCount++
                }
            }
            totalMigrated += contextCount
            plugin.logger.info("Migrated $contextCount gate creation contexts")

            // Migrate chat messages
            plugin.logger.info("Migrating chat messages...")
            var messageCount = 0
            factions.forEach { faction ->
                val messages = sourceChatRepo.getChatChannelMessages(faction.id)
                messages.forEach { message ->
                    targetChatRepo.insert(message)
                    messageCount++
                }
            }
            totalMigrated += messageCount
            plugin.logger.info("Migrated $messageCount chat messages")

            // Migrate duels
            plugin.logger.info("Migrating duels...")
            val duels = sourceDuelRepo.getDuels()
            duels.forEach { duel ->
                targetDuelRepo.upsert(duel)
            }
            totalMigrated += duels.size
            plugin.logger.info("Migrated ${duels.size} duels")

            // Migrate duel invites
            plugin.logger.info("Migrating duel invites...")
            val duelInvites = sourceDuelInviteRepo.getInvites()
            duelInvites.forEach { invite ->
                targetDuelInviteRepo.upsert(invite)
            }
            totalMigrated += duelInvites.size
            plugin.logger.info("Migrated ${duelInvites.size} duel invites")

            val duration = System.currentTimeMillis() - startTime
            plugin.logger.info("Migration completed successfully! Migrated $totalMigrated items in ${duration}ms")

            return MigrationResult(
                success = true,
                itemsMigrated = totalMigrated,
                durationMs = duration,
                message = "Migration completed successfully"
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            plugin.logger.severe("Migration failed: ${e.message}")
            e.printStackTrace()
            return MigrationResult(
                success = false,
                itemsMigrated = totalMigrated,
                durationMs = duration,
                message = "Migration failed after migrating $totalMigrated items, so the target is " +
                    "partially written and must be cleared before retrying: ${e.message}",
                error = e
            )
        }
    }

    /**
     * Names the data already present in the target, so a migration can refuse to run into a non-empty
     * target rather than merging two data sets. Merging is not a supported operation: entity versions
     * from the two backends diverge independently, so an upsert of an entity that already exists at a
     * different version throws and aborts the migration partway, leaving the target half-written.
     */
    private fun describeExistingTargetData(): List<String> {
        val existing = mutableListOf<String>()
        targetPlayerRepo.getPlayers().size.takeIf { it > 0 }?.let { existing += "$it players" }
        targetFactionRepo.getFactions().size.takeIf { it > 0 }?.let { existing += "$it factions" }
        targetRelationshipRepo.getFactionRelationships().size.takeIf { it > 0 }?.let { existing += "$it faction relationships" }
        targetClaimRepo.getClaims().size.takeIf { it > 0 }?.let { existing += "$it claims" }
        targetLockRepo.getLockedBlocks().size.takeIf { it > 0 }?.let { existing += "$it locked blocks" }
        targetGateRepo.getGates().size.takeIf { it > 0 }?.let { existing += "$it gates" }
        targetDuelRepo.getDuels().size.takeIf { it > 0 }?.let { existing += "$it duels" }
        return existing
    }
}
