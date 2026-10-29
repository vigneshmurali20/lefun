package app.lefun.ble

fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }

fun String.hexToBytes(): ByteArray {
    val clean = filter { !it.isWhitespace() }
    require(clean.length % 2 == 0) { "odd number of hex digits" }
    return ByteArray(clean.length / 2) { clean.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}
