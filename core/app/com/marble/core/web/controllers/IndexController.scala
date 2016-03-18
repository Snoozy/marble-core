package com.marble.core.web.controllers

import com.google.inject.Inject
import com.marble.core.data.db.models.{Board, Post, User}
import com.marble.utils.play.Auth
import com.marble.core.data.cache.Cache
import com.marble.core.data.Constants
import play.api.mvc._

class IndexController @Inject() (auth: Auth, cache: Cache) extends Controller {

    def homePage = auth.AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) =>
                if (user.get.session.isDefined && user.get.session.get.get("getting_started").isDefined) {
                    cachedGettingStartedHtml
                } else {
                    val boards = User.getBoards(user.get.userId.get)
                    val posts = User.getFeed(user.get.userId.get, boardIds = Some(boards.map(_.boardId.get)))
                    Ok(com.marble.core.web.views.html.desktop.core.index(posts, user.get, boards)())
                }
            case None =>
                cachedWelcomeHtml
        }
    }

    def cachedGettingStartedHtml = {
        val cached = cache.get("gettingStarted_cache")
        if (cached.isDefined) {
            Ok(cached.get).as(HTML)
        } else {
            val comp = com.marble.core.web.views.html.desktop.core.getting_started(Constants.TestBoards)
            cache.setex("gettingStarted_cache", comp.toString(), expire = 3600)
            Ok(comp)
        }
    }

    def cachedWelcomeHtml = {
        val cached = cache.get("welcome_cache")
        if (cached.isDefined) {
            Ok(cached.get).as(HTML)
        } else {
            val comp = com.marble.core.web.views.html.desktop.core.welcome(getWelcomeBoards)
            cache.setex("welcome_cache", comp.toString(), expire = 3600)
            Ok(comp)
        }
    }

    def getWelcomeBoards: Seq[Board] = {
        Board.getTrendingBoards(20)
    }

}
