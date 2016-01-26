package com.marble.core.api.controllers

import com.google.inject.Inject
import com.marble.utils.play.Auth
import play.api.libs.json.Json
import play.api.mvc._
import com.marble.core.data.search.Search
import com.marble.core.data.db.models._


class SearchController @Inject() (auth: Auth) extends Controller {

    def fullSearchBoard = auth.ApiAuthAction { implicit user => implicit request =>
        val query = request.getQueryString("q")
        if (query.isDefined) {
            val boards = Search.boardSearch(query.get)
            Ok(Json.obj("results" -> Board.toJsonSeq(boards, user = user)))
        } else {
            BadRequest(Json.obj("error" -> "Search query required."))
        }
    }

    def autoCompleteBoard = auth.ApiAuthAction { implicit user => implicit request =>
        val query = request.getQueryString("q")
        if (query.isDefined) {
            val boards = Search.autoCompleteBoard(query.get)
            Ok(Json.obj("results" -> Board.toJsonSeq(boards, user = user)))
        } else {
            BadRequest(Json.obj("error" -> "Search query required."))
        }
    }

}
