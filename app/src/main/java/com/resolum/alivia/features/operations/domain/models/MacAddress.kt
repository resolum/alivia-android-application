package com.resolum.alivia.features.operations.domain.models

/** MAC addresses as the installation form takes them: 12 hexadecimal digits in pairs separated by colons. */
object MacAddress {
    private val FORMAT = Regex("^([0-9A-F]{2}:){5}[0-9A-F]{2}$")

    /** Upper-cases the text, accepts dashes and inserts the colons while the technician types. */
    fun format(input: String): String {
        val digits = input.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }.take(12)
        return digits.chunked(2).joinToString(":")
    }

    fun isValid(value: String): Boolean = FORMAT.matches(value)
}
