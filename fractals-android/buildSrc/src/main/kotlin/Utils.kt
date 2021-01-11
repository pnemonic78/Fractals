fun String.padLeft(expectedLength: Int, pad: String): String {
    if (length >= expectedLength) {
        return this
    }
    val s = StringBuffer(this)
    do {
        s.insert(0, pad)
    } while (s.length < expectedLength)
    return s.toString()
}

fun String.padRight(expectedLength: Int, pad: String): String {
    if (length >= expectedLength) {
        return this
    }
    val s = StringBuffer(this)
    do {
        s.append(pad)
    } while (s.length < expectedLength)
    return s.toString()
}

fun generateVersionCode(major: Int, minor: Int): Int {
    return major * 100 + minor
}