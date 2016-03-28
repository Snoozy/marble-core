package com.marble.core.api.apple

import com.notnoop.apns._
import com.marble.core.data.db.models.AppleDeviceToken
import play.api.Play
import play.api.libs.concurrent.Akka

import scala.concurrent.duration._
import scala.collection.JavaConversions._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._

object PNController {

    private lazy val service = APNS.newService().withCert(certPath, certPassword)
        .withProductionDestination().build()
    private val certPath: String = Play.current.configuration.getString("apns.certPath").getOrElse("")
    private val certPassword: String = Play.current.configuration.getString("apns.certPassword").getOrElse("")

    def sendNotification(userId: Int, message: String): Unit = {
        if (Play.isProd) {
            Akka.system.scheduler.scheduleOnce(10.milliseconds) {
                val tokens = AppleDeviceToken.getDeviceTokens(userId)
                val payload = APNS.newPayload().alertBody(message).build()
                service.push(tokens, payload)
                clearInactive()
            }
        }
    }

    private def clearInactive() = {
        val inactive = service.getInactiveDevices
        for ((token, date) <- inactive) {
            AppleDeviceToken.deleteToken(token)
        }
    }

}