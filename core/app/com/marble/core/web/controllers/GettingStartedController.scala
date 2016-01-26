package com.marble.core.web.controllers

import com.marble.utils.play.Auth.AuthAction
import play.api.mvc._

class GettingStartedController extends Controller {

    def gettingStarted = AuthAction { implicit user => implicit request =>
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
