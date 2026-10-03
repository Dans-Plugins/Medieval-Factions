package com.dansplugins.factionsystem.command

fun Array<out String>.dropFirst() = if (isEmpty()) emptyArray() else copyOfRange(1, size)

fun Array<out String>.unquote(): Array<String> {
    val unquoted = mutableListOf<String>()
    val quotedParts = mutableListOf<String>()
    var insideQuotes = false

    for (arg in this) {
        if (!insideQuotes) {
            if (arg.startsWith("\"")) {
                val withoutOpeningQuote = arg.drop(1)
                insideQuotes = true

                if (arg.length > 1 && arg.endsWith("\"")) {
                    quotedParts.add(withoutOpeningQuote.dropLast(1))
                    unquoted.add(quotedParts.joinToString(" "))
                    quotedParts.clear()
                    insideQuotes = false
                } else if (withoutOpeningQuote.isNotEmpty()) {
                    quotedParts.add(withoutOpeningQuote)
                }
            } else {
                unquoted.add(arg)
            }
        } else {
            if (arg.endsWith("\"")) {
                quotedParts.add(arg.dropLast(1))
                unquoted.add(quotedParts.joinToString(" "))
                quotedParts.clear()
                insideQuotes = false
            } else {
                quotedParts.add(arg)
            }
        }
    }

    if (insideQuotes) {
        unquoted.add(quotedParts.joinToString(" "))
    }

    return unquoted.toTypedArray()
}

/**
 * Splits off the first argument, honouring the same quoting rules as [unquote], so a multi-word name can be given as
 * `"Name With Spaces"`. The remaining arguments are returned exactly as typed. Returns null when there are no arguments.
 */
fun Array<out String>.splitLeadingArg(): Pair<String, Array<String>>? {
    if (isEmpty()) return null
    val first = this[0]
    if (!first.startsWith("\"")) return first to copyOfRange(1, size).map { it }.toTypedArray()
    val closingIndex = indices.firstOrNull { index ->
        val arg = this[index]
        if (index == 0) arg.length > 1 && arg.endsWith("\"") else arg.endsWith("\"")
    } ?: lastIndex
    val leading = copyOfRange(0, closingIndex + 1).unquote().joinToString(" ")
    return leading to copyOfRange(closingIndex + 1, size).map { it }.toTypedArray()
}
