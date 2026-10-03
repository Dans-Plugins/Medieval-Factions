package com.dansplugins.factionsystem.update

/**
 * A release version in the `major.minor.patch[-pre-release]` form the plugin's
 * versions and GitHub release tags use, ordered by semantic-versioning
 * precedence: a pre-release (such as `7.0.1-SNAPSHOT`) sorts before the release
 * of the same number (`7.0.1`) but after every earlier release (`7.0.0`).
 *
 * Build metadata (`+...`) is ignored, as semver says. Pre-release identifiers
 * are compared as semver specifies: numeric ones numerically, others
 * lexically, numeric before alphanumeric, and a shorter list first when one is
 * a prefix of the other.
 */
data class MfReleaseVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: List<String> = emptyList()
) : Comparable<MfReleaseVersion> {

    val isPreRelease: Boolean get() = preRelease.isNotEmpty()

    override fun compareTo(other: MfReleaseVersion): Int {
        compareValues(major, other.major).let { if (it != 0) return it }
        compareValues(minor, other.minor).let { if (it != 0) return it }
        compareValues(patch, other.patch).let { if (it != 0) return it }
        if (preRelease.isEmpty() && other.preRelease.isEmpty()) return 0
        if (preRelease.isEmpty()) return 1
        if (other.preRelease.isEmpty()) return -1
        for (i in 0 until minOf(preRelease.size, other.preRelease.size)) {
            val result = compareIdentifiers(preRelease[i], other.preRelease[i])
            if (result != 0) return result
        }
        return compareValues(preRelease.size, other.preRelease.size)
    }

    override fun toString(): String =
        "$major.$minor.$patch" + if (preRelease.isEmpty()) "" else "-" + preRelease.joinToString(".")

    companion object {
        private val PATTERN = Regex("""^[vV]?(\d+)\.(\d+)(?:\.(\d+))?(?:-([0-9A-Za-z.-]+))?(?:\+[0-9A-Za-z.-]+)?$""")

        /**
         * Parses `7.0.0`, `v7.0.0`, `7.0.1-SNAPSHOT` or `7.0` (patch 0).
         * Returns null for anything else, such as `dev`, so an unrecognised
         * version never produces a notice.
         */
        fun parse(text: String?): MfReleaseVersion? {
            val match = PATTERN.matchEntire(text?.trim() ?: return null) ?: return null
            val (major, minor, patch, preRelease) = match.destructured
            return try {
                MfReleaseVersion(
                    major.toInt(),
                    minor.toInt(),
                    if (patch.isEmpty()) 0 else patch.toInt(),
                    if (preRelease.isEmpty()) emptyList() else preRelease.split('.')
                )
            } catch (exception: NumberFormatException) {
                null
            }
        }

        private fun compareIdentifiers(a: String, b: String): Int {
            val aNumber = a.toBigIntegerOrNull()
            val bNumber = b.toBigIntegerOrNull()
            return when {
                aNumber != null && bNumber != null -> aNumber.compareTo(bNumber)
                aNumber != null -> -1
                bNumber != null -> 1
                else -> a.compareTo(b)
            }
        }
    }
}
