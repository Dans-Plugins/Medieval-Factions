package com.dansplugins.factionsystem.command

fun Array<out String>.dropFirst() = drop(1).toTypedArray()

fun Array<out String>.unquote(): Array<out String> {
    val unquoted = mutableListOf<String>()
    var openQuotes = 0
    for (arg in this) {
        var strippedArg = arg
        if (strippedArg.startsWith("\"")) {
            if (openQuotes == 0) {
                unquoted.add("")
                strippedArg = strippedArg.drop(1)
            }
            var i = 0
            while (arg[i++] == '\"') {
                openQuotes++
            }
        }
        var closedQuotes = 0
        if (strippedArg.endsWith("\"")) {
            var i = arg.lastIndex
            while (arg[i--] == '\"') {
                closedQuotes++
            }
            if (closedQuotes >= openQuotes) {
                strippedArg = strippedArg.dropLast(1)
            }
        }
        if (openQuotes > 0) {
            if (unquoted[unquoted.lastIndex].isEmpty()) {
                unquoted[unquoted.lastIndex] = strippedArg
            } else {
                unquoted[unquoted.lastIndex] = "${unquoted[unquoted.lastIndex]} $strippedArg"
            }
        } else {
            unquoted.add(strippedArg)
        }
        openQuotes -= closedQuotes
    }
    return unquoted.toTypedArray()
}
