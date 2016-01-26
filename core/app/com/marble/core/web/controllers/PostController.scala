package com.marble.core.web.controllers

import com.marble.core.data.db.models._
import com.marble.core.web.views.html.desktop.components
import com.marble.core.web.views.html.desktop.core
import com.marble.utils.Etc._
import com.marble.utils.play.Auth.AuthAction
import play.api.libs.json.Json
import play.api.mvc._

class PostController extends Controller {

    def viewPostPage(board_name: String, postId: Int) = AuthAction { implicit user => implicit request =>
        val board = Board.find(board_name)
        val post = Post.find(postId)
        if (post.isDefined && board.isDefined && post.get.boardId == board.get.boardId.get) {
            Ok(core.view_post(user, post.get)())
        } else {
            NotFound("Post not found.")
        }
    }

    def post = AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => processPost(request, user.get)
            case None => BadRequest("User not authenticated.")
        }
    }

    def repost = AuthAction { implicit user => implicit request =>
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
                            val newPost = Post.createSimplePost(user.userId.get, None, comment.getOrElse(""), board.get.boardId.get, repostId)
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

    def deletePost(postId: Int) = AuthAction { implicit user => implicit request =>
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
        val body: AnyContent = request.body
        body.asFormUrlEncoded.map { form =>
            try {
                var title = form.get("title").map(_.head)
                val data = form.get("data").map(_.head)
                val board_name = form.get("board_name").map(_.head)
                val mediaIds = form.get("media").map(_.head)
                if (data.isDefined && board_name.isDefined) {
                    val board = Board.find(board_name.get)
                    if (board.isDefined) {
                        if (title.isDefined && title.get == "") {
                            title = None
                        }
                        val newPost = {
                            if (user.admin) {
                                val newUserName = form.get("user").map(_.head)
                                if (newUserName.isDefined && newUserName.get != "") {
                                    val newUserId = {
                                        val userExists = User.find(newUserName.get)
                                        if (userExists.isDefined) {
                                            userExists.get.userId.get
                                        } else {
                                            User.create(User.genUsername(newUserName.get + "@marble.co"), newUserName.get, newUserName.get, newUserName.get + "@marble.co", None).get.toInt
                                        }
                                    }
                                    val newUser = User.find(newUserId)
                                    if (mediaIds.isEmpty || mediaIds.get == "") {
                                        Post.createSimplePost(newUser.get.userId.get, title, data.get, board.get.boardId.get)
                                    } else {
                                        Post.createMediaPost(newUser.get.userId.get, title, data.get, board.get.boardId.get, mediaIds.get.split("~").map(_.toInt))
                                    }
                                } else {
                                    if (mediaIds.isEmpty || mediaIds.get == "") {
                                        Post.createSimplePost(user.userId.get, title, data.get, board.get.boardId.get)
                                    } else {
                                        Post.createMediaPost(user.userId.get, title, data.get, board.get.boardId.get, mediaIds.get.split("~").map(_.toInt))
                                    }
                                }
                            } else {
                                if (mediaIds.isEmpty || mediaIds.get == "") {
                                    Post.createSimplePost(user.userId.get, title, data.get, board.get.boardId.get)
                                } else {
                                    Post.createMediaPost(user.userId.get, title, data.get, board.get.boardId.get, mediaIds.get.split("~").map(_.toInt))
                                }
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
                case e: java.lang.NumberFormatException => return BadRequest("Invalid parameters.")
            }
        }.getOrElse {
            BadRequest("Only json or form url encoded content types accepted.")
        }
    }

}
