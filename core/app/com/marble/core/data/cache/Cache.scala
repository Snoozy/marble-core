package com.marble.core.data.cache

import com.google.inject.ImplementedBy

@ImplementedBy(classOf[Redis])
trait Cache {

    def get(key: String): Option[String]

    def set(key: String, value: Any)

    def delete(key: String)

    def setex(key: String, value: Any, expire: Int = 86400)

}