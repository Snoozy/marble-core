package com.marble.core.web.controllers

import com.google.inject.Inject
import com.marble.utils.play.Auth
import play.api.mvc._

class GettingStartedController @Inject() (auth: Auth) extends Controller {

    def gettingStarted = auth.AuthAction { implicit user => implicit request =>
        user match {
            case None => Found("/")
            case Some(_) =>
                if (user.get.session.isDefined) {
                    user.get.session.get.remove("getting_started")
                }
                Found("/")
        }
    }

}
