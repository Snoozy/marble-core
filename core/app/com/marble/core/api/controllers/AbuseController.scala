package com.marble.core.api.controllers

import com.google.inject.Inject
import com.marble.core.data.cache.Redis
import com.marble.core.data.db.models.{UserBlock, CommentFlag, PostFlag}
import com.marble.utils.play.Auth
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._

class AbuseController @Inject() (auth: Auth) extends Controller {

    def flagPost = auth.ApiAuthAction { implicit user => implicit request =>
        val body: AnyContent = request.body
        body.asFormUrlEncoded.map { form =>
            try {
                val postId = form.get("post_id").map(_.head.toInt)
                if (postId.isDefined) {
                    PostFlag.createPostFlag(user.get.userId.get, postId.get)
                    Ok(Json.obj("success" -> "Post flagged."))
                } else
                    BadRequest(Json.obj("error" -> "Invalid request format."))
            } catch {
                case e: NumberFormatException => BadRequest(Json.obj("error" -> "Invalid request format."))
            }
        }.getOrElse(BadRequest(Json.obj("error" -> "Invalid request format.")))
    }

    def flagComment = auth.ApiAuthAction { implicit user => implicit request =>
        val body: AnyContent = request.body
        body.asFormUrlEncoded.map { form =>
            try {
                val commentId = form.get("comment_id").map(_.head.toInt)
                if (commentId.isDefined) {
                    CommentFlag.createCommentFlag(user.get.userId.get, commentId.get)
                    Ok(Json.obj("success" -> "Comment flagged."))
                } else
                    BadRequest(Json.obj("error" -> "Invalid request format."))
            } catch {
                case e: NumberFormatException => BadRequest(Json.obj("error" -> "Invalid request format."))
            }
        }.getOrElse(BadRequest(Json.obj("error" -> "Invalid request format.")))
    }

    def blockUser = auth.ApiAuthAction { implicit user => implicit request =>
        val body: AnyContent = request.body
        body.asFormUrlEncoded.map { form =>
            try {
                val userId = form.get("user_id").map(_.head.toInt)
                if (userId.isDefined) {
                    UserBlock.blockUser(user.get.userId.get, userId.get)
                    Ok(Json.obj("success" -> "User blocked."))
                } else
                    BadRequest(Json.obj("error" -> "Invalid request format."))
            } catch {
                case e: NumberFormatException => BadRequest(Json.obj("error" -> "Invalid request format."))
            }
        }.getOrElse(BadRequest(Json.obj("error" -> "Invalid request format.")))

    }

}
