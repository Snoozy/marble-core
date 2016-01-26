package com.marble.core.config

import com.google.inject.Singleton
import play.api.Play

@Singleton
class FacebookConfig {
    val fbId: String = {
        try {
            Play.current.configuration.getString("facebook.client_id").get
        } catch {
            case e: NoSuchElementException => throw new FBConfigMissing("Facebook config missing")
        }
    }
    val fbSecret: String = {
        try {
            Play.current.configuration.getString("facebook.client_secret").get
        } catch {
            case e: NoSuchElementException => throw new FBConfigMissing("Facebook config missing")
        }
    }
}

case class FBConfigMissing(message: String) extends Exception(message)