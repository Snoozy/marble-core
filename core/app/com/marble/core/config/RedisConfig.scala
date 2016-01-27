package com.marble.core.config

import com.google.inject.Singleton
import play.api.Play

@Singleton
class RedisConfig {
    val addr: String = Play.current.configuration.getString("redis.address").getOrElse("127.0.0.1:6379")
}