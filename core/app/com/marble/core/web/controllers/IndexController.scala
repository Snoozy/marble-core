package com.marble.core.web.controllers

import com.google.inject.Inject
import com.marble.core.data.db.models.{Board, Post, User}
import com.marble.utils.play.Auth.AuthAction
import com.marble.core.data.Constants
import play.api.mvc._
import com.marble.core.data.cache.Redis

class IndexController @Inject() (redis: Redis) extends Controller {

    def homePage = AuthAction { implicit user => implicit request =>
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
        val cached = redis.get("gettingStarted_cache")
        if (cached.isDefined) {
            Ok(cached.get).as(HTML)
        } else {
            val comp = com.marble.core.web.views.html.desktop.core.getting_started(Constants.GettingStartedBoards, Constants.FeaturedBoards)
            redis.setex("gettingStarted_cache", comp.toString(), expire = 3600)
            Ok(comp)
        }
    }

    def cachedWelcomeHtml = {
        val cached = redis.get("welcome_cache")
        if (cached.isDefined) {
            Ok(cached.get).as(HTML)
        } else {
            val comp = com.marble.core.web.views.html.desktop.core.welcome(getWelcomeBoards)
            redis.setex("welcome_cache", comp.toString(), expire = 3600)
            Ok(comp)
        }
    }

    def getWelcomeBoards: Seq[Board] = {
        Board.getTrendingBoards(20)
    }

}
