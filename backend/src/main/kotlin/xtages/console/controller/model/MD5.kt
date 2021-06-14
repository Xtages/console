package xtages.console.controller.model

import java.security.MessageDigest
import kotlin.text.Charsets.UTF_8

/**
 *
 */
class MD5 {
    companion object {
        fun md5(str: String): String = MessageDigest.getInstance("MD5").digest(str.toByteArray(UTF_8)).toHex()
        private fun ByteArray.toHex() = joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
