package com.marble.core.inject

import com.google.inject.AbstractModule
import com.marble.core.data.cache.{Cache, Redis}

class CacheModule extends AbstractModule {

    def configure() = {
        bind(classOf[Cache]).to(classOf[Redis])
    }

}