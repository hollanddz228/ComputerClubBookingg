package com.example.computerclubbooking.data.models.managers

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.computerclubbooking.data.models.SavedAccount
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SavedAccountsManager {

    private const val PREF_NAME = "saved_accounts_secure"
    private const val KEY_ACCOUNTS = "accounts"

    private fun getPrefs(context: Context) =
        EncryptedSharedPreferences.create(
            PREF_NAME,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    private fun loadAccountsInternal(context: Context): MutableList<SavedAccount> {
        val prefs = getPrefs(context)
        val json = prefs.getString(KEY_ACCOUNTS, null) ?: return mutableListOf()

        return try {
            val type = object : TypeToken<MutableList<SavedAccount>>() {}.type
            Gson().fromJson<MutableList<SavedAccount>>(json, type) ?: mutableListOf()
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    /** Для UI */
    fun getAccounts(context: Context): List<SavedAccount> = loadAccountsInternal(context)

    /** Для сохранения / обновления */
    fun saveOrUpdateAccount(context: Context, account: SavedAccount) {
        val list = loadAccountsInternal(context)
        val existing = list.indexOfFirst { it.email == account.email }

        if (existing >= 0) {
            list[existing] = account
        } else {
            // новый аккаунт кладём в начало списка
            list.add(0, account)
        }

        val json = Gson().toJson(list)
        getPrefs(context).edit().putString(KEY_ACCOUNTS, json).apply()
    }

    fun removeAccount(context: Context, email: String) {
        val list = loadAccountsInternal(context)
        list.removeAll { it.email == email }
        val json = Gson().toJson(list)
        getPrefs(context).edit().putString(KEY_ACCOUNTS, json).apply()
    }
}
