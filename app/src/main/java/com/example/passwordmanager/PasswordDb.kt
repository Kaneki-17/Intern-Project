package com.example.passwordmanager

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Base64

import javax.crypto.Cipher

import javax.crypto.KeyGenerator
import javax.crypto.SecretKey


class PasswordDb(context: MainActivity) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "PasswordManager.db"
        private const val TABLE_NAME = "passwords"
        private const val COLUMN_ID = "id"
        private const val COLUMN_ACCOUNT_NAME = "account_name"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_PASSWORD = "password"

        private const val AES_ALGORITHM = "AES"

        private const val AES_TRANSFORMATION = "AES"
        private val secretKey: SecretKey = generateSecretKey()


        private fun generateSecretKey(): SecretKey {
            val keyGen = KeyGenerator.getInstance(AES_ALGORITHM)
            keyGen.init(256)
            return keyGen.generateKey()
        }

        private fun encrypt(input: String): String {
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encryptedBytes = cipher.doFinal(input.toByteArray())
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        }

        private fun decrypt(encrypted: String): String {
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decodedBytes = Base64.decode(encrypted, Base64.DEFAULT)
            return String(cipher.doFinal(decodedBytes))
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val CREATE_TABLE =
            "CREATE TABLE $TABLE_NAME ($COLUMN_ID INTEGER PRIMARY KEY, " +
                    "$COLUMN_ACCOUNT_NAME TEXT," +
                    " $COLUMN_USERNAME TEXT, " +
                    "$COLUMN_PASSWORD TEXT)"
        db?.execSQL(CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertPassword(passwordItem: PasswordItem): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COLUMN_ACCOUNT_NAME, passwordItem.accountName)
        contentValues.put(COLUMN_USERNAME, passwordItem.username)
        contentValues.put(COLUMN_PASSWORD, encrypt(passwordItem.password))
        val id = db.insert(TABLE_NAME, null, contentValues)
        db.close()
        return id
    }

    @SuppressLint("Range")
    fun getPasswords(): List<PasswordItem> {
        val passwordList = mutableListOf<PasswordItem>()
        val db = this.readableDatabase
        val cursor: Cursor? = db.rawQuery("SELECT * FROM $TABLE_NAME", null)
        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val accountName = it.getString(it.getColumnIndex(COLUMN_ACCOUNT_NAME))
                    val username = it.getString(it.getColumnIndex(COLUMN_USERNAME))
                    val encryptedPassword = it.getString(it.getColumnIndex(COLUMN_PASSWORD))
                    val password = decrypt(encryptedPassword)
                    passwordList.add(PasswordItem(accountName, username, password))
                } while (it.moveToNext())
            }
        }
        cursor?.close()
        db.close()
        return passwordList
    }

    fun deletePassword(passwordItem: PasswordItem) {
        val db = this.writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_ACCOUNT_NAME = ?", arrayOf(passwordItem.accountName))
        db.close()
    }

    fun updatePassword(passwordItem: PasswordItem): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COLUMN_ACCOUNT_NAME, passwordItem.accountName)
        contentValues.put(COLUMN_USERNAME, passwordItem.username)
        contentValues.put(COLUMN_PASSWORD, encrypt(passwordItem.password))
        val updatedRows = db.update(
            TABLE_NAME,
            contentValues,
            "$COLUMN_ACCOUNT_NAME = ?",
            arrayOf(passwordItem.accountName)
        )
        db.close()
        return updatedRows
    }
}