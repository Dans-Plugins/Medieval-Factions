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
