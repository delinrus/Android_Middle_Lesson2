package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

class User private constructor(
    private val firstName: String,
    private val lastName: String?,
    email: String? = null,
    rawPhone: String? = null,
    meta: Map<String, Any>? = null
) {
    val userInfo: String

    private val fullName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .capitalize(Locale.ROOT)

    private val initials: String
        get() = listOfNotNull(firstName, lastName)
            .map { it.first().toUpperCase() }
            .joinToString(" ")

    private var phone: String? = null
        set(value) {
            field = value?.trimPhone()
        }

    private var _login: String? = null
    internal var login: String
        set(value) {
            _login = value.toLowerCase(Locale.ROOT)
        }
        get() = _login!!

    private var _salt: String? = null
    private var salt: String
        set(value) {
            _salt = value
        }
        get() {
            if (_salt == null) {
                _salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()
            }
            return _salt!!
        }

    private lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    //for email
    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        password: String
    ) : this(firstName, lastName, email = email, meta = mapOf("auth" to "password")) {
        println("Secondary mail constructor")
        passwordHash = encrypt(password)
    }

    //for phone
    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String
    ) : this(firstName, lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")) {
        println("Secondary phone constructor")
        require(phone!!.matches("^[+]\\d{11}".toRegex())) {
            "Enter a valid phone number starting with a + and containing 11 digits"
        }
        renewAccessCode()
    }

    //for scv
    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        rawPhone: String?,
        salt: String?,
        passwordHash: String?
    ) : this(
        firstName,
        lastName,
        rawPhone = rawPhone,
        email = email,
        meta = mapOf("src" to "csv")
    ) {
        println("Secondary scv constructor")
        salt?.let { this.salt = it }
        passwordHash?.let { this.passwordHash = it }
        phone?.let {renewAccessCode()}
    }

    init {
        println("First init block, primary constructor was called")

        check(!firstName.isBlank()) { "FirstName must be not blank" }
        check((!email.isNullOrBlank()) || (!rawPhone.isNullOrBlank())) { "Email or phone must be not blank" }

        phone = rawPhone
        login = email ?: phone!!

        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
        """.trimIndent()
    }

    fun checkPassword(pass: String) = encrypt(pass) == passwordHash

    fun changePassword(oldPass: String, newPass: String) {
        if (checkPassword(oldPass)) passwordHash = encrypt(newPass)
        else throw IllegalArgumentException("The entered password does not match the current password")
    }

    fun renewAccessCode() {
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code
        sendAccessCodeToUser(phone, code)
    }

    private fun encrypt(password: String): String = salt.plus(password).md5()

    private fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        return StringBuilder().apply {
            repeat(6) {
                (possible.indices).random().also { index ->
                    append(possible[index])
                }
            }
        }.toString()
    }

    private fun sendAccessCodeToUser(phone: String?, code: String) {
        println("..... sending access code: $code on $phone")
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray()) //16 byte
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    companion object Factory {
        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            phone: String? = null
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()

            return when {
                !phone.isNullOrBlank() -> User(firstName, lastName, phone)
                !email.isNullOrBlank() && !password.isNullOrBlank() -> User(
                    firstName,
                    lastName,
                    email,
                    password
                )
                else -> throw IllegalArgumentException("Email or phone must be not null or blank")
            }
        }

        fun makeUserFromCsv(csvRecord: String): User {
            val spitedRecord = csvRecord.split(";")
            val fullName = spitedRecord[0]
            val email = spitedRecord[1].ifBlank { null }
            var salt : String? = null
            var hash : String? = null
            val saltAndHash = spitedRecord[2].ifBlank { null }
            saltAndHash?.let {
                salt = it.split(":")[0].ifBlank { null }
                hash = it.split(":")[1].ifBlank { null }
            }
            val phone = spitedRecord[3].ifBlank { null }
            if ((salt == null || (hash == null)) && (phone == null)) {
                throw IllegalArgumentException("Must contain hash-salt pair of phone")
            }

            return User(
                firstName = fullName.fullNameToPair().first,
                lastName = fullName.fullNameToPair().second,
                email,
                phone,
                salt,
                hash
            )
        }

        fun String.trimPhone(): String {
            return this.replace("[^+\\d]".toRegex(), "")
        }

        private fun String.fullNameToPair(): Pair<String, String?> {
            return this.split(" ")
                .filter { it.isNotBlank() }
                .run {
                    when (size) {
                        1 -> first() to null
                        2 -> first() to last()
                        else -> throw IllegalArgumentException(
                            "Fullname must contain only first name" +
                                    " and last name, current split result ${this@fullNameToPair}"
                        )
                    }
                }
        }
    }
}