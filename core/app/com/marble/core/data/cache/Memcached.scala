package com.marble.core.data.cache

import net.rubyeye.xmemcached._
import net.rubyeye.xmemcached.command.BinaryCommandFactory
import net.rubyeye.xmemcached.utils.AddrUtil

case class MemcachedAddressUndefined(message: String) extends Exception(message)

object Memcached {

    private lazy val builder = new XMemcachedClientBuilder(AddrUtil.getAddresses(addr.get))
    private var addr: Option[String] = None

    def setAddr(address: String) = {
        addr = Some(address)
        builder.setConnectionPoolSize(5)
        builder.setCommandFactory(new BinaryCommandFactory())
    }

    def get(key: String): Option[String] = {
        if (!addr.isDefined)
            throw new MemcachedAddressUndefined("Memcached address has not been set.")
        val client = builder.build()
        val res: String = client.get(key)
        client.shutdown()
        Option(res)
    }

    def getTouch[T](key: String, duration: Int = 2592000): Option[T] = {
        if (!addr.isDefined)
            throw new MemcachedAddressUndefined("Memcached address has not been set.")
        val client = builder.build()
        val res: T = client.getAndTouch(key, duration)
        client.shutdown()
        Option(res)
    }

    def set[T](key: String, value: T, duration: Int = 2592000): Boolean = {
        if (!addr.isDefined)
            throw new MemcachedAddressUndefined("Memcached address has not been set.")
        val client = builder.build()
        val res: Boolean = client.set(key, duration, value)
        client.shutdown()
        res
    }

    def delete(key: String): Boolean = {
        if (!addr.isDefined)
            throw new MemcachedAddressUndefined("Memcached address has not been set.")
        val client = builder.build()
        val res: Boolean = client.delete(key)
        client.shutdown()
        res
    }

}
