package com.marble.core.api.apple

import com.google.inject.Singleton
import com.google.inject.Inject
import com.marble.core.config.APNSConfig
import com.notnoop.apns._
import com.marble.core.data.db.models.AppleDeviceToken
import play.api.libs.concurrent.Akka
import scala.concurrent.duration._
import scala.collection.JavaConversions._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._

@Singleton
class PushNotifications @Inject() (apnsConfig: APNSConfig) {

    private lazy val service = APNS.newService().withCert(certPath, certPassword)
        .withSandboxDestination().build()
    private val certPath: String = apnsConfig.certPass
    private val certPassword: String = apnsConfig.certPath

    def sendNotification(userId: Int, message: String) = {
        val tokens = AppleDeviceToken.getDeviceTokens(userId)
        tokens.foreach { t =>
            send(message, t)
        }
        clearInactive()
    }

    private def send(message: String, token: String) = {
        val payload = APNS.newPayload().alertBody(message).build()
        service.push(token, payload)
    }

    private def clearInactive(): Unit = {
        Akka.system.scheduler.scheduleOnce(10.milliseconds) {
            val inactive = service.getInactiveDevices
            for ((token, date) <- inactive) {
                AppleDeviceToken.deleteToken(token)
            }
        }
    }

}
