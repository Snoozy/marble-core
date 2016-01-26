package com.marble.core.config

import com.google.inject.Singleton
import play.api.Play

@Singleton
class APNSConfig {
    val certPath = Play.current.configuration.getString("apns.certPath").getOrElse("")
    val certPass = Play.current.configuration.getString("apns.certPassword").getOrElse("")
}