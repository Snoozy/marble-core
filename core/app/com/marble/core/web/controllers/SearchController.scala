package com.marble.core.web.controllers

import com.marble.core.data.db.models._
import com.marble.core.data.search.Search
import com.marble.utils.play.Auth.AuthAction
import play.api.mvc._

class SearchController extends Controller {

    def searchPage = AuthAction { implicit user => implicit request =>
        val q = request.getQueryString("q")
        if (q.isDefined) {
            val boards = Search.boardSearch(q.get)
            val users = Search.userSearch(q.get)
            Ok(com.marble.core.web.views.html.desktop.core.search(user, boards, users, q.get))
        } else {
            Found("/")
        }
    }

}
