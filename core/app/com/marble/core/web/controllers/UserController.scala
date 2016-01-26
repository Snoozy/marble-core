package com.marble.core.web.controllers

import com.marble.utils.play.Auth.AuthAction
import play.api.mvc._
import com.marble.core.data.db.models._
import com.marble.core.web.views.html.desktop.core
import play.api.libs.json.Json

class UserController extends Controller {

    def userPage(username: String) = AuthAction { implicit user => implicit request =>
        if (username.startsWith("@")) {
            Found("/user/" + username.substring(1))
        } else {
            val describeUser = User.find(username)
            if (describeUser.isDefined) {
                val posts = User.getPosts(describeUser.get.userId.get)
                val comments = User.getComments(describeUser.get.userId.get)
                val boards = User.getBoards(describeUser.get.userId.get)
                val postsCount = User.getPostsCount(describeUser.get.userId.get)
                Ok(core.user(describeUser.get, user, posts, comments, boards, postsCount))
            } else {
                NotFound("User not found.")
            }
        }
    }

    def checkEmail = AuthAction { implicit user => implicit request =>
        val email = request.getQueryString("email")
        if (email.isDefined && User.checkEmail(email.get)) {
            Ok(Json.obj("success" -> "Username is available."))
        } else {
            NotFound(Json.obj("error" -> "Username not available."))
        }
    }

}
