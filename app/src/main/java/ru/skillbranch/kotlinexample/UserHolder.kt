package ru.skillbranch.kotlinexample

import ru.skillbranch.kotlinexample.User.Factory.trimPhone

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User {
        return User.makeUser(fullName, email = email, password = password)
            .also { user ->
                if (map.contains(user.login))
                    throw IllegalArgumentException("A user with this email already exists")
                else
                    map[user.login] = user
            }
    }

    fun loginUser(login: String, password: String): String? {
        val logins = listOf(login.trim(), login.trimPhone())
        logins.forEach {
            map[it.trim()]?.run {
                if (checkPassword(password)) {
                    return this.userInfo
                }
            }
        }
        return null
    }

    fun clearHolder() {
        map.clear()
    }

    fun registerUserByPhone(fullName: String, phone: String): User {
        return User.makeUser(fullName, phone = phone)
            .also { user ->
                if (map.contains(user.login))
                    throw IllegalArgumentException("A user with this phone already exists")
                else
                    map[user.login] = user
            }
    }

    fun requestAccessCode(phone: String) {
        map[phone.trimPhone()]?.renewAccessCode()
    }
}