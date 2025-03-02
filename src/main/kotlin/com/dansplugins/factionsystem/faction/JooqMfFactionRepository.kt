package com.dansplugins.factionsystem.faction

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfPosition
import com.dansplugins.factionsystem.faction.flag.MfFlagValues
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId
import com.dansplugins.factionsystem.faction.role.MfFactionRoles
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.jooq.Tables.MF_FACTION
import com.dansplugins.factionsystem.jooq.Tables.MF_FACTION_APPLICATION
import com.dansplugins.factionsystem.jooq.Tables.MF_FACTION_INVITE
import com.dansplugins.factionsystem.jooq.Tables.MF_FACTION_MEMBER
import com.dansplugins.factionsystem.jooq.tables.records.MfFactionApplicationRecord
import com.dansplugins.factionsystem.jooq.tables.records.MfFactionInviteRecord
import com.dansplugins.factionsystem.jooq.tables.records.MfFactionMemberRecord
import com.dansplugins.factionsystem.jooq.tables.records.MfFactionRecord
import com.dansplugins.factionsystem.player.MfPlayerId
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.JSON
import org.jooq.impl.DSL.`val`
import java.util.*

class JooqMfFactionRepository(
    private val plugin: MedievalFactions,
    private val dsl: DSLContext,
    private val gson: Gson
) : MfFactionRepository {

    override fun getFaction(id: MfFactionId): MfFaction? = getFaction(MF_FACTION.ID.eq(id.value))
    override fun getFaction(name: String): MfFaction? = getFaction(MF_FACTION.NAME.eq(name))
    override fun getFaction(playerId: MfPlayerId): MfFaction? = getFaction(
        `val`(playerId.value)
            .`in`(
                dsl.select(MF_FACTION_MEMBER.PLAYER_ID)
                    .from(MF_FACTION_MEMBER)
                    .where(MF_FACTION_MEMBER.FACTION_ID.eq(MF_FACTION.ID))
            )
    )

    override fun getFactions(): List<MfFaction> {
        return dsl.selectFrom(MF_FACTION).fetch()
            .map { factionRecord ->
                val roles = gson.fromJson<List<Map<String, Any?>>>(
                    factionRecord.roles.data(),
                    TypeToken.getParameterized(
                        List::class.java,
                        TypeToken.getParameterized(
                            Map::class.java,
                            String::class.java,
                            Any::class.java
                        ).type
                    ).type
                ).map(MfFactionRole.Companion::deserialize)
                val members = dsl.selectFrom(MF_FACTION_MEMBER).where(MF_FACTION_MEMBER.FACTION_ID.eq(factionRecord.id))
                    .fetch().map { it.toDomain(roles) }
                val invites = dsl.selectFrom(MF_FACTION_INVITE).where(MF_FACTION_INVITE.FACTION_ID.eq(factionRecord.id))
                    .fetch().map { it.toDomain() }
                val applications = dsl.selectFrom(MF_FACTION_APPLICATION).where(MF_FACTION_APPLICATION.FACTION_ID.eq(factionRecord.id))
                    .fetch().map { it.toDomain() }
                return@map factionRecord.toDomain(
                    members = members,
                    invites = invites,
                    roles = roles,
                    applications = applications
                )
            }
    }

    private fun getFaction(condition: Condition): MfFaction? {
        val factionRecord = dsl.selectFrom(MF_FACTION).where(condition).fetchOne() ?: return null
        val roles = gson.fromJson<List<Map<String, Any?>>>(
            factionRecord.roles.data(),
            TypeToken.getParameterized(
                List::class.java,
                TypeToken.getParameterized(
                    Map::class.java,
                    String::class.java,
                    Any::class.java
                ).type
            ).type
        ).map(MfFactionRole.Companion::deserialize)
        val members = dsl.selectFrom(MF_FACTION_MEMBER).where(MF_FACTION_MEMBER.FACTION_ID.eq(factionRecord.id))
            .fetch().map { it.toDomain(roles) }
        val invites = dsl.selectFrom(MF_FACTION_INVITE).where(MF_FACTION_INVITE.FACTION_ID.eq(factionRecord.id))
            .fetch().map { it.toDomain() }
        val applications = dsl.selectFrom(MF_FACTION_APPLICATION).where(MF_FACTION_APPLICATION.FACTION_ID.eq(factionRecord.id))
            .fetch().map { it.toDomain() }
        return factionRecord.toDomain(
            members = members,
            invites = invites,
            roles = roles,
            applications = applications
        )
    }

    override fun upsert(faction: MfFaction): MfFaction {
        return dsl.transactionResult { config ->
            val transactionalDsl = config.dsl()
            val newState = upsertFaction(transactionalDsl, faction)

            deleteMembers(transactionalDsl, faction.id)
            val newMembers = faction.members.map { upsertMember(transactionalDsl, faction.id, it, newState.roles) }
            deleteInvites(transactionalDsl, faction.id)
            val newInvites = faction.invites.map { upsertInvite(transactionalDsl, faction.id, it) }
            deleteApplications(transactionalDsl, faction.id)
            val newApplications = faction.applications.map { upsertApplication(transactionalDsl, faction.id, it) }

            return@transactionResult newState.copy(
                members = newMembers,
                invites = newInvites,
                applications = newApplications
            )
        }
    }

    private fun upsertFaction(dsl: DSLContext, faction: MfFaction): MfFaction {
        val rowCount = dsl.insertInto(MF_FACTION)
            .set(MF_FACTION.ID, faction.id.value)
            .set(MF_FACTION.VERSION, 1)
            .set(MF_FACTION.NAME, faction.name)
            .set(MF_FACTION.DESCRIPTION, faction.description)
            .set(MF_FACTION.FLAGS, JSON.valueOf(gson.toJson(faction.flags.valuesByName)))
            .set(MF_FACTION.PREFIX, faction.prefix)
            .set(MF_FACTION.HOME_WORLD_ID, faction.home?.worldId?.toString())
            .set(MF_FACTION.HOME_X, faction.home?.x)
            .set(MF_FACTION.HOME_Y, faction.home?.y)
            .set(MF_FACTION.HOME_Z, faction.home?.z)
            .set(MF_FACTION.HOME_YAW, faction.home?.yaw)
            .set(MF_FACTION.HOME_PITCH, faction.home?.pitch)
            .set(MF_FACTION.BONUS_POWER, faction.bonusPower)
            .set(MF_FACTION.AUTOCLAIM, faction.autoclaim)
            .set(
                MF_FACTION.ROLES,
                JSON.valueOf(
                    gson.toJson(faction.roles.map(MfFactionRole::serialize))
                )
            )
            .set(MF_FACTION.DEFAULT_ROLE_ID, faction.roles.default.id.value)
            .set(
                MF_FACTION.DEFAULT_PERMISSIONS,
                JSON.valueOf(
                    gson.toJson(faction.defaultPermissions.mapKeys { it.key.name })
                )
            )
            .onConflict(MF_FACTION.ID).doUpdate()
            .set(MF_FACTION.NAME, faction.name)
            .set(MF_FACTION.DESCRIPTION, faction.description)
            .set(MF_FACTION.FLAGS, JSON.valueOf(gson.toJson(faction.flags.valuesByName)))
            .set(MF_FACTION.PREFIX, faction.prefix)
            .set(MF_FACTION.HOME_WORLD_ID, faction.home?.worldId?.toString())
            .set(MF_FACTION.HOME_X, faction.home?.x)
            .set(MF_FACTION.HOME_Y, faction.home?.y)
            .set(MF_FACTION.HOME_Z, faction.home?.z)
            .set(MF_FACTION.HOME_YAW, faction.home?.yaw)
            .set(MF_FACTION.HOME_PITCH, faction.home?.pitch)
            .set(MF_FACTION.BONUS_POWER, faction.bonusPower)
            .set(MF_FACTION.AUTOCLAIM, faction.autoclaim)
            .set(MF_FACTION.ROLES, JSON.valueOf(gson.toJson(faction.roles.map(MfFactionRole::serialize))))
            .set(MF_FACTION.DEFAULT_ROLE_ID, faction.roles.default.id.value)
            .set(
                MF_FACTION.DEFAULT_PERMISSIONS,
                JSON.valueOf(
                    gson.toJson(faction.defaultPermissions.mapKeys { it.key.name })
                )
            )
            .set(MF_FACTION.VERSION, faction.version + 1)
            .where(MF_FACTION.ID.eq(faction.id.value))
            .and(MF_FACTION.VERSION.eq(faction.version))
            .execute()
        if (rowCount == 0) throw OptimisticLockingFailureException("Invalid version: ${faction.version}")
        return dsl.selectFrom(MF_FACTION)
            .where(MF_FACTION.ID.eq(faction.id.value))
            .fetchOne()
            .let(::requireNotNull)
            .toDomain()
    }

    private fun deleteMembers(dsl: DSLContext, factionId: MfFactionId) {
        dsl.deleteFrom(MF_FACTION_MEMBER)
            .where(MF_FACTION_MEMBER.FACTION_ID.eq(factionId.value))
            .execute()
    }

    private fun upsertMember(dsl: DSLContext, factionId: MfFactionId, member: MfFactionMember, roles: List<MfFactionRole>): MfFactionMember {
        dsl.insertInto(MF_FACTION_MEMBER)
            .set(MF_FACTION_MEMBER.FACTION_ID, factionId.value)
            .set(MF_FACTION_MEMBER.PLAYER_ID, member.playerId.value)
            .set(MF_FACTION_MEMBER.ROLE_ID, member.role.id.value)
            .onConflict(MF_FACTION_MEMBER.FACTION_ID, MF_FACTION_MEMBER.PLAYER_ID).doUpdate()
            .set(MF_FACTION_MEMBER.ROLE_ID, member.role.id.value)
            .where(MF_FACTION_MEMBER.FACTION_ID.eq(factionId.value))
            .and(MF_FACTION_MEMBER.PLAYER_ID.eq(member.playerId.value))
            .execute()
        return dsl.selectFrom(MF_FACTION_MEMBER)
            .where(MF_FACTION_MEMBER.FACTION_ID.eq(factionId.value))
            .and(MF_FACTION_MEMBER.PLAYER_ID.eq(member.playerId.value))
            .fetchOne()
            .let(::requireNotNull)
            .toDomain(roles)
    }

    private fun deleteInvites(dsl: DSLContext, factionId: MfFactionId) {
        dsl.deleteFrom(MF_FACTION_INVITE)
            .where(MF_FACTION_INVITE.FACTION_ID.eq(factionId.value))
            .execute()
    }

    private fun upsertInvite(dsl: DSLContext, factionId: MfFactionId, invite: MfFactionInvite): MfFactionInvite {
        dsl.insertInto(MF_FACTION_INVITE)
            .set(MF_FACTION_INVITE.FACTION_ID, factionId.value)
            .set(MF_FACTION_INVITE.PLAYER_ID, invite.playerId.value)
            .onConflict(MF_FACTION_INVITE.FACTION_ID, MF_FACTION_INVITE.PLAYER_ID).doNothing()
            .execute()
        return dsl.selectFrom(MF_FACTION_INVITE)
            .where(MF_FACTION_INVITE.FACTION_ID.eq(factionId.value))
            .and(MF_FACTION_INVITE.PLAYER_ID.eq(invite.playerId.value))
            .fetchOne()
            .let(::requireNotNull)
            .toDomain()
    }

    private fun deleteApplications(dsl: DSLContext, factionId: MfFactionId) {
        dsl.deleteFrom(MF_FACTION_APPLICATION)
            .where(MF_FACTION_APPLICATION.FACTION_ID.eq(factionId.value))
            .execute()
    }

    private fun upsertApplication(dsl: DSLContext, factionId: MfFactionId, application: MfFactionApplication): MfFactionApplication {
        dsl.insertInto(MF_FACTION_APPLICATION)
            .set(MF_FACTION_APPLICATION.FACTION_ID, factionId.value)
            .set(MF_FACTION_APPLICATION.PLAYER_ID, application.applicantId.value)
            .onConflict(MF_FACTION_APPLICATION.FACTION_ID, MF_FACTION_APPLICATION.PLAYER_ID).doNothing()
            .execute()
        return dsl.selectFrom(MF_FACTION_APPLICATION)
            .where(MF_FACTION_APPLICATION.FACTION_ID.eq(factionId.value))
            .and(MF_FACTION_APPLICATION.PLAYER_ID.eq(application.applicantId.value))
            .fetchOne()
            .let(::requireNotNull)
            .toDomain()
    }

    override fun delete(factionId: MfFactionId) {
        dsl.deleteFrom(MF_FACTION)
            .where(MF_FACTION.ID.eq(factionId.value))
            .execute()
    }

    private fun MfFactionRecord.toDomain(members: List<MfFactionMember> = emptyList(), invites: List<MfFactionInvite> = emptyList(), roles: List<MfFactionRole>? = null, applications: List<MfFactionApplication> = emptyList()): MfFaction {
        val factionRoles = MfFactionRoles(
            defaultRoleId = defaultRoleId.let(::MfFactionRoleId),
            roles = roles ?: gson.fromJson<List<Map<String, Any?>>>(
                this.roles.data(),
                TypeToken.getParameterized(
                    List::class.java,
                    TypeToken.getParameterized(
                        Map::class.java,
                        String::class.java,
                        Any::class.java
                    ).type
                ).type
            ).map(MfFactionRole.Companion::deserialize)
        )
        return MfFaction(
            plugin = plugin,
            id = id.let(::MfFactionId),
            version = version,
            name = name,
            description = description,
            members = members,
            invites = invites,
            flags = MfFlagValues(
                plugin,
                gson.fromJson(
                    flags.data(),
                    TypeToken.getParameterized(
                        Map::class.java,
                        String::class.java,
                        Any::class.java
                    ).type
                )
            ),
            prefix = prefix,
            home = homeWorldId?.let {
                MfPosition(
                    UUID.fromString(it),
                    homeX,
                    homeY,
                    homeZ,
                    homeYaw.toFloat(),
                    homePitch.toFloat()
                )
            },
            bonusPower = bonusPower,
            autoclaim = autoclaim,
            roles = factionRoles,
            defaultPermissionsByName = gson.fromJson(
                defaultPermissions.data(),
                TypeToken.getParameterized(
                    Map::class.java,
                    String::class.java,
                    Boolean::class.javaObjectType
                ).type
            ),
            applications = applications
        )
    }

    private fun MfFactionMemberRecord.toDomain(roles: List<MfFactionRole>): MfFactionMember = MfFactionMember(
        playerId.let(::MfPlayerId),
        roles.single { it.id.value == roleId }
    )

    private fun MfFactionInviteRecord.toDomain() =
        MfFactionInvite(
            playerId.let(::MfPlayerId)
        )

    private fun MfFactionApplicationRecord.toDomain(): MfFactionApplication {
        return MfFactionApplication(
            applicantId = MfPlayerId(this.playerId),
            factionId = MfFactionId(this.factionId)
        )
    }
}
