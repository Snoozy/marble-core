package com.marble.core.web.controllers

import com.marble.core.data.db.models._
import com.marble.utils.play.Auth.AuthAction
import play.api.mvc._

class ExploreController extends Controller {

    def explore = AuthAction { implicit user => implicit request =>
        val boards = Board.getTrendingBoards(limit = 50)
        Ok(com.marble.core.web.views.html.desktop.core.explore(user, boards))
    }

}
