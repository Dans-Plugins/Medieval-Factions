package com.dansplugins.factionsystem.command.faction.claim

/** The shape of the area `/f claim` claims around the sender's chunk. */
enum class MfClaimShape(val permission: String, val usageKey: String?) {
    CIRCLE("mf.claim.circle", null),
    SQUARE("mf.claim.square", "CommandFactionClaimSquareUsage"),
    RECTANGLE("mf.claim.rectangle", "CommandFactionClaimRectangleUsage")
}
