package com.marble.core.data.cache

import com.google.inject.{Inject, Singleton}
import com.marble.core.config.RedisConfig
import com.redis._
import serialization._
import Parse.Implicits._

case class RedisAddressUndefined(message: String) extends Exception(message)

@Singleton
class Redis @Inject() (rc: RedisConfig) extends Cache {

    private lazy val clients = new RedisClientPool(addr.split(":")(0), addr.split(":")(1).toInt)
    private val addr: String = rc.addr

    def init = {
        clients.withClient {
            client => {
                client.get[String]("init")
            }
        }
    }

    def get(key: String): Option[String] = {
        clients.withClient {
            client => {
                client.get[String](key)
            }
        }
    }

    def lrange[A](key: String, start: Int, end: Int)(implicit format: Format, parse: Parse[A]): Option[List[Option[A]]] = {
        clients.withClient {
            client => {
                client.lrange[A](key, start, end)
            }
        }
    }

    def set(key: String, value: Any) = {
        clients.withClient {
            client => {
                client.set(key, value)
            }
        }
    }

    def setex(key: String, value: Any, expire: Int = 86400) = {
        clients.withClient {
            client => {
                client.setex(key, expire, value)
            }
        }
    }

    def lpush[A](key: String, values: List[A]) = {
        clients.withClient {
            client => {
                client.lpush(key, values)
            }
        }
    }

    def llen(key: String): Option[Int] = {
        clients.withClient {
            client => {
                client.llen(key).map(_.toInt)
            }
        }
    }

    def delete(key: String) = {
        clients.withClient {
            client => {
                client.del(key)
            }
        }
    }

}
