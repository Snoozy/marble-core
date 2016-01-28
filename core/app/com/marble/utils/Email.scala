package com.marble.utils

object Email {

    def verifyEmail(email: String): Boolean = {
        email.contains('@')
    }

}