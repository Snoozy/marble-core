package com.marble.core.web.controllers

import com.google.inject.Inject
import com.marble.core.data.db.models._
import com.marble.utils.play.Auth
import play.api.mvc._

class ExploreController @Inject() (auth: Auth) extends Controller {

    def explore = auth.AuthAction { implicit user => implicit request =>
        val boards = Board.getTrendingBoards(limit = 50)
        Ok(com.marble.core.web.views.html.desktop.core.explore(user, boards))
    }

}