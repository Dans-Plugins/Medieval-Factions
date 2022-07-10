package dansplugins.factionsystem.faction

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dansplugins.factionsystem.MedievalFactions
import dansplugins.factionsystem.area.MfPosition
import dansplugins.factionsystem.faction.flag.MfFlagValues
import dansplugins.factionsystem.faction.permission.MfFactionPermission
import dansplugins.factionsystem.faction.role.MfFactionRole
import dansplugins.factionsystem.faction.role.MfFactionRoles
import dansplugins.factionsystem.failure.OptimisticLockingFailureException
import dansplugins.factionsystem.jooq.Tables.*
import dansplugins.factionsystem.jooq.tables.records.MfFactionInviteRecord
import dansplugins.factionsystem.jooq.tables.records.MfFactionMemberRecord
import dansplugins.factionsystem.jooq.tables.records.MfFactionRecord
import dansplugins.factionsystem.player.MfPlayer
import dansplugins.factionsystem.player.MfPlayerId
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.JSON
import org.jooq.impl.DSL.`val`

class JooqMfFactionRepository(
    private val plugin: MedievalFactions,
    private val dsl: DSLContext
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

    private fun getFaction(condition: Condition): MfFaction? {
        val factionRecord = dsl.selectFrom(MF_FACTION).where(condition).fetchOne() ?: return null
        val roles = Gson().fromJson<List<MfFactionRole>>(
            factionRecord.roles.data(),
            TypeToken.getParameterized(List::class.java, MfFactionRole::class.java).type
        )
        val members = dsl.selectFrom(MF_FACTION_MEMBER).where(MF_FACTION_MEMBER.FACTION_ID.eq(factionRecord.id))
            .fetch().map { it.toDomain(roles) }
        val invites = dsl.selectFrom(MF_FACTION_INVITE).where(MF_FACTION_INVITE.FACTION_ID.eq(factionRecord.id))
            .fetch().map { it.toDomain() }
        return factionRecord.toDomain(
            members = members,
            invites = invites
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

            return@transactionResult newState.copy(
                members = newMembers,
                invites = newInvites
            )
        }
    }

    private fun upsertFaction(dsl: DSLContext, faction: MfFaction): MfFaction {
        val rowCount = dsl.insertInto(MF_FACTION)
            .set(MF_FACTION.ID, faction.id.value)
            .set(MF_FACTION.VERSION, 1)
            .set(MF_FACTION.NAME, faction.name)
            .set(MF_FACTION.DESCRIPTION, faction.description)
            .set(MF_FACTION.FLAGS, JSON.valueOf(Gson().toJson(faction.flags.serialize())))
            .set(MF_FACTION.LIEGE_ID, faction.liegeId?.value)
            .set(MF_FACTION.PREFIX, faction.prefix)
            .set(MF_FACTION.HOME_WORLD, faction.home?.worldName)
            .set(MF_FACTION.HOME_X, faction.home?.x)
            .set(MF_FACTION.HOME_Y, faction.home?.y)
            .set(MF_FACTION.HOME_Z, faction.home?.z)
            .set(MF_FACTION.HOME_YAW, faction.home?.yaw)
            .set(MF_FACTION.HOME_PITCH, faction.home?.pitch)
            .set(MF_FACTION.BONUS_POWER, faction.bonusPower)
            .set(MF_FACTION.AUTOCLAIM, faction.autoclaim)
            .set(MF_FACTION.ROLES, JSON.valueOf(
                Gson().toJson(faction.roles)
            ))
            .set(MF_FACTION.DEFAULT_ROLE_ID, faction.roles.default.id.value)
            .set(MF_FACTION.DEFAULT_PERMISSIONS, JSON.valueOf(
                Gson().toJson(faction.defaultPermissions.mapKeys { it.key.name })
            ))
            .onConflict(MF_FACTION.ID).doUpdate()
            .set(MF_FACTION.VERSION, faction.version + 1)
            .set(MF_FACTION.NAME, faction.name)
            .set(MF_FACTION.DESCRIPTION, faction.description)
            .set(MF_FACTION.FLAGS, JSON.valueOf(Gson().toJson(faction.flags)))
            .set(MF_FACTION.LIEGE_ID, faction.liegeId?.value)
            .set(MF_FACTION.PREFIX, faction.prefix)
            .set(MF_FACTION.HOME_WORLD, faction.home?.worldName)
            .set(MF_FACTION.HOME_X, faction.home?.x)
            .set(MF_FACTION.HOME_Y, faction.home?.y)
            .set(MF_FACTION.HOME_Z, faction.home?.z)
            .set(MF_FACTION.HOME_YAW, faction.home?.yaw)
            .set(MF_FACTION.HOME_PITCH, faction.home?.pitch)
            .set(MF_FACTION.BONUS_POWER, faction.bonusPower)
            .set(MF_FACTION.AUTOCLAIM, faction.autoclaim)
            .set(MF_FACTION.ROLES, JSON.valueOf(Gson().toJson(faction.roles)))
            .set(MF_FACTION.DEFAULT_ROLE_ID, faction.roles.default.id.value)
            .set(MF_FACTION.DEFAULT_PERMISSIONS, JSON.valueOf(
                Gson().toJson(faction.defaultPermissions.mapKeys { it.key.name })
            ))
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
            .set(MF_FACTION_MEMBER.PLAYER_ID, member.player.id.value)
            .set(MF_FACTION_MEMBER.ROLE_ID, member.role.id.value)
            .onConflict(MF_FACTION_MEMBER.FACTION_ID, MF_FACTION_MEMBER.PLAYER_ID).doUpdate()
            .set(MF_FACTION_MEMBER.ROLE_ID, member.role.id.value)
            .where(MF_FACTION_MEMBER.FACTION_ID.eq(factionId.value))
            .and(MF_FACTION_MEMBER.PLAYER_ID.eq(member.player.id.value))
            .execute()
        return dsl.selectFrom(MF_FACTION_MEMBER)
            .where(MF_FACTION_MEMBER.FACTION_ID.eq(factionId.value))
            .and(MF_FACTION_MEMBER.PLAYER_ID.eq(member.player.id.value))
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
            .set(MF_FACTION_INVITE.PLAYER_ID, invite.player.id.value)
            .onConflict(MF_FACTION_INVITE.FACTION_ID, MF_FACTION_INVITE.PLAYER_ID).doNothing()
            .execute()
        return dsl.selectFrom(MF_FACTION_INVITE)
            .where(MF_FACTION_INVITE.FACTION_ID.eq(factionId.value))
            .and(MF_FACTION_INVITE.PLAYER_ID.eq(invite.player.id.value))
            .fetchOne()
            .let(::requireNotNull)
            .toDomain()
    }

    private fun MfFactionRecord.toDomain(members: List<MfFactionMember> = emptyList(), invites: List<MfFactionInvite> = emptyList()): MfFaction {
        val roles = Gson().fromJson<List<MfFactionRole>>(roles.data(), TypeToken.getParameterized(List::class.java, MfFactionRole::class.java).type)
        return MfFaction(
            plugin = plugin,
            id = id.let(::MfFactionId),
            version = version,
            name = name,
            description = description,
            members = members,
            invites = invites,
            flags = MfFlagValues.deserialize(
                Gson().fromJson(
                    flags.data(),
                    TypeToken.getParameterized(
                        Map::class.java,
                        String::class.java,
                        Any::class.java
                    ).type
                )
            ),
            liegeId = liegeId?.let(::MfFactionId),
            prefix = prefix,
            home = homeWorld?.let {
                MfPosition(
                    it,
                    homeX,
                    homeY,
                    homeZ,
                    homeYaw.toFloat(),
                    homePitch.toFloat()
                )
            },
            bonusPower = bonusPower,
            autoclaim = autoclaim,
            roles = MfFactionRoles(
                default = roles.single { it.id.value == defaultRoleId },
                roles = roles
            ),
            defaultPermissions = Gson().fromJson<Map<String, Boolean>>(
                defaultPermissions.data(),
                TypeToken.getParameterized(
                    Map::class.java,
                    String::class.java,
                    Boolean::class.javaObjectType
                ).type
            ).mapKeys { MfFactionPermission.valueOf(it.key) }
        )
    }

    private fun MfFactionMemberRecord.toDomain(roles: List<MfFactionRole>) =
        MfFactionMember(
            MfPlayer(playerId.let(::MfPlayerId)),
            roles.single { it.id.value == roleId }
        )

    private fun MfFactionInviteRecord.toDomain() =
        MfFactionInvite(
            MfPlayer(playerId.let(::MfPlayerId))
        )

}