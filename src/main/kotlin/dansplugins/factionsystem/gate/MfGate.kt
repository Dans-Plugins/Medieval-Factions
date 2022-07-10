package dansplugins.factionsystem.gate

import dansplugins.factionsystem.area.MfCuboidArea
import dansplugins.factionsystem.area.MfBlockPosition
import dansplugins.factionsystem.faction.MfFaction
import dansplugins.factionsystem.gate.MfGateStatus.READY
import org.bukkit.Material
import org.bukkit.Material.IRON_BARS

data class MfGate(
    val id: MfGateId,
    val name: String,
    val faction: MfFaction,
    val isOpen: Boolean,
    val isVertical: Boolean,
    val area: MfCuboidArea,
    val trigger: MfBlockPosition,
    val material: Material = IRON_BARS,
    val status: MfGateStatus = READY
)