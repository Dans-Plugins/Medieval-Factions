package com.dansplugins.factionsystem.failure

/**
 * Thrown when a write to a JSON storage file is refused because that file could not be read.
 *
 * The JSON backend mutates a file by loading it, changing the loaded copy and writing the whole thing
 * back. If the load could not read the stored contents, the copy being written no longer represents
 * them, and writing it would replace real records with nothing. The write is refused instead, and the
 * unreadable contents are copied aside next to the file.
 */
class UnreadableJsonFileException(val fileName: String) : Exception(
    "Refusing to write $fileName because its stored contents could not be read. " +
        "Writing would replace them. A copy has been saved alongside the file; " +
        "repair or remove the file, and writes resume as soon as it can be read again."
)
