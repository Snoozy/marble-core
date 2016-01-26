package com.marble.core.api.controllers

import com.marble.core.config.APNSConfig
import play.api.libs.json.Json
import play.api.mvc._
import com.marble.core.api.apple.PushNotifications

class EtcController extends Controller {

    def healthCheck = Action {
        Ok("Instance healthy.")
    }

    def rejectHttp = Action {
        BadRequest(Json.obj("error" -> "Https required."))
    }

    def etc = Action {
        new PushNotifications(new APNSConfig).sendNotification(76, "Hi.")
        Ok("asdf")
    }

}
