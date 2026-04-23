package com.dansplugins.factionsystem.command

fun Array<out String>.dropFirst() = if (isEmpty()) emptyArray() else copyOfRange(1, size)

fun Array<out String>.unquote(): Array<String> {
    val unquoted = mutableListOf<String>()
    val quotedParts = mutableListOf<String>()
    var insideQuotes = false

    for (arg in this) {
        if (!insideQuotes) {
            if (arg.startsWith("\"")) {
                quotedParts.clear()
                quotedParts.add(arg)
                insideQuotes = true

                if (arg.length > 1 && arg.endsWith("\"")) {
                    val combined = quotedParts.joinToString(" ")
                    unquoted.add(combined.drop(1).dropLast(1))
                    quotedParts.clear()
                    insideQuotes = false
                }
            } else {
                unquoted.add(arg)
            }
        } else {
            quotedParts.add(arg)

            if (arg.endsWith("\"")) {
                val combined = quotedParts.joinToString(" ")
                unquoted.add(combined.drop(1).dropLast(1))
                quotedParts.clear()
                insideQuotes = false
            }
        }
    }

    if (insideQuotes) {
        unquoted.add(quotedParts.joinToString(" "))
    }

    return unquoted.toTypedArray()
}
