package com.marble.utils.security

import java.math.BigInteger
import java.security.SecureRandom

object SecureRand {

    private val random = new SecureRandom()

    def newSessionId(): String = {
        new BigInteger(130, random).toString(32)
    }

}
