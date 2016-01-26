package com.marble.core.web.controllers

import play.api.mvc._
import play.api.Play

class StaticController extends Controller {

    def support = Action {
        Ok(com.marble.core.web.views.html.desktop.core.support())
    }

}
