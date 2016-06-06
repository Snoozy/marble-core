package com.marble.core.web.controllers

import com.google.inject.Inject
import com.marble.core.data.Constants
import com.marble.core.data.aws.S3
import com.marble.core.data.db.models._
import com.marble.core.web.views.html.desktop.components
import com.marble.core.web.views.html.desktop.core
import com.marble.utils.Etc._
import com.marble.utils.play.Auth
import play.api.libs.json.Json
import play.api.mvc._
import com.marble.core.web.controllers.MediaController._

class PostController @Inject() (auth: Auth) extends Controller {

    def viewPostPage(boardName: String, postId: Int) = auth.AuthAction { implicit user => implicit request =>
        val board = Board.find(boardName)
        val post = Post.find(postId)
        if (post.isDefined && board.isDefined && post.get.boardId == board.get.boardId.get) {
            Ok(core.view_post(user, post.get)())
        } else {
            NotFound("Post not found.")
        }
    }

    def post = auth.AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => processPost(request, user.get)
            case None => BadRequest("User not authenticated.")
        }
    }

    def repost = auth.AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest("User not authenticated.")
            case Some(_) => processRepost(request, user.get)
        }
    }

    def processRepost(request: Request[AnyContent], user: User): Result = {
        val body: AnyContent = request.body
        body.asFormUrlEncoded.map { form =>
            try {
                val repostId = form.get("repost").map(_.head)
                val comment = form.get("comment").map(_.head)
                val board_name = form.get("board").map(_.head)
                if (board_name.isDefined) {
                    val board = Board.find(board_name.get)
                    if (board.isDefined) {
                        val repost = Post.find(repostId.get.toInt)
                        if (repost.isDefined) {
                            val repostId = {
                                if (repost.get.repostId.isDefined)
                                    repost.get.repostId
                                else
                                    repost.get.postId
                            }
                            val newPost = Post.createSimplePost(user.userId.get, comment.getOrElse(""), board.get.boardId.get, repostId)
                            if (newPost.isDefined) {
                                Ok(Json.obj("item_html" -> compressHtml(components.post(Post.find(newPost.get.toInt).get, Some(user))().toString())))
                            } else {
                                BadRequest("Invalid parameters.")
                            }
                        } else {
                            BadRequest("Post does not exist.")
                        }
                    } else {
                        BadRequest("Board does not exist.")
                    }
                } else {
                    BadRequest("Board name not defined.")
                }
            } catch {
                case e: java.lang.NumberFormatException => return BadRequest("Invalid parameters.")
            }
        }.getOrElse(BadRequest("FormUrlEncoded required."))
    }

    def deletePost(postId: Int) = auth.AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest("User not authenticated.")
            case Some(_) =>
                val post = Post.find(postId)
                if (post.isDefined && (post.get.userId == user.get.userId.get || user.get.admin)) {
                    if (Post.deletePost(postId)) {
                        Ok(Json.obj("success" -> "Successfully deleted post."))
                    } else {
                        BadRequest(Json.obj("error" -> "Unknown error deleting post."))
                    }
                } else {
                    BadRequest(Json.obj("error" -> "Error deleting post."))
                }
        }
    }

    private def processPost(request: Request[AnyContent], user: User): Result = {
        val body = request.body.asMultipartFormData
        if (body.isDefined) {
            try {
                val form = body.get.asFormUrlEncoded
                val data = form.get("data").map(_.head)
                val board_name = form.get("board_name").map(_.head)
                val mediaIds: Seq[Int] = uploadMedia(body.get.files.filter(_.key.matches(MediaIdentifier.toString())))
                if (data.isDefined && board_name.isDefined) {
                    val board = Board.find(board_name.get)
                    if (board.isDefined) {
                        val newPost = {
                            if (mediaIds.isEmpty) {
                                Post.createSimplePost(user.userId.get, data.get, board.get.boardId.get)
                            } else {
                                Post.createMediaPost(user.userId.get, data.get, board.get.boardId.get, mediaIds)
                            }
                        }
                        if (newPost.isDefined)
                            Ok(Json.obj("item_html" -> compressHtml(components.post(Post.find(newPost.get.toInt).get, Some(user))().toString())))
                        else
                            BadRequest("Invalid request.")
                    } else {
                        BadRequest("Invalid board name.")
                    }
                } else
                    BadRequest("Invalid request.")
            } catch {
                case e: java.lang.NumberFormatException => BadRequest("Invalid parameters.")
            }
        } else {
            BadRequest("Invalid format.")
        }
    }

}
