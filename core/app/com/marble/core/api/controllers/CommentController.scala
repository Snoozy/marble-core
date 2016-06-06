package com.marble.core.api.controllers

import com.google.inject.Inject
import com.marble.core.data.db.models.{CommentTree, Comment}
import com.marble.utils.play.Auth
import play.api.libs.json.Json
import play.api.mvc._

/**
 * Handles all Comment API requests including:
 *      Describing
 *      Creating
 */

class CommentController @Inject() (auth: Auth) extends Controller {

    /**
     * Creates a new comment from the supplied parameters: parent_id, post_id, data.
     *
     * @return If successful returns the newly created comment.
     */
    def create = auth.ApiAuthAction { implicit user => implicit request =>
        val body: AnyContent = request.body
        body.asFormUrlEncoded.map { form =>
            try {
                val parentId = form.get("parent_id").map(_.head.toInt)
                val postId = form.get("post_id").map(_.head)
                val data = form.get("data").map(_.head)
                if (postId.isDefined && data.isDefined) {
                    val comment_id = Comment.create(postId.get.toInt, user.get.userId.get, data.get, parentId)
                    if (comment_id.isDefined)
                        Ok(Comment.toJson(Comment.find(comment_id.get.toInt).get, user = user))
                    else
                        BadRequest(Json.obj("error" -> "Invalid request format."))
                } else
                    BadRequest(Json.obj("error" -> "Invalid request format."))
            } catch {
                case e: NumberFormatException => BadRequest(Json.obj("error" -> "Invalid request format."))
            }
        }.getOrElse(BadRequest(Json.obj("error" -> "Invalid request format.")))
    }

    def describe(id: Int) = auth.ApiAuthAction { implicit user => implicit request =>
        val comment = Comment.find(id, status = None)
        if (comment.isDefined) {
            val tree = CommentTree.getCommentTreeThread(comment.get)
            Ok(Json.obj("comment_tree" -> CommentTree.commentTreeJson(tree, user)))
        } else {
            BadRequest(Json.obj("error" -> "Entity does not exist."))
        }
    }

}
