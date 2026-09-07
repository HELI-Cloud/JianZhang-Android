package com.jianzhang.app.data

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * 解锁密码存储：加盐 SHA-256 哈希（与桌面版 auth.json 同思路），
 * 仅存本机 SharedPreferences，不联网。
 */
class AuthStore(context: Context) {
    private val prefs = context.getSharedPreferences("jz_auth", Context.MODE_PRIVATE)

    fun hasPassword(): Boolean = prefs.contains("salt")

    fun setPassword(password: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()
        prefs.edit()
            .putString("salt", salt)
            .putString("hash", sha256(salt + password))
            .apply()
    }

    fun verify(password: String): Boolean {
        val salt = prefs.getString("salt", null) ?: return false
        val hash = prefs.getString("hash", null) ?: return false
        return sha256(salt + password) == hash
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(s.toByteArray(Charsets.UTF_8))
            .toHex()

    private fun ByteArray.toHex(): String = joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
}
