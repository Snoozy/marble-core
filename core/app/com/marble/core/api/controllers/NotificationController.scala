package com.marble.core.api.controllers

import com.google.inject.Inject
import com.marble.core.data.db.models._
import com.marble.utils.play.Auth
import play.api.mvc._
import play.api.libs.json.Json

class NotificationController @Inject() (auth: Auth) extends Controller {

    def getNotifications = auth.ApiAuthAction { implicit user => implicit request =>
            val notifs = Notification.getNotifications(user.get.userId.get)
            Ok(Json.obj("notifications" -> Notification.toJsonSeq(notifs)))
    }

    def readNotifications = auth.ApiAuthAction { implicit user => implicit request =>
        Notification.read(user.get.userId.get)
        Ok(Json.obj("success" -> "Successful"))
    }

}
