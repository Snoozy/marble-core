package com.marble.core.web.controllers

import com.google.inject.Inject
import com.marble.core.data.db.models._
import com.marble.utils.play.Auth
import play.api.mvc._
import play.api.libs.json.Json

class NotificationController @Inject() (auth: Auth) extends Controller {

    def readNotifications = auth.AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User not authenticated."))
            case Some(_) =>
                Notification.read(user.get.userId.get)
                Ok(Json.obj("status" -> "success"))
        }
    }

}
