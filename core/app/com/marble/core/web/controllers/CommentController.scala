package com.marble.core.web.controllers

import com.google.inject.Inject
import com.marble.core.data.db.models._
import com.marble.utils.play.Auth
import com.marble.core.web.views.html.desktop.core
import com.marble.core.web.views.html.desktop.components
import com.marble.utils.Etc._
import play.api.mvc._
import play.api.libs.json.Json

class CommentController @Inject() (auth: Auth) extends Controller {

    def viewSingleComment(name: String, id: Int) = auth.AuthAction { implicit user => implicit request =>
        val board = Board.find(name)
        val comment = Comment.find(id, status = None)
        if (comment.isDefined && board.isDefined) {
            val post = Post.find(comment.get.postId)
            if (post.isDefined) {
                Ok(core.view_post(user, post.get, singleComment = comment)())
            } else {
                NotFound("Comment not found.")
            }
        } else {
            NotFound("Comment not found.")
        }
    }

    def createComment(postId: Int) = auth.AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User not authenticated."))
            case Some(_) =>
                val post = Post.find(postId)
                val data = request.body.asFormUrlEncoded.flatMap(_.get("data").flatMap(_.headOption))
                try {
                    val parentId_raw = request.body.asFormUrlEncoded.flatMap(_.get("parent").map(_.head.toInt))
                    if (post.isDefined && data.isDefined) {
                        val parentId: Option[Int] = if (parentId_raw.isDefined && parentId_raw.get == 0) None else parentId_raw
                        val commentId = Comment.create(postId, user.get.userId.get, data.get, parentId)
                        if (commentId.isDefined) {
                            val board = Board.find(post.get.boardId)
                            val ctn = CommentTreeNode(Comment.find(commentId.get.toInt).get, Seq())
                            Ok(Json.obj("status" -> "success", "item_html" -> compressHtml(components.comment(ctn, user, board.get, post.get.userId)(expanded = false, root = parentId.isEmpty).toString())))
                        } else {
                            BadRequest(Json.obj("error" -> "Request invalid."))
                        }
                    } else {
                        BadRequest(Json.obj("error" -> "Request format invalid."))
                    }
                } catch {
                    case e: NumberFormatException => BadRequest(Json.obj("error" -> "Request invalid."))
                }
        }
    }

    def deleteComment(commentId: Int) = auth.AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User not authenticated"))
            case Some(_) =>
                val comment = Comment.find(commentId)
                if (comment.isDefined && (comment.get.userId == user.get.userId.get || user.get.admin)) {
                    if (Comment.delete(commentId)) {
                        Ok(Json.obj("status" -> "Success"))
                    } else {
                        InternalServerError(Json.obj("error" -> "Something broke."))
                    }
                } else {
                    BadRequest(Json.obj("error" -> "User is not authorized to perform this action."))
                }
        }
    }

}
